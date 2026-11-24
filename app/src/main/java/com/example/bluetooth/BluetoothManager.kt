package com.example.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothGattServer
import android.bluetooth.BluetoothGattServerCallback
import android.bluetooth.BluetoothGattService
import android.bluetooth.BluetoothManager as AndroidBluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.bluetooth.le.AdvertiseCallback
import android.bluetooth.le.AdvertiseData
import android.bluetooth.le.AdvertiseSettings
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.ParcelUuid
import androidx.core.content.ContextCompat
import com.example.data.model.DevicePlatform
import com.example.data.model.DiscoveredBluetoothDevice
import com.example.data.model.PermissionSummary
import com.example.data.model.RelayConnectionState
import com.example.data.model.RelayDeviceRole
import com.example.data.model.RelayTransport
import com.example.data.model.RelayedEnvelope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Binary MTU-safe chunking and reassembly helper for BLE GATT relaying.
 * Header format (8 bytes):
 * - [0..1]: Magic bytes 'E' (0x45), 'S' (0x53)
 * - [2..3]: Unsigned 16-bit messageId
 * - [4..5]: Unsigned 16-bit chunkIndex (0-based)
 * - [6..7]: Unsigned 16-bit totalChunks (>= 1)
 */
object BleRelayFramer {
    const val HEADER_SIZE = 8
    private const val MAGIC_0: Byte = 0x45 // 'E'
    private const val MAGIC_1: Byte = 0x53 // 'S'
    private const val MAX_CHUNKS = 2048
    private const val MAX_TOTAL_BYTES = 2_000_000

    private val messageCounter = AtomicInteger(1)

    fun nextMessageId(): Int = (messageCounter.getAndIncrement() and 0xFFFF).let { if (it == 0) 1 else it }

    fun encodeChunks(
        payloadBytes: ByteArray,
        maxAttPayloadBytes: Int = 180,
        messageId: Int = nextMessageId()
    ): List<ByteArray> {
        val safeAtt = maxAttPayloadBytes.coerceAtLeast(HEADER_SIZE + 8)
        val chunkDataCapacity = safeAtt - HEADER_SIZE
        if (payloadBytes.isEmpty()) {
            return listOf(buildFrame(messageId, 0, 1, ByteArray(0)))
        }
        val totalChunks = ((payloadBytes.size + chunkDataCapacity - 1) / chunkDataCapacity)
            .coerceIn(1, MAX_CHUNKS)
        val frames = ArrayList<ByteArray>(totalChunks)
        var offset = 0
        for (index in 0 until totalChunks) {
            val remaining = payloadBytes.size - offset
            val len = minOf(chunkDataCapacity, remaining)
            val slice = payloadBytes.copyOfRange(offset, offset + len)
            frames.add(buildFrame(messageId, index, totalChunks, slice))
            offset += len
        }
        return frames
    }

    private fun buildFrame(messageId: Int, index: Int, total: Int, slice: ByteArray): ByteArray {
        val frame = ByteArray(HEADER_SIZE + slice.size)
        frame[0] = MAGIC_0
        frame[1] = MAGIC_1
        frame[2] = ((messageId ushr 8) and 0xFF).toByte()
        frame[3] = (messageId and 0xFF).toByte()
        frame[4] = ((index ushr 8) and 0xFF).toByte()
        frame[5] = (index and 0xFF).toByte()
        frame[6] = ((total ushr 8) and 0xFF).toByte()
        frame[7] = (total and 0xFF).toByte()
        System.arraycopy(slice, 0, frame, HEADER_SIZE, slice.size)
        return frame
    }

    class ReassemblyBuffer {
        private data class PendingMessage(
            val totalChunks: Int,
            val chunks: MutableMap<Int, ByteArray> = mutableMapOf(),
            var totalBytes: Int = 0
        )

        private val pending = ConcurrentHashMap<String, PendingMessage>()

        /**
         * Consumes an incoming BLE characteristic write payload.
         * Returns the reassembled UTF-8 string once all chunks have arrived, or `null` if waiting for more chunks.
         * Unframed UTF-8 packets (e.g. direct JSON writes) are returned immediately.
         */
        @Synchronized
        fun accept(senderKey: String, packet: ByteArray): String? {
            if (packet.size < HEADER_SIZE || packet[0] != MAGIC_0 || packet[1] != MAGIC_1) {
                return String(packet, Charsets.UTF_8)
            }
            val msgId = ((packet[2].toInt() and 0xFF) shl 8) or (packet[3].toInt() and 0xFF)
            val chunkIndex = ((packet[4].toInt() and 0xFF) shl 8) or (packet[5].toInt() and 0xFF)
            val totalChunks = ((packet[6].toInt() and 0xFF) shl 8) or (packet[7].toInt() and 0xFF)

            if (totalChunks <= 0 || totalChunks > MAX_CHUNKS || chunkIndex >= totalChunks) {
                return null
            }

            val slice = packet.copyOfRange(HEADER_SIZE, packet.size)
            if (totalChunks == 1) {
                return String(slice, Charsets.UTF_8)
            }

            val key = "$senderKey:$msgId"
            val entry = pending.getOrPut(key) { PendingMessage(totalChunks) }
            if (entry.totalChunks != totalChunks) {
                pending.remove(key)
                return null
            }
            if (!entry.chunks.containsKey(chunkIndex)) {
                if (entry.totalBytes + slice.size > MAX_TOTAL_BYTES) {
                    pending.remove(key)
                    return null
                }
                entry.chunks[chunkIndex] = slice
                entry.totalBytes += slice.size
            }

            if (entry.chunks.size == totalChunks) {
                pending.remove(key)
                val out = ByteArrayOutputStream(entry.totalBytes)
                for (i in 0 until totalChunks) {
                    val part = entry.chunks[i] ?: return null
                    out.write(part)
                }
                return out.toString(Charsets.UTF_8.name())
            }
            return null
        }

        @Synchronized
        fun clear() {
            pending.clear()
        }
    }
}

/**
 * Manages Primary <-> Secondary Bluetooth connections (Classic RFCOMM & BLE GATT)
 * and provides persistent, bidirectional data relaying across paired devices.
 */
class BluetoothManager(private val context: Context) {

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
        val BLE_BRIDGE_SERVICE_UUID: UUID = UUID.fromString("6E400001-B5A3-F393-E0A9-E50E24DCCA9E")
        val BLE_BRIDGE_RX_CHAR_UUID: UUID = UUID.fromString("6E400002-B5A3-F393-E0A9-E50E24DCCA9E")
        val BLE_BRIDGE_TX_CHAR_UUID: UUID = UUID.fromString("6E400003-B5A3-F393-E0A9-E50E24DCCA9E")
        val CLIENT_CONFIG_DESCRIPTOR_UUID: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
        const val SERVICE_NAME = "eSkyTellBridge"
        private const val APPLE_MANUFACTURER_ID = 0x004C
        private const val CONTROL_ACK_KEY = "_esky_ctrl"
        private const val MAX_RFCOMM_FRAME_BYTES = 10_000_000
        private const val PREFS_NAME = "eskytell_bluetooth_prefs"
        private const val KEY_AUTO_RECONNECT = "auto_reconnect_enabled"
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val systemBluetoothManager: AndroidBluetoothManager? =
        context.getSystemService(Context.BLUETOOTH_SERVICE) as? AndroidBluetoothManager
    private val bluetoothAdapter: BluetoothAdapter? = systemBluetoothManager?.adapter

