package com.example.bluetooth

import android.content.Context
import android.content.Intent
import com.example.data.model.BridgeCommandType
import com.example.data.model.DiscoveredBluetoothDevice
import com.example.data.model.PermissionSummary
import com.example.data.model.RelayConnectionState
import com.example.data.model.RelayDeviceRole
import com.example.data.model.RelayTransport
import com.example.data.model.RelayedEnvelope
import com.example.data.model.TransferSessionState
import com.example.data.repository.GeneratedExportFile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

/**
 * Application-level bridge transfer facade backed by [BluetoothManager].
 * Preserves high-level export intents and [TransferSessionState] UI banners while delegating
 * Primary <-> Secondary connections and bidirectional relaying to [BluetoothManager].
 */
class BluetoothTransferManager(private val context: Context) {

    companion object {
        val SERVICE_UUID: UUID = BluetoothManager.SERVICE_UUID
        val BLE_BRIDGE_SERVICE_UUID: UUID = BluetoothManager.BLE_BRIDGE_SERVICE_UUID
        val BLE_BRIDGE_RX_CHAR_UUID: UUID = BluetoothManager.BLE_BRIDGE_RX_CHAR_UUID
        val BLE_BRIDGE_TX_CHAR_UUID: UUID = BluetoothManager.BLE_BRIDGE_TX_CHAR_UUID
        const val SERVICE_NAME = BluetoothManager.SERVICE_NAME
    }

    val coreManager: BluetoothManager = BluetoothManager(context)

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var incomingRelayJob: Job? = null

    val isBluetoothSupported: StateFlow<Boolean> = coreManager.isBluetoothSupported
    val isBluetoothEnabled: StateFlow<Boolean> = coreManager.isBluetoothEnabled
    val hasPermissions: StateFlow<Boolean> = coreManager.hasPermissions
    val permissionSummary: StateFlow<PermissionSummary> = coreManager.permissionSummary
    val isScanning: StateFlow<Boolean> = coreManager.isScanning
    val pairedDevices: StateFlow<List<DiscoveredBluetoothDevice>> = coreManager.pairedDevices
    val nearbyDevices: StateFlow<List<DiscoveredBluetoothDevice>> = coreManager.nearbyDevices
    val deviceRole: StateFlow<RelayDeviceRole> = coreManager.deviceRole
    val connectionState: StateFlow<RelayConnectionState> = coreManager.connectionState
    val autoReconnectEnabled: StateFlow<Boolean> = coreManager.autoReconnectEnabled
    val incomingPayloads: SharedFlow<RelayedEnvelope> = coreManager.incomingPayloads

    private val _transferState = MutableStateFlow<TransferSessionState>(TransferSessionState.Idle)
    val transferState: StateFlow<TransferSessionState> = _transferState.asStateFlow()

    init {
        scope.launch {
            coreManager.connectionState.collect { relayState ->
                when (relayState) {
                    is RelayConnectionState.Listening -> {
                        _transferState.value = TransferSessionState.Listening(relayState.localDeviceName)
                    }
                    is RelayConnectionState.Connecting -> {
                        _transferState.value = TransferSessionState.Connecting(relayState.deviceName)
                    }
                    is RelayConnectionState.Reconnecting -> {
                        _transferState.value = TransferSessionState.Reconnecting(
                            targetName = relayState.deviceName,
                            attempt = relayState.attempt,
                            maxAttempts = relayState.maxAttempts
                        )
                    }
                    is RelayConnectionState.Relaying -> {
                        _transferState.value = TransferSessionState.Transferring(
                            targetName = relayState.deviceName,
                            progress = relayState.progress,
                            bytesSent = relayState.bytesTransferred,
                            totalBytes = relayState.totalBytes
                        )
                    }
                    is RelayConnectionState.Error -> {
                        _transferState.value = TransferSessionState.Error(relayState.message)
                    }
                    is RelayConnectionState.Connected -> {
                        if (_transferState.value is TransferSessionState.Connecting ||
                            _transferState.value is TransferSessionState.Reconnecting ||
                            _transferState.value is TransferSessionState.Listening
                        ) {
                            _transferState.value = TransferSessionState.Success(
                                "اتصال رله با «${relayState.deviceName}» (${relayState.transport.labelFa}) برقرار شد."
                            )
                        }
                    }
                    RelayConnectionState.Disconnected -> {
                        if (_transferState.value is TransferSessionState.Listening ||
                            _transferState.value is TransferSessionState.Connecting ||
                            _transferState.value is TransferSessionState.Reconnecting
                        ) {
                            _transferState.value = TransferSessionState.Idle
                        }
                    }
                }
            }
        }
    }

    fun requiredPermissions(): Array<String> = coreManager.requiredPermissions()

    fun allCompliantPermissions(): Array<String> = coreManager.allCompliantPermissions()

    fun checkRequiredPermissions(): Boolean = coreManager.checkRequiredPermissions()