    private val _isBluetoothSupported = MutableStateFlow(bluetoothAdapter != null)
    val isBluetoothSupported: StateFlow<Boolean> = _isBluetoothSupported.asStateFlow()

    private val _isBluetoothEnabled = MutableStateFlow(bluetoothAdapter?.isEnabled == true)
    val isBluetoothEnabled: StateFlow<Boolean> = _isBluetoothEnabled.asStateFlow()

    private val _hasPermissions = MutableStateFlow(checkRequiredPermissions())
    val hasPermissions: StateFlow<Boolean> = _hasPermissions.asStateFlow()

    private val _permissionSummary = MutableStateFlow(buildPermissionSummary())
    val permissionSummary: StateFlow<PermissionSummary> = _permissionSummary.asStateFlow()

    private val _isScanning = MutableStateFlow(false)
    val isScanning: StateFlow<Boolean> = _isScanning.asStateFlow()

    private val _pairedDevices = MutableStateFlow<List<DiscoveredBluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<DiscoveredBluetoothDevice>> = _pairedDevices.asStateFlow()

    private val _nearbyDevices = MutableStateFlow<List<DiscoveredBluetoothDevice>>(emptyList())
    val nearbyDevices: StateFlow<List<DiscoveredBluetoothDevice>> = _nearbyDevices.asStateFlow()

    private val _deviceRole = MutableStateFlow(RelayDeviceRole.PRIMARY)
    val deviceRole: StateFlow<RelayDeviceRole> = _deviceRole.asStateFlow()

    private val _connectionState = MutableStateFlow<RelayConnectionState>(RelayConnectionState.Disconnected)
    val connectionState: StateFlow<RelayConnectionState> = _connectionState.asStateFlow()

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _autoReconnectEnabled = MutableStateFlow(prefs.getBoolean(KEY_AUTO_RECONNECT, true))
    val autoReconnectEnabled: StateFlow<Boolean> = _autoReconnectEnabled.asStateFlow()

    private val _incomingPayloads = MutableSharedFlow<RelayedEnvelope>(extraBufferCapacity = 32)
    val incomingPayloads: SharedFlow<RelayedEnvelope> = _incomingPayloads.asSharedFlow()

    // Auto-reconnect tracking
    private var lastConnectedDevice: DiscoveredBluetoothDevice? = null
    private var lastConnectedTransport: RelayTransport = RelayTransport.AUTO
    @Volatile
    private var manualDisconnect: Boolean = false
    private var reconnectJob: Job? = null
    internal var reconnectRetryDelaysMs: List<Long> = listOf(2_000L, 4_000L, 8_000L, 12_000L, 15_000L)
    internal var reconnectAttemptOverride: (suspend (DiscoveredBluetoothDevice, RelayTransport) -> Result<Unit>)? = null

    // Active RFCOMM state
    private var serverSocket: BluetoothServerSocket? = null
    private var serverJob: Job? = null
    private var activeRfcommSocket: BluetoothSocket? = null
    private var activeRfcommOutput: DataOutputStream? = null
    private var activeRfcommReaderJob: Job? = null
    private var activePeerDevice: DiscoveredBluetoothDevice? = null
    private val rfcommWriteMutex = Mutex()

    // Active BLE GATT Server state
    private var gattServer: BluetoothGattServer? = null
    private var serverTxCharacteristic: BluetoothGattCharacteristic? = null
    private var activeAdvertiseCallback: AdvertiseCallback? = null
    private val subscribedBleClients = ConcurrentHashMap<String, BluetoothDevice>()

    // Active BLE GATT Client state
    private var activeClientGatt: BluetoothGatt? = null
    private var activeClientRxChar: BluetoothGattCharacteristic? = null
    private var activeBlePeerDevice: DiscoveredBluetoothDevice? = null
    private var negotiatedAttPayloadBytes: Int = 20
    private var pendingGattConnectDeferred: CompletableDeferred<Boolean>? = null
    private var pendingGattWriteDeferred: CompletableDeferred<Int>? = null
    private val bleWriteMutex = Mutex()

    private val bleReassembler = BleRelayFramer.ReassemblyBuffer()
    private var activeBleScanCallback: ScanCallback? = null
    private var receiverRegistered = false