    fun refreshStatus() = coreManager.refreshStatus()

    fun refreshPairedDevices() = coreManager.refreshPairedDevices()

    fun setDeviceRole(role: RelayDeviceRole) = coreManager.setDeviceRole(role)

    fun setAutoReconnectEnabled(enabled: Boolean) = coreManager.setAutoReconnectEnabled(enabled)

    fun cancelAutoReconnect() = coreManager.cancelAutoReconnect()

    fun startScan() = coreManager.startScan()

    fun stopScan() = coreManager.stopScan()

    fun pairDevice(address: String) {
        coreManager.pairDevice(address)
    }

    suspend fun connectToPeer(
        device: DiscoveredBluetoothDevice,
        transport: RelayTransport = RelayTransport.AUTO
    ): Result<Unit> = coreManager.connect(device, transport)

    fun disconnectPeer() {
        coreManager.disconnect()
        _transferState.value = TransferSessionState.Idle
    }

    suspend fun relayData(
        payload: String,
        encryptWithPin: String? = null,
        targetDevice: DiscoveredBluetoothDevice? = null,
        transport: RelayTransport = RelayTransport.AUTO
    ): Result<Long> {
        val result = coreManager.relayData(
            payload = payload,
            encryptWithPin = encryptWithPin,
            targetDevice = targetDevice,
            transport = transport
        )
        if (result.isSuccess) {
            val targetName = targetDevice?.name ?: when (val st = coreManager.connectionState.value) {
                is RelayConnectionState.Connected -> st.deviceName
                else -> "دستگاه متصل"
            }
            _transferState.value = TransferSessionState.Success(
                "داده با موفقیت به «$targetName» رله شد (${result.getOrDefault(0L)} بایت)."
            )
        }
        return result
    }

    fun startListeningServer(onPackageReceived: suspend (jsonPayload: String, senderName: String) -> Unit) {
        incomingRelayJob?.cancel()
        incomingRelayJob = scope.launch {
            coreManager.incomingPayloads.collect { envelope ->
                onPackageReceived(envelope.payload, envelope.senderName)
                _transferState.value = TransferSessionState.Success(
                    "بسته داده از «${envelope.senderName}» (${envelope.transport.labelFa}) دریافت شد."
                )
            }
        }
        coreManager.startListening(coreManager.deviceRole.value)
    }

    fun stopListeningServer() {
        incomingRelayJob?.cancel()
        incomingRelayJob = null
        coreManager.disconnect()
        if (_transferState.value is TransferSessionState.Listening) {
            _transferState.value = TransferSessionState.Idle
        }
    }

    fun buildBridgeCommandPayload(
        type: BridgeCommandType,
        sourceDeviceName: String,
        contactName: String,
        phoneNumber: String,
        messageBody: String = "",
        simSlot: Int = 1
    ): String {
        return JSONObject().apply {
            put("app", "eSkyTell")
            put("bridgeType", type.name)
            put("sourceDeviceName", sourceDeviceName)
            put("senderRole", coreManager.deviceRole.value.name)
            put("contactName", contactName.ifBlank { phoneNumber })
            put("phoneNumber", phoneNumber)
            put("messageBody", messageBody)
            put("simSlot", simSlot)
            put("timestampMillis", System.currentTimeMillis())
        }.toString()
    }

    suspend fun sendOverRfcomm(
        device: DiscoveredBluetoothDevice,
        jsonPayload: String
    ): Result<Long> = relayData(
        payload = jsonPayload,
        targetDevice = device,
        transport = RelayTransport.RFCOMM
    )

    fun sendOverBleGatt(device: DiscoveredBluetoothDevice, payload: String) {
        scope.launch {
            relayData(
                payload = payload,
                targetDevice = device,
                transport = RelayTransport.BLE_GATT
            )
        }
    }

    fun buildBluetoothShareIntent(exportFile: GeneratedExportFile): Intent {
        val baseIntent = Intent(Intent.ACTION_SEND).apply {
            type = exportFile.mimeType
            putExtra(Intent.EXTRA_STREAM, exportFile.uri)
            putExtra(Intent.EXTRA_SUBJECT, "بسته ارتباطی eSkyTell")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val resolved = context.packageManager.queryIntentActivities(baseIntent, 0)
        val btHandler = resolved.firstOrNull {
            it.activityInfo.packageName.contains("bluetooth", ignoreCase = true)
        }
        return if (btHandler != null) {
            Intent(baseIntent).apply {
                setPackage(btHandler.activityInfo.packageName)
            }
        } else {
            Intent.createChooser(baseIntent, "ارسال فایل eSkyTell از طریق بلوتوث یا اشتراک‌گذاری")
        }
    }

    fun clearTransferState() {
        _transferState.value = TransferSessionState.Idle
    }

    fun release() {
        incomingRelayJob?.cancel()
        incomingRelayJob = null
        coreManager.release()
        scope.cancel()
    }
}