    private val discoveryReceiver = object : BroadcastReceiver() {
        @SuppressLint("MissingPermission")
        override fun onReceive(ctx: Context?, intent: Intent?) {
            when (intent?.action) {
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    _isBluetoothEnabled.value = (state == BluetoothAdapter.STATE_ON)
                    if (state == BluetoothAdapter.STATE_ON) {
                        refreshPairedDevices()
                    } else if (state == BluetoothAdapter.STATE_OFF) {
                        _isScanning.value = false
                        _pairedDevices.value = emptyList()
                        disconnect()
                    }
                }

                BluetoothDevice.ACTION_FOUND -> {
                    val device: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE)
                        .takeIf { it != Short.MIN_VALUE }?.toInt()
                    if (device != null && checkConnectPermission()) {
                        upsertDiscoveredDevice(
                            device = device,
                            rssi = rssi,
                            isBle = false,
                            isAppleManufacturer = false
                        )
                    }
                }

                BluetoothDevice.ACTION_BOND_STATE_CHANGED,
                BluetoothDevice.ACTION_ACL_CONNECTED -> {
                    refreshPairedDevices()
                }

                BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                    val aclDevice: BluetoothDevice? = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val currentPeer = activePeerDevice ?: activeBlePeerDevice
                    if (aclDevice?.address != null &&
                        currentPeer?.address == aclDevice.address &&
                        !manualDisconnect &&
                        (_connectionState.value is RelayConnectionState.Connected ||
                            _connectionState.value is RelayConnectionState.Relaying)
                    ) {
                        val lostTransport = ( _connectionState.value as? RelayConnectionState.Connected )?.transport
                            ?: lastConnectedTransport
                        handleSignalLost(currentPeer, lostTransport)
                    }
                    refreshPairedDevices()
                }

                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    _isScanning.value = false
                }
            }
        }
    }

    init {
        registerReceivers()
        refreshStatus()
    }

    fun setDeviceRole(role: RelayDeviceRole) {
        _deviceRole.value = role
        val current = _connectionState.value
        if (current is RelayConnectionState.Listening) {
            _connectionState.value = current.copy(role = role)
        }
    }

    fun setAutoReconnectEnabled(enabled: Boolean) {
        _autoReconnectEnabled.value = enabled
        prefs.edit().putBoolean(KEY_AUTO_RECONNECT, enabled).apply()
        if (!enabled) {
            cancelAutoReconnect()
        }
    }

    fun cancelAutoReconnect() {
        reconnectJob?.cancel()
        reconnectJob = null
        if (_connectionState.value is RelayConnectionState.Reconnecting) {
            _connectionState.value = RelayConnectionState.Disconnected
        }
    }

    fun requiredPermissions(): Array<String> {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }
    }

    fun allCompliantPermissions(): Array<String> {
        val list = mutableListOf<String>()
        list.addAll(requiredPermissions())
        list.add(Manifest.permission.READ_CONTACTS)
        list.add(Manifest.permission.WRITE_CONTACTS)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            list.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        return list.distinct().toTypedArray()
    }

    fun checkRequiredPermissions(): Boolean {
        return requiredPermissions().all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkContactsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.READ_CONTACTS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun checkNotificationsPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun buildPermissionSummary(): PermissionSummary {
        return PermissionSummary(
            bluetoothGranted = checkRequiredPermissions(),
            contactsGranted = checkContactsPermission(),
            notificationsGranted = checkNotificationsPermission()
        )
    }

    private fun checkConnectPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_CONNECT
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun checkScanPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_SCAN
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    private fun checkAdvertisePermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.BLUETOOTH_ADVERTISE
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    fun refreshStatus() {
        _isBluetoothSupported.value = bluetoothAdapter != null
        _isBluetoothEnabled.value = bluetoothAdapter?.isEnabled == true
        _hasPermissions.value = checkRequiredPermissions()
        _permissionSummary.value = buildPermissionSummary()
        refreshPairedDevices()
    }

    @SuppressLint("MissingPermission")
    fun refreshPairedDevices() {
        if (!checkConnectPermission() || bluetoothAdapter?.isEnabled != true) {
            _pairedDevices.value = emptyList()
            return
        }
        val bonded = try {
            bluetoothAdapter.bondedDevices.orEmpty()
        } catch (_: SecurityException) {
            emptySet()
        }
        val connectedGattAddresses = try {
            systemBluetoothManager?.getConnectedDevices(BluetoothProfile.GATT)
                ?.map { it.address }
                ?.toSet()
                .orEmpty()
        } catch (_: Exception) {
            emptySet()
        }

        _pairedDevices.value = bonded.map { device ->
            val name = safeDeviceName(device)
            val isAclConnected = try {
                val method = device.javaClass.getMethod("isConnected")
                (method.invoke(device) as? Boolean) == true
            } catch (_: Exception) {
                false
            }
            val isRelayConnected = activePeerDevice?.address == device.address ||
                activeBlePeerDevice?.address == device.address
            DiscoveredBluetoothDevice(
                name = name,
                address = device.address,
                isPaired = true,
                rssi = null,
                platform = inferPlatform(name, device, isAppleManufacturer = false),
                isBle = false,
                isConnected = isRelayConnected || isAclConnected || connectedGattAddresses.contains(device.address)
            )
        }.sortedWith(
            compareByDescending<DiscoveredBluetoothDevice> { it.isConnected }
                .thenBy { it.name }
        )
    }

    @SuppressLint("MissingPermission")
    fun startScan() {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled || !checkScanPermission()) {
            refreshStatus()
            return
        }
        stopScan()
        _nearbyDevices.value = emptyList()
        _isScanning.value = true

        try {
            if (bluetoothAdapter.isDiscovering) {
                bluetoothAdapter.cancelDiscovery()
            }
            bluetoothAdapter.startDiscovery()
        } catch (_: SecurityException) {
        }

        try {
            val scanner = bluetoothAdapter.bluetoothLeScanner
            if (scanner != null) {
                val callback = object : ScanCallback() {
                    override fun onScanResult(callbackType: Int, result: ScanResult?) {
                        val device = result?.device ?: return
                        if (!checkConnectPermission()) return
                        val hasAppleId =
                            result.scanRecord?.getManufacturerSpecificData(APPLE_MANUFACTURER_ID) != null
                        val localName = result.scanRecord?.deviceName
                        upsertDiscoveredDevice(
                            device = device,
                            rssi = result.rssi,
                            isBle = true,
                            isAppleManufacturer = hasAppleId,
                            overrideName = localName
                        )
                    }
                }
                activeBleScanCallback = callback
                val settings = ScanSettings.Builder()
                    .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                    .build()
                scanner.startScan(null, settings, callback)
            }
        } catch (_: Exception) {
        }
    }

    @SuppressLint("MissingPermission")
    fun stopScan() {
        _isScanning.value = false
        if (!checkScanPermission()) return
        try {
            if (bluetoothAdapter?.isDiscovering == true) {
                bluetoothAdapter.cancelDiscovery()
            }
        } catch (_: SecurityException) {
        }
        try {
            activeBleScanCallback?.let { callback ->
                bluetoothAdapter?.bluetoothLeScanner?.stopScan(callback)
            }
        } catch (_: Exception) {
        }
        activeBleScanCallback = null
    }

    @SuppressLint("MissingPermission")
    fun pairDevice(address: String): Result<Unit> {
        if (!checkConnectPermission() || bluetoothAdapter == null) {
            return Result.failure(IllegalStateException("دسترسی بلوتوث یا آداپتور در دسترس نیست."))
        }
        return try {
            stopScan()
            val device = bluetoothAdapter.getRemoteDevice(address)
            if (device.bondState == BluetoothDevice.BOND_NONE) {
                device.createBond()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            _connectionState.value = RelayConnectionState.Error(
                "امکان جفت‌سازی با این دستگاه وجود ندارد: ${e.localizedMessage}"
            )
            Result.failure(e)
        }
    }

    /**
     * Starts persistent RFCOMM + BLE GATT relay server so a Primary or Secondary peer can connect
     * and exchange bidirectional relay packets continuously.
     */
    @SuppressLint("MissingPermission")
    fun startListening(role: RelayDeviceRole = _deviceRole.value) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled || !checkConnectPermission()) {
            _connectionState.value = RelayConnectionState.Error(
                "برای فعال‌سازی پل زنده، بلوتوث و دسترسی اتصال را فعال کنید."
            )
            return
        }
        _deviceRole.value = role
        stopListeningServerOnly()

        val localName = try {
            bluetoothAdapter.name ?: "این دستگاه"
        } catch (_: SecurityException) {
            "این دستگاه"
        }
        _connectionState.value = RelayConnectionState.Listening(role = role, localDeviceName = localName)

        startBleGattRelayServer()

        serverJob = scope.launch {
            try {
                val srv = bluetoothAdapter.listenUsingRfcommWithServiceRecord(SERVICE_NAME, SERVICE_UUID)
                serverSocket = srv
                while (isActive) {
                    val socket = try {
                        srv.accept()
                    } catch (_: IOException) {
                        break
                    } ?: break

                    handleAcceptedRfcommSocket(socket, localName)
                }
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                if (gattServer == null) {
                    _connectionState.value = RelayConnectionState.Error(
                        "عدم امکان راه‌اندازی پل بلوتوث: ${e.localizedMessage ?: "خطای سوکت"}"
                    )
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun handleAcceptedRfcommSocket(socket: BluetoothSocket, localName: String) {
        val remoteDevice = socket.remoteDevice
        val remoteAddress = remoteDevice?.address ?: "RFCOMM-PEER"
        val remoteName = try {
            remoteDevice?.name ?: remoteAddress
        } catch (_: SecurityException) {
            remoteAddress
        }
        val peerPlatform = if (remoteDevice != null) {
            inferPlatform(remoteName, remoteDevice, isAppleManufacturer = false)
        } else {
            DevicePlatform.ANDROID
        }
        val peerModel = DiscoveredBluetoothDevice(
            name = remoteName,
            address = remoteAddress,
            isPaired = true,
            platform = peerPlatform,
            isBle = false,
            isConnected = true
        )

        closeActiveRfcommClient()
        activeRfcommSocket = socket
        activeRfcommOutput = DataOutputStream(socket.outputStream)
        activePeerDevice = peerModel
        _connectionState.value = RelayConnectionState.Connected(
            deviceName = remoteName,
            address = remoteAddress,
            role = _deviceRole.value,
            transport = RelayTransport.RFCOMM
        )
        refreshPairedDevices()

        try {
            readRfcommLoop(
                socket = socket,
                peerName = remoteName,
                peerAddress = remoteAddress
            )
        } finally {
            closeActiveRfcommClient()
            if (serverSocket != null && serverJob?.isActive == true) {
                _connectionState.value = RelayConnectionState.Listening(
                    role = _deviceRole.value,
                    localDeviceName = localName
                )
            } else if (_connectionState.value is RelayConnectionState.Connected ||
                _connectionState.value is RelayConnectionState.Relaying
            ) {
                _connectionState.value = RelayConnectionState.Disconnected
            }
            refreshPairedDevices()
        }
    }

    /**
     * Establishes a persistent connection to a peer Primary or Secondary device over RFCOMM or BLE GATT
     * so subsequent relay calls and incoming peer events reuse the active channel.
     */
    @SuppressLint("MissingPermission")
    suspend fun connect(
        device: DiscoveredBluetoothDevice,
        transport: RelayTransport = RelayTransport.AUTO
    ): Result<Unit> = withContext(Dispatchers.IO) {
        manualDisconnect = false
        cancelAutoReconnect()
        connectInternal(device, transport)
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectInternal(
        device: DiscoveredBluetoothDevice,
        transport: RelayTransport
    ): Result<Unit> = withContext(Dispatchers.IO) {
        if (bluetoothAdapter == null || !bluetoothAdapter.isEnabled || !checkConnectPermission()) {
            val msg = "بلوتوث خاموش است یا دسترسی اتصال صادر نشده است."
            _connectionState.value = RelayConnectionState.Error(msg)
            return@withContext Result.failure(IllegalStateException(msg))
        }

        stopScan()
        lastConnectedDevice = device
        lastConnectedTransport = transport
        _connectionState.value = RelayConnectionState.Connecting(device.name, device.address)

        val preferBle = when (transport) {
            RelayTransport.BLE_GATT -> true
            RelayTransport.RFCOMM -> false
            RelayTransport.AUTO -> device.platform == DevicePlatform.IPHONE || device.isBle
        }

        if (!preferBle) {
            val rfcommResult = connectRfcommInternal(device)
            if (rfcommResult.isSuccess || transport == RelayTransport.RFCOMM) {
                return@withContext rfcommResult
            }
        }

        val bleResult = connectBleGattInternal(device)
        if (bleResult.isFailure && transport == RelayTransport.AUTO && preferBle) {
            return@withContext connectRfcommInternal(device)
        }
        bleResult
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectRfcommInternal(device: DiscoveredBluetoothDevice): Result<Unit> {
        closeActiveRfcommClient()
        var socket: BluetoothSocket? = null
        return try {
            val remote = bluetoothAdapter?.getRemoteDevice(device.address)
                ?: return Result.failure(IllegalStateException("آداپتور بلوتوث در دسترس نیست."))
            socket = remote.createRfcommSocketToServiceRecord(SERVICE_UUID)
            socket.connect()

            activeRfcommSocket = socket
            activeRfcommOutput = DataOutputStream(socket.outputStream)
            activePeerDevice = device.copy(isConnected = true)
            lastConnectedDevice = activePeerDevice
            lastConnectedTransport = RelayTransport.RFCOMM
            _connectionState.value = RelayConnectionState.Connected(
                deviceName = device.name,
                address = device.address,
                role = _deviceRole.value,
                transport = RelayTransport.RFCOMM
            )
            refreshPairedDevices()

            activeRfcommReaderJob = scope.launch {
                val droppedPeer = activePeerDevice
                try {
                    readRfcommLoop(
                        socket = socket,
                        peerName = device.name,
                        peerAddress = device.address
                    )
                } finally {
                    closeActiveRfcommClient()
                    val wasConnectedOrRelaying = _connectionState.value is RelayConnectionState.Connected ||
                        _connectionState.value is RelayConnectionState.Relaying
                    if (wasConnectedOrRelaying) {
                        handleSignalLost(droppedPeer, RelayTransport.RFCOMM)
                    }
                    refreshPairedDevices()
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            if (e is CancellationException) throw e
            try {
                socket?.close()
            } catch (_: IOException) {
            }
            val hint = if (device.platform == DevicePlatform.IPHONE) {
                "برای اتصال با آیفون از حالت «پل زنده BLE GATT» یا «ارسال فایل استاندارد» استفاده کنید."
            } else {
                "اتصال مستقیم به «${device.name}» برقرار نشد. مطمئن شوید در گوشی مقصد حالت «آماده دریافت» فعال است."
            }
            _connectionState.value = RelayConnectionState.Error(hint)
            Result.failure(e)
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun connectBleGattInternal(device: DiscoveredBluetoothDevice): Result<Unit> {
        closeActiveBleClient()
        val adapter = bluetoothAdapter
            ?: return Result.failure(IllegalStateException("آداپتور بلوتوث در دسترس نیست."))

        val readyDeferred = CompletableDeferred<Boolean>()
        pendingGattConnectDeferred = readyDeferred
        negotiatedAttPayloadBytes = 20

        val remote = adapter.getRemoteDevice(device.address)
        val gatt = remote.connectGatt(context, false, object : BluetoothGattCallback() {
            override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS && newState != BluetoothProfile.STATE_CONNECTED) {
                    readyDeferred.complete(false)
                    closeActiveBleClient()
                    if (_connectionState.value is RelayConnectionState.Connected ||
                        _connectionState.value is RelayConnectionState.Connecting
                    ) {
                        _connectionState.value = RelayConnectionState.Disconnected
                    }
                    refreshPairedDevices()
                    return
                }

                when (newState) {
                    BluetoothProfile.STATE_CONNECTED -> {
                        activeClientGatt = gatt
                        runCatching { gatt.requestMtu(512) }
                        gatt.discoverServices()
                    }
                    BluetoothProfile.STATE_DISCONNECTED -> {
                        val droppedPeer = activeBlePeerDevice ?: device
                        readyDeferred.complete(false)
                        pendingGattWriteDeferred?.complete(BluetoothGatt.GATT_FAILURE)
                        closeActiveBleClient()
                        if (_connectionState.value is RelayConnectionState.Connected ||
                            _connectionState.value is RelayConnectionState.Relaying
                        ) {
                            handleSignalLost(droppedPeer, RelayTransport.BLE_GATT)
                        }
                        refreshPairedDevices()
                    }
                }
            }

            override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int) {
                if (status == BluetoothGatt.GATT_SUCCESS && mtu > 23) {
                    negotiatedAttPayloadBytes = (mtu - 3).coerceIn(20, 480)
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    readyDeferred.complete(false)
                    return
                }
                val service = gatt.getService(BLE_BRIDGE_SERVICE_UUID)
                val rxChar = service?.getCharacteristic(BLE_BRIDGE_RX_CHAR_UUID)
                val txChar = service?.getCharacteristic(BLE_BRIDGE_TX_CHAR_UUID)
                if (rxChar != null) {
                    activeClientRxChar = rxChar
                    activeBlePeerDevice = device.copy(isConnected = true, isBle = true)
                    if (txChar != null) {
                        enableBleTxNotifications(gatt, txChar)
                    }
                    readyDeferred.complete(true)
                } else {
                    readyDeferred.complete(false)
                }
            }

            @Deprecated("Deprecated in Android 13")
            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic
            ) {
                @Suppress("DEPRECATION")
                val value = characteristic.value ?: return
                if (characteristic.uuid == BLE_BRIDGE_TX_CHAR_UUID) {
                    handleIncomingBlePacket(device.address, device.name, value)
                }
            }

            override fun onCharacteristicChanged(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                value: ByteArray
            ) {
                if (characteristic.uuid == BLE_BRIDGE_TX_CHAR_UUID) {
                    handleIncomingBlePacket(device.address, device.name, value)
                }
            }

            override fun onCharacteristicWrite(
                gatt: BluetoothGatt,
                characteristic: BluetoothGattCharacteristic,
                status: Int
            ) {
                pendingGattWriteDeferred?.complete(status)
            }
        })

        if (gatt == null) {
            pendingGattConnectDeferred = null
            val err = IllegalStateException("خطا در شروع اتصال BLE GATT.")
            _connectionState.value = RelayConnectionState.Error(err.message ?: "خطای BLE")
            return Result.failure(err)
        }
        activeClientGatt = gatt

        val connected = withTimeoutOrNull(12_000L) { readyDeferred.await() } ?: false
        pendingGattConnectDeferred = null

        return if (connected && activeClientRxChar != null) {
            lastConnectedDevice = activeBlePeerDevice ?: device.copy(isConnected = true, isBle = true)
            lastConnectedTransport = RelayTransport.BLE_GATT
            _connectionState.value = RelayConnectionState.Connected(
                deviceName = device.name,
                address = device.address,
                role = _deviceRole.value,
                transport = RelayTransport.BLE_GATT
            )
            refreshPairedDevices()
            Result.success(Unit)
        } else {
            closeActiveBleClient()
            val msg = "سرویس پل زنده eSkyTell روی «${device.name}» یافت نشد یا اتصال برقرار نشد."
            _connectionState.value = RelayConnectionState.Error(msg)
            Result.failure(IllegalStateException(msg))
        }
    }

    @SuppressLint("MissingPermission")
    private fun enableBleTxNotifications(gatt: BluetoothGatt, txChar: BluetoothGattCharacteristic) {
        try {
            gatt.setCharacteristicNotification(txChar, true)
            val cccd = txChar.getDescriptor(CLIENT_CONFIG_DESCRIPTOR_UUID) ?: return
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                gatt.writeDescriptor(cccd, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
            } else {
                @Suppress("DEPRECATION")
                cccd.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
                @Suppress("DEPRECATION")
                gatt.writeDescriptor(cccd)
            }
        } catch (_: Exception) {
        }
    }

    /**
     * Relays a payload over the active Primary <-> Secondary link, or connects to [targetDevice]
     * if no persistent link is currently active. Optionally encrypts the payload using [encryptWithPin].
     */
    @SuppressLint("MissingPermission")
    suspend fun relayData(
        payload: String,
        encryptWithPin: String? = null,
        targetDevice: DiscoveredBluetoothDevice? = null,
        transport: RelayTransport = RelayTransport.AUTO
    ): Result<Long> = withContext(Dispatchers.IO) {
        val wirePayload = if (!encryptWithPin.isNullOrBlank()) {
            BridgeCrypto.encryptEnvelope(payload, encryptWithPin)
        } else {
            payload
        }

        // 1. Prefer an already-open RFCOMM socket
        val openRfcomm = activeRfcommSocket
        val openPeer = activePeerDevice
        if (openRfcomm != null && openRfcomm.isConnected) {
            return@withContext writeRfcommFrame(
                output = activeRfcommOutput ?: DataOutputStream(openRfcomm.outputStream),
                peerName = openPeer?.name ?: "دستگاه متصل",
                peerAddress = openPeer?.address ?: "",
                wirePayload = wirePayload,
                restoreConnected = true
            )
        }

        // 2. Prefer an already-open BLE GATT client link
        val openGatt = activeClientGatt
        val openRxChar = activeClientRxChar
        val blePeer = activeBlePeerDevice
        if (openGatt != null && openRxChar != null) {
            return@withContext writeBleGattFrames(
                gatt = openGatt,
                rxChar = openRxChar,
                peerName = blePeer?.name ?: "دستگاه BLE",
                peerAddress = blePeer?.address ?: "",
                wirePayload = wirePayload,
                restoreConnected = true
            )
        }

        // 3. If acting as BLE GATT server with subscribed peers, push via TX notifications
        if (gattServer != null && subscribedBleClients.isNotEmpty()) {
            val notifyResult = notifySubscribedBleClients(wirePayload)
            if (notifyResult.isSuccess) return@withContext notifyResult
        }

        // 4. Otherwise connect on demand to targetDevice and send
        val peer = targetDevice
            ?: return@withContext Result.failure(
                IllegalStateException("اتصال فعالی بین دستگاه اصلی و دوم برقرار نیست.")
            )

        val connectRes = connect(peer, transport)
        if (connectRes.isFailure) {
            return@withContext Result.failure(
                connectRes.exceptionOrNull() ?: IllegalStateException("خطا در اتصال به ${peer.name}")
            )
        }

        val connectedRfcomm = activeRfcommSocket
        if (connectedRfcomm != null && connectedRfcomm.isConnected) {
            return@withContext writeRfcommFrame(
                output = activeRfcommOutput ?: DataOutputStream(connectedRfcomm.outputStream),
                peerName = peer.name,
                peerAddress = peer.address,
                wirePayload = wirePayload,
                restoreConnected = true
            )
        }

        val connectedGatt = activeClientGatt
        val connectedRxChar = activeClientRxChar
        if (connectedGatt != null && connectedRxChar != null) {
            return@withContext writeBleGattFrames(
                gatt = connectedGatt,
                rxChar = connectedRxChar,
                peerName = peer.name,
                peerAddress = peer.address,
                wirePayload = wirePayload,
                restoreConnected = true
            )
        }

        Result.failure(IllegalStateException("کانال انتقال بلوتوث آماده نشد."))
    }

    private suspend fun writeRfcommFrame(
        output: DataOutputStream,
        peerName: String,
        peerAddress: String,
        wirePayload: String,
        restoreConnected: Boolean
    ): Result<Long> {
        val bytes = wirePayload.toByteArray(Charsets.UTF_8)
        val total = bytes.size.toLong()
        return rfcommWriteMutex.withLock {
            try {
                output.writeInt(bytes.size)
                val chunkSize = 4096
                var offset = 0
                while (offset < bytes.size) {
                    val len = minOf(chunkSize, bytes.size - offset)
                    output.write(bytes, offset, len)
                    offset += len
                    _connectionState.value = RelayConnectionState.Relaying(
                        deviceName = peerName,
                        progress = if (bytes.isEmpty()) 1f else offset.toFloat() / bytes.size.toFloat(),
                        bytesTransferred = offset.toLong(),
                        totalBytes = total
                    )
                }
                output.flush()
                if (restoreConnected && activeRfcommSocket?.isConnected == true) {
                    _connectionState.value = RelayConnectionState.Connected(
                        deviceName = peerName,
                        address = peerAddress,
                        role = _deviceRole.value,
                        transport = RelayTransport.RFCOMM
                    )
                }
                Result.success(total)
            } catch (e: Exception) {
                _connectionState.value = RelayConnectionState.Error(
                    "خطا در ارسال داده به «$peerName»: ${e.localizedMessage ?: "قطع ارتباط"}"
                )
                Result.failure(e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun writeBleGattFrames(
        gatt: BluetoothGatt,
        rxChar: BluetoothGattCharacteristic,
        peerName: String,
        peerAddress: String,
        wirePayload: String,
        restoreConnected: Boolean
    ): Result<Long> {
        val bytes = wirePayload.toByteArray(Charsets.UTF_8)
        val total = bytes.size.toLong()
        val frames = BleRelayFramer.encodeChunks(
            payloadBytes = bytes,
            maxAttPayloadBytes = negotiatedAttPayloadBytes
        )
        return bleWriteMutex.withLock {
            try {
                var sentBytes = 0L
                frames.forEachIndexed { idx, frame ->
                    val deferred = CompletableDeferred<Int>()
                    pendingGattWriteDeferred = deferred
                    val started = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        val status = gatt.writeCharacteristic(
                            rxChar,
                            frame,
                            BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        )
                        status == android.bluetooth.BluetoothStatusCodes.SUCCESS
                    } else {
                        @Suppress("DEPRECATION")
                        rxChar.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
                        @Suppress("DEPRECATION")
                        rxChar.value = frame
                        @Suppress("DEPRECATION")
                        gatt.writeCharacteristic(rxChar)
                    }
                    if (!started) {
                        pendingGattWriteDeferred = null
                        throw IOException("ارسال بسته BLE متوقف شد.")
                    }
                    val writeStatus = withTimeoutOrNull(8_000L) { deferred.await() }
                    pendingGattWriteDeferred = null
                    if (writeStatus != BluetoothGatt.GATT_SUCCESS) {
                        throw IOException("خطای GATT در نوشتن بسته (کد $writeStatus).")
                    }
                    val payloadPart = (frame.size - BleRelayFramer.HEADER_SIZE).coerceAtLeast(0)
                    sentBytes = (sentBytes + payloadPart).coerceAtMost(total)
                    _connectionState.value = RelayConnectionState.Relaying(
                        deviceName = peerName,
                        progress = (idx + 1).toFloat() / frames.size.toFloat(),
                        bytesTransferred = sentBytes,
                        totalBytes = total
                    )
                }
                if (restoreConnected && activeClientGatt != null) {
                    _connectionState.value = RelayConnectionState.Connected(
                        deviceName = peerName,
                        address = peerAddress,
                        role = _deviceRole.value,
                        transport = RelayTransport.BLE_GATT
                    )
                }
                Result.success(total)
            } catch (e: Exception) {
                pendingGattWriteDeferred = null
                _connectionState.value = RelayConnectionState.Error(
                    "خطا در رله BLE به «$peerName»: ${e.localizedMessage ?: "خطای ارتباط"}"
                )
                Result.failure(e)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun notifySubscribedBleClients(wirePayload: String): Result<Long> {
        val server = gattServer ?: return Result.failure(IllegalStateException("سرور BLE فعال نیست."))
        val txChar = serverTxCharacteristic ?: return Result.failure(IllegalStateException("مشخصه TX یافت نشد."))
        val clients = subscribedBleClients.values.toList()
        if (clients.isEmpty()) {
            return Result.failure(IllegalStateException("دستگاه متصل به سرور BLE وجود ندارد."))
        }
        val bytes = wirePayload.toByteArray(Charsets.UTF_8)
        val frames = BleRelayFramer.encodeChunks(bytes, maxAttPayloadBytes = 180)
        return bleWriteMutex.withLock {
            try {
                clients.forEach { device ->
                    frames.forEach { frame ->
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            server.notifyCharacteristicChanged(device, txChar, false, frame)
                        } else {
                            @Suppress("DEPRECATION")
                            txChar.value = frame
                            @Suppress("DEPRECATION")
                            server.notifyCharacteristicChanged(device, txChar, false)
                        }
                    }
                }
                Result.success(bytes.size.toLong())
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun readRfcommLoop(
        socket: BluetoothSocket,
        peerName: String,
        peerAddress: String
    ) {
        val input = DataInputStream(socket.inputStream)
        while (socket.isConnected) {
            val totalBytes = try {
                input.readInt()
            } catch (_: IOException) {
                break
            }
            if (totalBytes !in 1..MAX_RFCOMM_FRAME_BYTES) {
                break
            }
            val buffer = ByteArray(totalBytes)
            var readSoFar = 0
            while (readSoFar < totalBytes) {
                val chunk = try {
                    input.read(buffer, readSoFar, totalBytes - readSoFar)
                } catch (_: IOException) {
                    -1
                }
                if (chunk <= 0) break
                readSoFar += chunk
                _connectionState.value = RelayConnectionState.Relaying(
                    deviceName = peerName,
                    progress = readSoFar.toFloat() / totalBytes.toFloat(),
                    bytesTransferred = readSoFar.toLong(),
                    totalBytes = totalBytes.toLong()
                )
            }
            if (readSoFar != totalBytes) break

            val rawText = String(buffer, 0, readSoFar, Charsets.UTF_8)
            _connectionState.value = RelayConnectionState.Connected(
                deviceName = peerName,
                address = peerAddress,
                role = _deviceRole.value,
                transport = RelayTransport.RFCOMM
            )

            if (isControlAck(rawText)) {
                continue
            }

            // Send lightweight length-prefixed control ACK so sender knows frame arrived
            sendControlAckOverRfcomm(socket)

            val envelope = buildIncomingEnvelope(
                senderAddress = peerAddress,
                senderName = peerName,
                transport = RelayTransport.RFCOMM,
                payload = rawText
            )
            _incomingPayloads.emit(envelope)
        }
    }

    private fun isControlAck(rawText: String): Boolean {
        return try {
            val json = JSONObject(rawText)
            json.optString(CONTROL_ACK_KEY) == "ACK"
        } catch (_: Exception) {
            rawText == "ACK"
        }
    }

    private suspend fun sendControlAckOverRfcomm(socket: BluetoothSocket) {
        val ackBytes = JSONObject().put(CONTROL_ACK_KEY, "ACK").toString().toByteArray(Charsets.UTF_8)
        rfcommWriteMutex.withLock {
            runCatching {
                val out = activeRfcommOutput ?: DataOutputStream(socket.outputStream)
                out.writeInt(ackBytes.size)
                out.write(ackBytes)
                out.flush()
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startBleGattRelayServer() {
        if (systemBluetoothManager == null || !checkConnectPermission()) return
        try {
            val callback = object : BluetoothGattServerCallback() {
                override fun onConnectionStateChange(device: BluetoothDevice, status: Int, newState: Int) {
                    val addr = device.address ?: return
                    val name = safeDeviceName(device)
                    if (newState == BluetoothProfile.STATE_CONNECTED) {
                        _connectionState.value = RelayConnectionState.Connected(
                            deviceName = name,
                            address = addr,
                            role = _deviceRole.value,
                            transport = RelayTransport.BLE_GATT
                        )
                    } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                        subscribedBleClients.remove(addr)
                        if (subscribedBleClients.isEmpty() && serverSocket != null) {
                            val localName = runCatching { bluetoothAdapter?.name }.getOrNull() ?: "این دستگاه"
                            _connectionState.value = RelayConnectionState.Listening(
                                role = _deviceRole.value,
                                localDeviceName = localName
                            )
                        }
                    }
                }

                override fun onDescriptorWriteRequest(
                    device: BluetoothDevice,
                    requestId: Int,
                    descriptor: BluetoothGattDescriptor,
                    preparedWrite: Boolean,
                    responseNeeded: Boolean,
                    offset: Int,
                    value: ByteArray?
                ) {
                    if (descriptor.uuid == CLIENT_CONFIG_DESCRIPTOR_UUID) {
                        val enabled = value != null &&
                            value.contentEquals(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                        if (enabled) {
                            subscribedBleClients[device.address] = device
                        } else {
                            subscribedBleClients.remove(device.address)
                        }
                    }
                    if (responseNeeded) {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                    }
                }

                override fun onCharacteristicWriteRequest(
                    device: BluetoothDevice,
                    requestId: Int,
                    characteristic: BluetoothGattCharacteristic,
                    preparedWrite: Boolean,
                    responseNeeded: Boolean,
                    offset: Int,
                    value: ByteArray?
                ) {
                    if (characteristic.uuid == BLE_BRIDGE_RX_CHAR_UUID && value != null) {
                        val sender = safeDeviceName(device)
                        handleIncomingBlePacket(device.address, sender, value)
                    }
                    if (responseNeeded) {
                        gattServer?.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, value)
                    }
                }
            }

            val server = systemBluetoothManager.openGattServer(context, callback) ?: return
            val service = BluetoothGattService(
                BLE_BRIDGE_SERVICE_UUID,
                BluetoothGattService.SERVICE_TYPE_PRIMARY
            )
            val rxChar = BluetoothGattCharacteristic(
                BLE_BRIDGE_RX_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_WRITE or BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE,
                BluetoothGattCharacteristic.PERMISSION_WRITE
            )
            val txChar = BluetoothGattCharacteristic(
                BLE_BRIDGE_TX_CHAR_UUID,
                BluetoothGattCharacteristic.PROPERTY_READ or BluetoothGattCharacteristic.PROPERTY_NOTIFY,
                BluetoothGattCharacteristic.PERMISSION_READ
            )
            val cccd = BluetoothGattDescriptor(
                CLIENT_CONFIG_DESCRIPTOR_UUID,
                BluetoothGattDescriptor.PERMISSION_READ or BluetoothGattDescriptor.PERMISSION_WRITE
            )
            txChar.addDescriptor(cccd)
            service.addCharacteristic(rxChar)
            service.addCharacteristic(txChar)
            server.addService(service)
            serverTxCharacteristic = txChar
            gattServer = server

            if (checkAdvertisePermission()) {
                val advertiser = bluetoothAdapter?.bluetoothLeAdvertiser
                if (advertiser != null) {
                    val settings = AdvertiseSettings.Builder()
                        .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                        .setConnectable(true)
                        .build()
                    val data = AdvertiseData.Builder()
                        .setIncludeDeviceName(true)
                        .addServiceUuid(ParcelUuid(BLE_BRIDGE_SERVICE_UUID))
                        .build()
                    val advCallback = object : AdvertiseCallback() {}
                    activeAdvertiseCallback = advCallback
                    advertiser.startAdvertising(settings, data, advCallback)
                }
            }
        } catch (_: Exception) {
        }
    }

    private fun handleIncomingBlePacket(senderAddress: String, senderName: String, packet: ByteArray) {
        val completePayload = bleReassembler.accept(senderAddress, packet) ?: return
        if (isControlAck(completePayload)) return
        scope.launch {
            val envelope = buildIncomingEnvelope(
                senderAddress = senderAddress,
                senderName = senderName,
                transport = RelayTransport.BLE_GATT,
                payload = completePayload
            )
            _incomingPayloads.emit(envelope)
        }
    }

    private fun buildIncomingEnvelope(
        senderAddress: String,
        senderName: String,
        transport: RelayTransport,
        payload: String
    ): RelayedEnvelope {
        val peerRole = if (_deviceRole.value == RelayDeviceRole.PRIMARY) {
            RelayDeviceRole.SECONDARY
        } else {
            RelayDeviceRole.PRIMARY
        }
        val isEncrypted = runCatching {
            JSONObject(payload).optString("enc") == "AES-256-GCM"
        }.getOrDefault(false)

        return RelayedEnvelope(
            id = UUID.randomUUID().toString(),
            senderAddress = senderAddress,
            senderName = senderName,
            senderRole = peerRole,
            transport = transport,
            payload = payload,
            encrypted = isEncrypted
        )
    }

    @SuppressLint("MissingPermission")
    private fun stopListeningServerOnly() {
        serverJob?.cancel()
        serverJob = null
        try {
            serverSocket?.close()
        } catch (_: IOException) {
        }
        serverSocket = null

        try {
            if (checkAdvertisePermission()) {
                activeAdvertiseCallback?.let { cb ->
                    bluetoothAdapter?.bluetoothLeAdvertiser?.stopAdvertising(cb)
                }
            }
        } catch (_: Exception) {
        }
        activeAdvertiseCallback = null

        try {
            gattServer?.close()
        } catch (_: Exception) {
        }
        gattServer = null
        serverTxCharacteristic = null
        subscribedBleClients.clear()
    }

    private fun closeActiveRfcommClient() {
        activeRfcommReaderJob?.cancel()
        activeRfcommReaderJob = null
        try {
            activeRfcommOutput?.close()
        } catch (_: Exception) {
        }
        activeRfcommOutput = null
        try {
            activeRfcommSocket?.close()
        } catch (_: IOException) {
        }
        activeRfcommSocket = null
        activePeerDevice = null
    }

    @SuppressLint("MissingPermission")
    private fun closeActiveBleClient() {
        pendingGattConnectDeferred?.cancel()
        pendingGattConnectDeferred = null
        pendingGattWriteDeferred?.cancel()
        pendingGattWriteDeferred = null
        try {
            if (checkConnectPermission()) {
                activeClientGatt?.disconnect()
            }
            activeClientGatt?.close()
        } catch (_: Exception) {
        }
        activeClientGatt = null
        activeClientRxChar = null
        activeBlePeerDevice = null
    }

    internal fun handleSignalLost(
        lostDevice: DiscoveredBluetoothDevice?,
        transport: RelayTransport
    ) {
        val target = lostDevice ?: lastConnectedDevice
        val canRetryHardware = (bluetoothAdapter?.isEnabled == true && checkConnectPermission()) ||
            reconnectAttemptOverride != null
        if (!_autoReconnectEnabled.value || manualDisconnect || target == null || !canRetryHardware) {
            _connectionState.value = RelayConnectionState.Disconnected
            return
        }
        if (reconnectJob?.isActive == true) return

        val delays = reconnectRetryDelaysMs.ifEmpty { listOf(2_000L) }
        val maxAttempts = delays.size
        reconnectJob = scope.launch {
            try {
                for ((idx, delayMs) in delays.withIndex()) {
                    if (!isActive || !_autoReconnectEnabled.value || manualDisconnect) return@launch
                    val attempt = idx + 1
                    _connectionState.value = RelayConnectionState.Reconnecting(
                        deviceName = target.name,
                        address = target.address,
                        attempt = attempt,
                        maxAttempts = maxAttempts,
                        nextRetrySeconds = ((delayMs + 999L) / 1000L).toInt().coerceAtLeast(1)
                    )
                    delay(delayMs)
                    if (!isActive || !_autoReconnectEnabled.value || manualDisconnect) return@launch

                    val res = reconnectAttemptOverride?.invoke(target, transport)
                        ?: connectInternal(target, transport)
                    if (res.isSuccess) {
                        if (_connectionState.value !is RelayConnectionState.Connected) {
                            lastConnectedDevice = target.copy(isConnected = true)
                            lastConnectedTransport = transport
                            _connectionState.value = RelayConnectionState.Connected(
                                deviceName = target.name,
                                address = target.address,
                                role = _deviceRole.value,
                                transport = transport
                            )
                        }
                        return@launch
                    }
                }
                if (isActive && _autoReconnectEnabled.value && !manualDisconnect) {
                    _connectionState.value = RelayConnectionState.Error(
                        "اتصال به «${target.name}» پس از $maxAttempts تلاش مجدد برقرار نشد."
                    )
                }
            } finally {
                reconnectJob = null
            }
        }
    }

    fun disconnect() {
        manualDisconnect = true
        cancelAutoReconnect()
        lastConnectedDevice = null
        stopListeningServerOnly()
        closeActiveRfcommClient()
        closeActiveBleClient()
        bleReassembler.clear()
        _connectionState.value = RelayConnectionState.Disconnected
        refreshPairedDevices()
    }

    @SuppressLint("MissingPermission")
    private fun upsertDiscoveredDevice(
        device: BluetoothDevice,
        rssi: Int?,
        isBle: Boolean,
        isAppleManufacturer: Boolean,
        overrideName: String? = null
    ) {
        val address = device.address ?: return
        val resolvedName = overrideName?.takeIf { it.isNotBlank() } ?: safeDeviceName(device)
        val platform = inferPlatform(resolvedName, device, isAppleManufacturer)
        val isPaired = try {
            device.bondState == BluetoothDevice.BOND_BONDED
        } catch (_: SecurityException) {
            false
        }

        val item = DiscoveredBluetoothDevice(
            name = resolvedName,
            address = address,
            isPaired = isPaired,
            rssi = rssi,
            platform = platform,
            isBle = isBle,
            isConnected = activePeerDevice?.address == address || activeBlePeerDevice?.address == address
        )

        val current = _nearbyDevices.value.toMutableList()
        val existingIdx = current.indexOfFirst { it.address == address }
        if (existingIdx >= 0) {
            val existing = current[existingIdx]
            val betterName = if (existing.name.startsWith("دستگاه") && !item.name.startsWith("دستگاه")) {
                item.name
            } else {
                existing.name
            }
            val betterPlatform = if (existing.platform == DevicePlatform.OTHER) item.platform else existing.platform
            current[existingIdx] = item.copy(
                name = betterName,
                platform = betterPlatform,
                rssi = rssi ?: existing.rssi
            )
        } else {
            current.add(item)
        }
        _nearbyDevices.value = current.sortedWith(
            compareByDescending<DiscoveredBluetoothDevice> {
                it.platform == DevicePlatform.IPHONE || it.platform == DevicePlatform.ANDROID
            }.thenByDescending { it.rssi ?: -200 }
        )
    }

    @SuppressLint("MissingPermission")
    private fun safeDeviceName(device: BluetoothDevice): String {
        val raw = try {
            device.name
        } catch (_: SecurityException) {
            null
        }
        return raw?.takeIf { it.isNotBlank() } ?: "دستگاه (${device.address.takeLast(5)})"
    }

    @SuppressLint("MissingPermission")
    private fun inferPlatform(
        name: String,
        device: BluetoothDevice,
        isAppleManufacturer: Boolean
    ): DevicePlatform {
        val lower = name.lowercase()
        if (isAppleManufacturer ||
            lower.contains("iphone") ||
            lower.contains("ipad") ||
            lower.contains("apple") ||
            lower.contains("airpod")
        ) {
            return DevicePlatform.IPHONE
        }
        if (lower.contains("macbook") || lower.contains("imac") || lower.contains("windows") || lower.contains("pc")) {
            return DevicePlatform.COMPUTER
        }
        val majorClass = try {
            device.bluetoothClass?.majorDeviceClass
        } catch (_: SecurityException) {
            null
        }
        return when (majorClass) {
            BluetoothClass.Device.Major.PHONE -> DevicePlatform.ANDROID
            BluetoothClass.Device.Major.COMPUTER -> DevicePlatform.COMPUTER
            else -> if (lower.contains("galaxy") || lower.contains("xiaomi") || lower.contains("redmi") ||
                lower.contains("pixel") || lower.contains("poco") || lower.contains("huawei") ||
                lower.contains("honor") || lower.contains("nokia") || lower.contains("oneplus")
            ) {
                DevicePlatform.ANDROID
            } else {
                DevicePlatform.OTHER
            }
        }
    }

    private fun registerReceivers() {
        if (receiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothDevice.ACTION_BOND_STATE_CHANGED)
            addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        ContextCompat.registerReceiver(
            context,
            discoveryReceiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED
        )
        receiverRegistered = true
    }

    fun release() {
        stopScan()
        disconnect()
        if (receiverRegistered) {
            try {
                context.unregisterReceiver(discoveryReceiver)
            } catch (_: IllegalArgumentException) {
            }
            receiverRegistered = false
        }
        scope.cancel()
    }
}
