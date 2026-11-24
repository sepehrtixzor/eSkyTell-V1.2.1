package com.example.ui

import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.net.Uri
import android.provider.ContactsContract
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.bluetooth.BluetoothTransferManager
import com.example.bluetooth.BridgeCrypto
import com.example.data.local.CallRecordEntity
import com.example.data.local.ContactEntity
import com.example.data.local.SmsRecordEntity
import com.example.data.local.TransferDatabase
import com.example.data.local.TransferHistoryEntity
import com.example.data.model.ActiveBridgeCall
import com.example.data.model.AnnouncementCategory
import com.example.data.model.AnnouncementModule
import com.example.data.model.AppGlobalConfig
import com.example.data.model.BridgeCommandType
import com.example.data.model.CallDirection
import com.example.data.model.DevicePlatform
import com.example.data.model.DiscoveredBluetoothDevice
import com.example.data.model.ExportFormat
import com.example.data.model.IncomingBridgeEvent
import com.example.data.model.PaymentMethodType
import com.example.data.model.PaymentRequest
import com.example.data.model.PaymentRequestStatus
import com.example.data.model.PermissionSummary
import com.example.data.model.RelayConnectionState
import com.example.data.model.RelayDeviceRole
import com.example.data.model.RelayTransport
import com.example.data.model.SmsDirection
import com.example.data.model.SmsThreadSummary
import com.example.data.model.SubscriptionTier
import com.example.data.model.TransferMode
import com.example.data.model.TransferSessionState
import com.example.data.model.UserProfile
import com.example.data.remote.CloudAdminRepository
import com.example.data.repository.TransferRepository
import com.example.notifications.SubscriptionNotificationManager
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class MainUiState(
    val currentUserId: String = "",
    val currentUserEmail: String = "",
    val isAdmin: Boolean = false,
    val isDarkTheme: Boolean = true,
    val isBluetoothSupported: Boolean = true,
    val isBluetoothEnabled: Boolean = false,
    val hasBluetoothPermissions: Boolean = false,
    val permissionSummary: PermissionSummary = PermissionSummary(),
    val isScanning: Boolean = false,
    val pairedDevices: List<DiscoveredBluetoothDevice> = emptyList(),
    val nearbyDevices: List<DiscoveredBluetoothDevice> = emptyList(),
    val selectedBridgeSourceAddress: String? = null,
    val activeSimSlot: Int = 1,
    val dialerNumber: String = "",
    val activeCall: ActiveBridgeCall? = null,
    val isQuietHoursEnabled: Boolean = false,
    val pairingPin: String = "482910",
    val encryptionEnabled: Boolean = true,
    val transferState: TransferSessionState = TransferSessionState.Idle,
    val transferMode: TransferMode = TransferMode.BLE_GATT_BRIDGE,
    val exportFormat: ExportFormat = ExportFormat.HTML_REPORT,
    val smsRecords: List<SmsRecordEntity> = emptyList(),
    val smsThreads: List<SmsThreadSummary> = emptyList(),
    val selectedSmsThreadPhone: String? = null,
    val callRecords: List<CallRecordEntity> = emptyList(),
    val contacts: List<ContactEntity> = emptyList(),
    val transferHistory: List<TransferHistoryEntity> = emptyList(),
    val selectedSmsCount: Int = 0,
    val selectedCallCount: Int = 0,
    val cachedBridgeSmsCount: Int = 0,
    val cachedBridgeContactsCount: Int = 0,
    val latestIncomingBridgeEvent: IncomingBridgeEvent? = null,
    val appConfig: AppGlobalConfig = AppGlobalConfig(),
    val userProfile: UserProfile? = null,
    val publishedAnnouncements: List<AnnouncementModule> = emptyList(),
    val adminAnnouncements: List<AnnouncementModule> = emptyList(),
    val adminUsers: List<UserProfile> = emptyList(),
    val userPaymentRequests: List<PaymentRequest> = emptyList(),
    val adminPaymentRequests: List<PaymentRequest> = emptyList(),
    val isCloudSaving: Boolean = false,
    val showPaywallSheet: Boolean = false,
    val renewalNotificationsEnabled: Boolean = true,
    val autoReconnectEnabled: Boolean = true,
    val relayDeviceRole: RelayDeviceRole = RelayDeviceRole.PRIMARY,
    val relayConnectionState: RelayConnectionState = RelayConnectionState.Disconnected,
    val isRefreshing: Boolean = false
) {
    val allKnownDevices: List<DiscoveredBluetoothDevice>
        get() = (pairedDevices + nearbyDevices).distinctBy { it.address }

    val activeBridgeDevice: DiscoveredBluetoothDevice?
        get() = allKnownDevices.firstOrNull { it.address == selectedBridgeSourceAddress }
            ?: allKnownDevices.firstOrNull { it.isConnected }
            ?: pairedDevices.firstOrNull { it.platform == DevicePlatform.IPHONE }
            ?: pairedDevices.firstOrNull()

    val sim1DisplayLabel: String
        get() = userProfile?.sim1Label?.takeIf { it.isNotBlank() } ?: "SIM 1 • خط کاری"

    val sim2DisplayLabel: String
        get() = userProfile?.sim2Label?.takeIf { it.isNotBlank() } ?: "SIM 2 • خط شخصی"

    val activeSimLabel: String
        get() = if (activeSimSlot == 2) sim2DisplayLabel else sim1DisplayLabel

    val hasProBridgeAccess: Boolean
        get() = !appConfig.subscriptionRequired || isAdmin || (userProfile?.hasActiveSubscription == true)

    val pendingPaymentRequestsCount: Int
        get() = adminPaymentRequests.count { it.status == PaymentRequestStatus.PENDING.name }

    val subscriptionDaysRemaining: Int?
        get() {
            val expires = userProfile?.subscriptionExpiresAtMillis ?: 0L
            if (expires <= 0L) return null
            val diff = expires - System.currentTimeMillis()
            if (diff <= 0L) return 0
            return kotlin.math.ceil(diff.toDouble() / 86_400_000.0).toInt().coerceAtLeast(1)
        }

    val unreadSmsTotal: Int
        get() = smsThreads.sumOf { it.unreadCount }

    val selectedThreadMessages: List<SmsRecordEntity>
        get() {
            val target = selectedSmsThreadPhone ?: return emptyList()
            return smsRecords.filter { it.phoneNumber == target }.sortedBy { it.timestampMillis }
        }
}

class MainViewModel(
    private val appContext: Context,
    private val repository: TransferRepository,
    val bluetoothManager: BluetoothTransferManager,
    private val cloudAdminRepository: CloudAdminRepository,
    private val currentUserId: String,
    private val currentUserEmail: String,
    private val isAdmin: Boolean
) : ViewModel() {

    private val audioManager = appContext.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
    private val notificationManager = SubscriptionNotificationManager(appContext)

    private val _isDarkTheme = MutableStateFlow(true)
    private val _transferMode = MutableStateFlow(TransferMode.BLE_GATT_BRIDGE)
    private val _exportFormat = MutableStateFlow(ExportFormat.HTML_REPORT)
    private val _selectedBridgeSourceAddress = MutableStateFlow<String?>(null)
    private val _activeSimSlot = MutableStateFlow(1)
    private val _dialerNumber = MutableStateFlow("")
    private val _activeCall = MutableStateFlow<ActiveBridgeCall?>(null)
    private val _isQuietHoursEnabled = MutableStateFlow(false)
    private val _pairingPin = MutableStateFlow(BridgeCrypto.generatePairingPin())
    private val _encryptionEnabled = MutableStateFlow(true)
    private val _selectedSmsThreadPhone = MutableStateFlow<String?>(null)
    private val _latestIncomingBridgeEvent = MutableStateFlow<IncomingBridgeEvent?>(null)
    private val _isCloudSaving = MutableStateFlow(false)
    private val _showPaywallSheet = MutableStateFlow(false)
    private val _renewalNotificationsEnabled = MutableStateFlow(notificationManager.isRenewalReminderEnabled())
    private val _isRefreshing = MutableStateFlow(false)

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _shareIntentEvent = MutableSharedFlow<Intent>()
    val shareIntentEvent: SharedFlow<Intent> = _shareIntentEvent.asSharedFlow()

    init {
        notificationManager.ensureNotificationChannel()
        viewModelScope.launch {
            val user = Firebase.auth.currentUser
            if (user != null && user.uid == currentUserId) {
                cloudAdminRepository.ensureUserProfile(user)
            }
        }
        viewModelScope.launch {
            bluetoothManager.incomingPayloads.collect { envelope ->
                handleIncomingRelayEnvelope(
                    rawPayload = envelope.payload,
                    senderName = envelope.senderName,
                    senderAddress = envelope.senderAddress,
                    channelLabel = "رله زنده (${envelope.transport.labelFa})"
                )
            }
        }
        viewModelScope.launch {
            combine(userProfileFlow, globalConfigFlow) { profile, config -> profile to config }
                .collect { (profile, config) ->
                    notificationManager.evaluateAndNotifyIfNeeded(profile, config)
                }
        }
    }

    private fun <T> FlowSafeCatch(flow: kotlinx.coroutines.flow.Flow<T>, fallback: T): kotlinx.coroutines.flow.Flow<T> =
        flow.catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            emit(fallback)
        }

    private val globalConfigFlow = FlowSafeCatch(
        cloudAdminRepository.observeGlobalConfig(),
        AppGlobalConfig()
    )

    private val publishedAnnouncementsFlow = FlowSafeCatch(
        cloudAdminRepository.observePublishedAnnouncements(),
        emptyList()
    )

    private val userProfileFlow = FlowSafeCatch(
        cloudAdminRepository.observeCurrentUserProfile(currentUserId),
        null
    )

    private val adminAnnouncementsFlow = if (isAdmin) {
        FlowSafeCatch(cloudAdminRepository.observeAllAnnouncementsForAdmin(), emptyList())
    } else {
        flowOf(emptyList())
    }

    private val adminUsersFlow = if (isAdmin) {
        FlowSafeCatch(cloudAdminRepository.observeAllUsersForAdmin(), emptyList())
    } else {
        flowOf(emptyList())
    }

    private val userPaymentRequestsFlow = FlowSafeCatch(
        cloudAdminRepository.observeUserPaymentRequests(currentUserId),
        emptyList()
    )

    private val adminPaymentRequestsFlow = if (isAdmin) {
        FlowSafeCatch(cloudAdminRepository.observeAllPaymentRequestsForAdmin(), emptyList())
    } else {
        flowOf(emptyList())
    }

    private val btStatusFlow = combine(
        bluetoothManager.isBluetoothSupported,
        bluetoothManager.isBluetoothEnabled,
        bluetoothManager.hasPermissions,
        bluetoothManager.isScanning,
        bluetoothManager.permissionSummary
    ) { supported, enabled, perms, scanning, permSummary ->
        BtStatusTuple(supported, enabled, perms, scanning, permSummary)
    }

    private val btDevicesFlow = combine(
        bluetoothManager.pairedDevices,
        bluetoothManager.nearbyDevices,
        bluetoothManager.transferState,
        _selectedBridgeSourceAddress,
        _latestIncomingBridgeEvent,
        bluetoothManager.deviceRole,
        bluetoothManager.connectionState,
        bluetoothManager.autoReconnectEnabled
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        BtDevicesTuple(
            paired = args[0] as List<DiscoveredBluetoothDevice>,
            nearby = args[1] as List<DiscoveredBluetoothDevice>,
            state = args[2] as TransferSessionState,
            sourceAddr = args[3] as String?,
            incoming = args[4] as IncomingBridgeEvent?,
            relayRole = args[5] as RelayDeviceRole,
            relayState = args[6] as RelayConnectionState,
            autoReconnectEnabled = args[7] as Boolean
        )
    }

    private val localDataFlow = combine(
        repository.smsRecords,
        repository.callRecords,
        repository.contacts,
        repository.transferHistory,
        _selectedSmsThreadPhone,
        repository.cachedBridgeSmsCount,
        repository.cachedBridgeContactsCount
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        LocalDataTuple(
            sms = args[0] as List<SmsRecordEntity>,
            calls = args[1] as List<CallRecordEntity>,
            contacts = args[2] as List<ContactEntity>,
            history = args[3] as List<TransferHistoryEntity>,
            selectedThreadPhone = args[4] as String?,
            cachedBridgeSmsCount = args[5] as Int,
            cachedBridgeContactsCount = args[6] as Int
        )
    }

    private val dialerAndCallFlow = combine(
        _isDarkTheme,
        _activeSimSlot,
        _dialerNumber,
        _activeCall,
        _isQuietHoursEnabled
    ) { dark, simSlot, dialer, call, quiet ->
        DialerTuple(dark, simSlot, dialer, call, quiet)
    }

    private val bridgePrefsFlow = combine(
        _transferMode,
        _exportFormat,
        _pairingPin,
        _encryptionEnabled,
        _showPaywallSheet
    ) { mode, format, pin, enc, paywall ->
        BridgePrefsTuple(mode, format, pin, enc, paywall)
    }

    private val cloudTupleFlow = combine(
        globalConfigFlow,
        userProfileFlow,
        publishedAnnouncementsFlow,
        adminAnnouncementsFlow,
        adminUsersFlow,
        userPaymentRequestsFlow,
        adminPaymentRequestsFlow,
        _isCloudSaving
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        CloudTuple(
            config = args[0] as AppGlobalConfig,
            profile = args[1] as UserProfile?,
            published = args[2] as List<AnnouncementModule>,
            adminModules = args[3] as List<AnnouncementModule>,
            users = args[4] as List<UserProfile>,
            userPayments = args[5] as List<PaymentRequest>,
            adminPayments = args[6] as List<PaymentRequest>,
            saving = args[7] as Boolean
        )
    }

    val uiState: StateFlow<MainUiState> = combine(
        btStatusFlow,
        btDevicesFlow,
        localDataFlow,
        dialerAndCallFlow,
        bridgePrefsFlow,
        cloudTupleFlow,
        _isRefreshing
    ) { args ->
        val bt = args[0] as BtStatusTuple
        val devices = args[1] as BtDevicesTuple
        val local = args[2] as LocalDataTuple
        val dialer = args[3] as DialerTuple
        val prefs = args[4] as BridgePrefsTuple
        val cloud = args[5] as CloudTuple
        val refreshing = args[6] as Boolean

        val contactNamesByPhone = local.contacts.associate {
            normalizePhoneKey(it.phoneNumber) to it.displayName
        }

        val threads = local.sms
            .groupBy { it.phoneNumber }
            .map { (phone, items) ->
                val sorted = items.sortedByDescending { it.timestampMillis }
                val latest = sorted.first()
                val resolvedName = contactNamesByPhone[normalizePhoneKey(phone)]
                    ?: latest.senderName.takeIf { it.isNotBlank() }
                    ?: phone
                SmsThreadSummary(
                    phoneNumber = phone,
                    displayName = resolvedName,
                    lastMessage = latest.body,
                    lastTimestampMillis = latest.timestampMillis,
                    unreadCount = items.count { !it.isRead },
                    messageCount = items.size,
                    simSlot = latest.simSlot
                )
            }
            .sortedByDescending { it.lastTimestampMillis }

        MainUiState(
            currentUserId = currentUserId,
            currentUserEmail = currentUserEmail,
            isAdmin = isAdmin,
            isDarkTheme = dialer.dark,
            isBluetoothSupported = bt.supported,
            isBluetoothEnabled = bt.enabled,
            hasBluetoothPermissions = bt.perms,
            permissionSummary = bt.permSummary,
            isScanning = bt.scanning,
            pairedDevices = devices.paired,
            nearbyDevices = devices.nearby,
            selectedBridgeSourceAddress = devices.sourceAddr,
            activeSimSlot = dialer.simSlot,
            dialerNumber = dialer.dialerNumber,
            activeCall = dialer.activeCall,
            isQuietHoursEnabled = dialer.quietHours,
            pairingPin = prefs.pin,
            encryptionEnabled = prefs.encryption,
            transferState = devices.state,
            transferMode = prefs.mode,
            exportFormat = prefs.format,
            smsRecords = local.sms,
            smsThreads = threads,
            selectedSmsThreadPhone = local.selectedThreadPhone,
            callRecords = local.calls,
            contacts = local.contacts,
            transferHistory = local.history,
            selectedSmsCount = local.sms.count { it.isSelected },
            selectedCallCount = local.calls.count { it.isSelected },
            cachedBridgeSmsCount = local.cachedBridgeSmsCount,
            cachedBridgeContactsCount = local.cachedBridgeContactsCount,
            latestIncomingBridgeEvent = devices.incoming,
            appConfig = cloud.config,
            userProfile = cloud.profile,
            publishedAnnouncements = cloud.published,
            adminAnnouncements = cloud.adminModules,
            adminUsers = cloud.users,
            userPaymentRequests = cloud.userPayments,
            adminPaymentRequests = cloud.adminPayments,
            isCloudSaving = cloud.saving,
            showPaywallSheet = prefs.showPaywall,
            renewalNotificationsEnabled = _renewalNotificationsEnabled.value,
            autoReconnectEnabled = devices.autoReconnectEnabled,
            relayDeviceRole = devices.relayRole,
            relayConnectionState = devices.relayState,
            isRefreshing = refreshing
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainUiState(
            currentUserId = currentUserId,
            currentUserEmail = currentUserEmail,
            isAdmin = isAdmin
        )
    )

    private fun normalizePhoneKey(phone: String): String {
        val digits = phone.filter { it.isDigit() }
        return if (digits.length > 9) digits.takeLast(9) else digits
    }

    // --- Theme, Quiet Hours, SIM & Security ---
    fun toggleTheme() {
        _isDarkTheme.value = !_isDarkTheme.value
    }

    fun selectSimSlot(slotIndex: Int) {
        _activeSimSlot.value = slotIndex.coerceIn(1, 2)
    }

    fun toggleQuietHours(enabled: Boolean) {
        _isQuietHoursEnabled.value = enabled
        _snackbarMessage.value = if (enabled) {
            "حالت سکوت پل (Quiet Hours) فعال شد."
        } else {
            "حالت سکوت پل غیرفعال شد."
        }
    }

    fun regeneratePairingPin() {
        _pairingPin.value = BridgeCrypto.generatePairingPin()
        _snackbarMessage.value = "کد امنیتی رمزنگاری بلوتوث به‌روزرسانی شد."
    }

    fun setEncryptionEnabled(enabled: Boolean) {
        _encryptionEnabled.value = enabled
    }

    // --- Subscription Gate ---
    fun openPaywallSheet() {
        _showPaywallSheet.value = true
    }

    fun dismissPaywallSheet() {
        _showPaywallSheet.value = false
    }

    private fun requireProBridgeAccess(): Boolean {
        return if (uiState.value.hasProBridgeAccess) {
            true
        } else {
            _showPaywallSheet.value = true
            false
        }
    }

    // --- Phone Dialer & Active Bridge Call ---
    fun appendDialerDigit(digit: String) {
        if (_dialerNumber.value.length < 24) {
            _dialerNumber.value += digit
        }
    }

    fun backspaceDialer() {
        if (_dialerNumber.value.isNotEmpty()) {
            _dialerNumber.value = _dialerNumber.value.dropLast(1)
        }
    }

    fun clearDialer() {
        _dialerNumber.value = ""
    }

    fun setDialerNumber(number: String) {
        _dialerNumber.value = number
    }

    fun placeSystemDialerCall(phoneNumber: String) {
        val clean = phoneNumber.trim()
        if (clean.isEmpty()) {
            _snackbarMessage.value = "ابتدا شماره تلفن را وارد کنید."
            return
        }
        viewModelScope.launch {
            val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${Uri.encode(clean)}"))
            _shareIntentEvent.emit(intent)
        }
    }

    fun startBridgeCall(phoneNumber: String, contactName: String = "") {
        val clean = phoneNumber.trim()
        if (clean.isEmpty()) {
            _snackbarMessage.value = "شماره تماس را وارد کنید."
            return
        }
        if (!requireProBridgeAccess()) return

        val state = uiState.value
        val matchedContact = state.contacts.firstOrNull {
            normalizePhoneKey(it.phoneNumber) == normalizePhoneKey(clean) && normalizePhoneKey(clean).isNotEmpty()
        }
        val resolvedName = contactName.ifBlank { matchedContact?.displayName ?: clean }
        val targetDevice = state.activeBridgeDevice
        val simSlot = state.activeSimSlot

        _activeCall.value = ActiveBridgeCall(
            contactName = resolvedName,
            phoneNumber = clean,
            simSlot = simSlot,
            targetDeviceName = targetDevice?.name ?: "شماره‌گیر گوشی / پل eSkyTell"
        )

        viewModelScope.launch {
            repository.addManualCall(
                contactName = resolvedName,
                phoneNumber = clean,
                durationSeconds = 0,
                direction = CallDirection.OUTGOING,
                simSlot = simSlot,
                sourceLabel = state.activeSimLabel
            )

            val connectedTarget = state.relayConnectionState as? RelayConnectionState.Connected
            if (targetDevice != null || connectedTarget != null) {
                val rawPayload = bluetoothManager.buildBridgeCommandPayload(
                    type = BridgeCommandType.CALL_TRANSFER,
                    sourceDeviceName = state.activeSimLabel,
                    contactName = resolvedName,
                    phoneNumber = clean,
                    simSlot = simSlot
                )
                val wirePayload = prepareWirePayload(rawPayload)
                val preferredTransport = when (state.transferMode) {
                    TransferMode.DIRECT_RFCOMM -> RelayTransport.RFCOMM
                    TransferMode.BLE_GATT_BRIDGE -> RelayTransport.BLE_GATT
                    TransferMode.CROSS_PLATFORM_OPP -> RelayTransport.AUTO
                }
                bluetoothManager.relayData(
                    payload = wirePayload,
                    targetDevice = targetDevice,
                    transport = preferredTransport
                )
                repository.recordTransferHistory(
                    TransferHistoryEntity(
                        targetDeviceName = targetDevice?.name ?: connectedTarget?.deviceName ?: "دستگاه متصل",
                        targetAddress = targetDevice?.address ?: connectedTarget?.address ?: "RELAY",
                        targetPlatform = targetDevice?.platform?.labelFa ?: "دستگاه بلوتوثی",
                        channelMethod = "تماس پل بلوتوث (${state.activeSimLabel})",
                        exportFormat = if (_encryptionEnabled.value) "AES-GCM" else "BRIDGE",
                        smsCount = 0,
                        callCount = 1,
                        bytesTransferred = wirePayload.toByteArray().size.toLong(),
                        status = "ارسال فرمان تماس"
                    )
                )
            }
        }
    }

    fun toggleActiveCallMute() {
        val current = _activeCall.value ?: return
        val nextMuted = !current.isMuted
        runCatching { audioManager?.isMicrophoneMute = nextMuted }
        _activeCall.value = current.copy(isMuted = nextMuted)
    }

    fun toggleActiveCallSpeaker() {
        val current = _activeCall.value ?: return
        val nextSpeaker = !current.isSpeaker
        runCatching { audioManager?.isSpeakerphoneOn = nextSpeaker }
        _activeCall.value = current.copy(isSpeaker = nextSpeaker)
    }

    fun toggleActiveCallBluetoothAudio() {
        val current = _activeCall.value ?: return
        _activeCall.value = current.copy(isBluetoothAudio = !current.isBluetoothAudio)
    }

    fun endActiveBridgeCall() {
        runCatching {
            audioManager?.isMicrophoneMute = false
            audioManager?.isSpeakerphoneOn = false
        }
        _activeCall.value = null
    }

    // --- Messages & Threads ---
    fun openSmsThread(phoneNumber: String) {
        _selectedSmsThreadPhone.value = phoneNumber
        viewModelScope.launch {
            repository.markThreadRead(phoneNumber)
        }
    }

    fun closeSmsThread() {
        _selectedSmsThreadPhone.value = null
    }

    fun deleteSmsThread(phoneNumber: String) {
        viewModelScope.launch {
            repository.deleteSmsThread(phoneNumber)
            if (_selectedSmsThreadPhone.value == phoneNumber) {
                _selectedSmsThreadPhone.value = null
            }
            _snackbarMessage.value = "گفتگو حذف شد."
        }
    }

    fun sendBridgeSms(
        phoneNumber: String,
        contactName: String,
        body: String,
        alsoOpenSystemSmsIfNoDevice: Boolean = true
    ) {
        val cleanPhone = phoneNumber.trim()
        val cleanBody = body.trim()
        if (cleanPhone.isEmpty() || cleanBody.isEmpty()) {
            _snackbarMessage.value = "شماره و متن پیامک را وارد کنید."
            return
        }
        if (!requireProBridgeAccess()) return

        val state = uiState.value
        val matchedContact = state.contacts.firstOrNull {
            normalizePhoneKey(it.phoneNumber) == normalizePhoneKey(cleanPhone) && normalizePhoneKey(cleanPhone).isNotEmpty()
        }
        val resolvedName = contactName.ifBlank { matchedContact?.displayName ?: cleanPhone }
        val simSlot = state.activeSimSlot
        val targetDevice = state.activeBridgeDevice

        viewModelScope.launch {
            repository.addManualSms(
                senderName = resolvedName,
                phoneNumber = cleanPhone,
                body = cleanBody,
                direction = SmsDirection.OUTGOING,
                simSlot = simSlot,
                sourceLabel = state.activeSimLabel
            )

            val connectedTarget = state.relayConnectionState as? RelayConnectionState.Connected
            if (targetDevice != null || connectedTarget != null) {
                val rawPayload = bluetoothManager.buildBridgeCommandPayload(
                    type = BridgeCommandType.SMS_RELAY,
                    sourceDeviceName = state.activeSimLabel,
                    contactName = resolvedName,
                    phoneNumber = cleanPhone,
                    messageBody = cleanBody,
                    simSlot = simSlot
                )
                val wirePayload = prepareWirePayload(rawPayload)
                val preferredTransport = when (state.transferMode) {
                    TransferMode.DIRECT_RFCOMM -> RelayTransport.RFCOMM
                    TransferMode.BLE_GATT_BRIDGE -> RelayTransport.BLE_GATT
                    TransferMode.CROSS_PLATFORM_OPP -> RelayTransport.AUTO
                }
                bluetoothManager.relayData(
                    payload = wirePayload,
                    targetDevice = targetDevice,
                    transport = preferredTransport
                )
                val peerName = targetDevice?.name ?: connectedTarget?.deviceName ?: "دستگاه متصل"
                val peerAddr = targetDevice?.address ?: connectedTarget?.address ?: "RELAY"
                val peerPlatform = targetDevice?.platform?.labelFa ?: "دستگاه بلوتوثی"
                repository.recordTransferHistory(
                    TransferHistoryEntity(
                        targetDeviceName = peerName,
                        targetAddress = peerAddr,
                        targetPlatform = peerPlatform,
                        channelMethod = "رله پیامک (${state.activeSimLabel})",
                        exportFormat = if (_encryptionEnabled.value) "AES-GCM" else "BRIDGE",
                        smsCount = 1,
                        callCount = 0,
                        bytesTransferred = wirePayload.toByteArray().size.toLong(),
                        status = "ارسال بلوتوثی"
                    )
                )
                _snackbarMessage.value = "پیامک از طریق پل بلوتوث به «$peerName» ارسال شد."
            } else if (alsoOpenSystemSmsIfNoDevice) {
                openSystemSmsComposer(cleanPhone, cleanBody)
            }
        }
    }

    fun openSystemSmsComposer(phoneNumber: String, body: String = "") {
        val clean = phoneNumber.trim()
        if (clean.isEmpty()) {
            _snackbarMessage.value = "شماره گیرنده را وارد کنید."
            return
        }
        viewModelScope.launch {
            val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:${Uri.encode(clean)}")).apply {
                if (body.isNotBlank()) putExtra("sms_body", body)
            }
            _shareIntentEvent.emit(intent)
        }
    }

    // --- Manual Records, Contacts & File Import ---
    fun addManualSms(
        sender: String,
        phone: String,
        body: String,
        direction: SmsDirection
    ) {
        viewModelScope.launch {
            repository.addManualSms(
                senderName = sender,
                phoneNumber = phone,
                body = body,
                direction = direction,
                simSlot = _activeSimSlot.value,
                sourceLabel = uiState.value.activeSimLabel
            )
            _snackbarMessage.value = "پیامک در صندوق ثبت شد."
        }
    }

    fun addManualCall(
        contact: String,
        phone: String,
        durationSeconds: Int,
        direction: CallDirection
    ) {
        viewModelScope.launch {
            repository.addManualCall(
                contactName = contact,
                phoneNumber = phone,
                durationSeconds = durationSeconds,
                direction = direction,
                simSlot = _activeSimSlot.value,
                sourceLabel = uiState.value.activeSimLabel
            )
            _snackbarMessage.value = "سابقه تماس ثبت شد."
        }
    }

    fun saveContact(
        displayName: String,
        phoneNumber: String,
        email: String = "",
        notes: String = "",
        isFavorite: Boolean = false,
        existingId: Long? = null
    ) {
        viewModelScope.launch {
            repository.upsertContact(
                displayName = displayName,
                phoneNumber = phoneNumber,
                email = email,
                notes = notes,
                isFavorite = isFavorite,
                existingId = existingId
            )
            _snackbarMessage.value = "مخاطب «${displayName.ifBlank { phoneNumber }}» ذخیره شد."
        }
    }

    fun toggleContactFavorite(id: Long, favorite: Boolean) {
        viewModelScope.launch { repository.setContactFavorite(id, favorite) }
    }

    fun deleteContact(id: Long) {
        viewModelScope.launch { repository.deleteContact(id) }
    }

    fun importContactsFromPhone() {
        viewModelScope.launch {
            if (!bluetoothManager.permissionSummary.value.contactsGranted) {
                _snackbarMessage.value = "ابتدا مجوز دسترسی به مخاطبین گوشی را تأیید کنید."
                return@launch
            }
            val added = repository.importDeviceContacts()
            _snackbarMessage.value = if (added > 0) {
                "$added مخاطب جدید از دفترچه گوشی همگام‌سازی شد."
            } else {
                "همه مخاطبین گوشی از قبل در eSkyTell موجود هستند."
            }
        }
    }

    fun exportContactToPhone(contact: ContactEntity) {
        viewModelScope.launch {
            val insertIntent = Intent(ContactsContract.Intents.Insert.ACTION).apply {
                type = ContactsContract.RawContacts.CONTENT_TYPE
                putExtra(ContactsContract.Intents.Insert.NAME, contact.displayName)
                putExtra(ContactsContract.Intents.Insert.PHONE, contact.phoneNumber)
                if (contact.email.isNotBlank()) {
                    putExtra(ContactsContract.Intents.Insert.EMAIL, contact.email)
                }
                if (contact.notes.isNotBlank()) {
                    putExtra(ContactsContract.Intents.Insert.NOTES, contact.notes)
                }
            }
            repository.markContactSynced(contact.id)
            _shareIntentEvent.emit(insertIntent)
        }
    }

    fun shareContactsViaBluetooth(contactsToShare: List<ContactEntity>) {
        if (contactsToShare.isEmpty()) {
            _snackbarMessage.value = "مخاطبی برای اشتراک وجود ندارد."
            return
        }
        viewModelScope.launch {
            val vcf = repository.createContactsVCardFile(contactsToShare)
            val intent = bluetoothManager.buildBluetoothShareIntent(vcf)
            repository.recordTransferHistory(
                TransferHistoryEntity(
                    targetDeviceName = "آیفون / دستگاه بلوتوثی",
                    targetAddress = "VCARD-OPP",
                    targetPlatform = DevicePlatform.IPHONE.labelFa,
                    channelMethod = "ارسال کارت مخاطب (vCard)",
                    exportFormat = "VCF",
                    smsCount = 0,
                    callCount = contactsToShare.size,
                    bytesTransferred = vcf.byteSize,
                    status = "آماده ارسال"
                )
            )
            _shareIntentEvent.emit(intent)
        }
    }

    fun importBackupUri(uri: Uri) {
        viewModelScope.launch {
            val result = repository.importFromUri(uri)
            _snackbarMessage.value = if (result.smsAdded + result.callsAdded + result.contactsAdded > 0) {
                "از «${result.sourceName}»: ${result.smsAdded} پیامک، ${result.callsAdded} تماس و ${result.contactsAdded} مخاطب وارد شد."
            } else {
                "اطلاعات معتبری در فایل «${result.sourceName}» یافت نشد."
            }
        }
    }

    fun importPastedOrSharedText(text: String, sourceLabel: String = "کلیپ‌بورد") {
        viewModelScope.launch {
            val result = repository.importSharedOrPastedText(text, sourceLabel)
            _snackbarMessage.value = if (result.smsAdded + result.callsAdded > 0) {
                "${result.smsAdded} پیامک و ${result.callsAdded} تماس اضافه شد."
            } else {
                "متن معتبری برای افزودن یافت نشد."
            }
        }
    }

    fun deleteCall(id: Long) {
        viewModelScope.launch { repository.deleteCall(id) }
    }

    fun deleteHistory(id: Long) {
        viewModelScope.launch { repository.deleteHistory(id) }
    }

    fun clearHistory() {
        viewModelScope.launch {
            repository.clearHistory()
            _snackbarMessage.value = "تاریخچه انتقال‌ها پاک شد."
        }
    }

    // --- Bluetooth Scanning, Bridge Server & Transfers ---
    fun refreshBluetoothStatus() {
        bluetoothManager.refreshStatus()
    }

    fun syncFromPairedDevice() {
        viewModelScope.launch {
            _isRefreshing.value = true
            bluetoothManager.refreshStatus()
            repository.importDeviceContacts()
            val activeDevice = uiState.value.activeBridgeDevice
            if (activeDevice != null) {
                bluetoothManager.pairDevice(activeDevice.address)
                _snackbarMessage.value = "همگام‌سازی از «${activeDevice.name}» انجام شد."
            } else {
                _snackbarMessage.value = "وضعیت پل بلوتوث و مخاطبین محلی به‌روز شد."
            }
            kotlinx.coroutines.delay(800L)
            _isRefreshing.value = false
        }
    }

    fun toggleScan() {
        if (bluetoothManager.isScanning.value) {
            bluetoothManager.stopScan()
        } else {
            bluetoothManager.startScan()
        }
    }

    fun selectBridgeSource(address: String) {
        _selectedBridgeSourceAddress.value = address
        _snackbarMessage.value = "دستگاه فعال پل بلوتوث انتخاب شد."
    }

    fun pairDevice(device: DiscoveredBluetoothDevice) {
        bluetoothManager.pairDevice(device.address)
        _snackbarMessage.value = "درخواست جفت‌سازی برای «${device.name}» ارسال شد."
    }

    fun setTransferMode(mode: TransferMode) {
        if (mode == TransferMode.BLE_GATT_BRIDGE && !uiState.value.appConfig.enableBleGattBridge) {
            _snackbarMessage.value = "حالت BLE GATT موقتاً توسط مدیریت غیرفعال شده است."
            return
        }
        _transferMode.value = mode
    }

    fun setExportFormat(format: ExportFormat) {
        _exportFormat.value = format
    }

    fun setRelayDeviceRole(role: RelayDeviceRole) {
        bluetoothManager.setDeviceRole(role)
        _snackbarMessage.value = "نقش دستگاه روی «${role.labelFa}» تنظیم شد."
    }

    fun connectToDevice(device: DiscoveredBluetoothDevice) {
        if (!requireProBridgeAccess()) return
        _selectedBridgeSourceAddress.value = device.address
        val transport = when (uiState.value.transferMode) {
            TransferMode.DIRECT_RFCOMM -> RelayTransport.RFCOMM
            TransferMode.BLE_GATT_BRIDGE -> RelayTransport.BLE_GATT
            TransferMode.CROSS_PLATFORM_OPP -> RelayTransport.AUTO
        }
        viewModelScope.launch {
            val res = bluetoothManager.connectToPeer(device, transport)
            if (res.isSuccess) {
                _snackbarMessage.value = "اتصال رله با «${device.name}» برقرار شد."
            }
        }
    }

    fun disconnectBridgePeer() {
        bluetoothManager.disconnectPeer()
        _snackbarMessage.value = "اتصال پل بلوتوث قطع شد."
    }

    fun setAutoReconnectEnabled(enabled: Boolean) {
        bluetoothManager.setAutoReconnectEnabled(enabled)
        _snackbarMessage.value = if (enabled) {
            "اتصال مجدد خودکار بلوتوث در صورت قطع سیگنال فعال شد."
        } else {
            "اتصال مجدد خودکار بلوتوث غیرفعال شد."
        }
    }

    fun cancelAutoReconnect() {
        bluetoothManager.cancelAutoReconnect()
        _snackbarMessage.value = "تلاش برای اتصال مجدد متوقف شد."
    }

    fun toggleReceiveServer() {
        if (bluetoothManager.transferState.value is TransferSessionState.Listening ||
            bluetoothManager.connectionState.value is RelayConnectionState.Listening
        ) {
            bluetoothManager.stopListeningServer()
            return
        }
        if (!requireProBridgeAccess()) return

        bluetoothManager.coreManager.startListening(bluetoothManager.deviceRole.value)
    }

    private suspend fun handleIncomingRelayEnvelope(
        rawPayload: String,
        senderName: String,
        senderAddress: String,
        channelLabel: String
    ) {
        val decrypted = BridgeCrypto.decryptEnvelopeIfNeeded(rawPayload, _pairingPin.value)
        val result = repository.importBridgePayload(decrypted, senderName)
        val event = result.incomingEvent
        if (event != null && !_isQuietHoursEnabled.value) {
            _latestIncomingBridgeEvent.value = event
        }
        repository.recordTransferHistory(
            TransferHistoryEntity(
                targetDeviceName = senderName,
                targetAddress = senderAddress.ifBlank { "BRIDGE-IN" },
                targetPlatform = DevicePlatform.IPHONE.labelFa,
                channelMethod = channelLabel,
                exportFormat = "JSON",
                smsCount = result.importResult.smsAdded,
                callCount = result.importResult.callsAdded,
                bytesTransferred = rawPayload.toByteArray().size.toLong(),
                status = "دریافت موفق"
            )
        )
    }

    fun dismissIncomingBridgeEvent() {
        _latestIncomingBridgeEvent.value = null
    }

    private fun prepareWirePayload(rawJson: String): String {
        return if (_encryptionEnabled.value) {
            BridgeCrypto.encryptEnvelope(rawJson, _pairingPin.value)
        } else {
            rawJson
        }
    }

    fun sendToDevice(device: DiscoveredBluetoothDevice) {
        if (!requireProBridgeAccess()) return
        val state = uiState.value
        val smsBatch = state.smsRecords.ifEmpty { emptyList() }
        val callBatch = state.callRecords.ifEmpty { emptyList() }
        val contactsBatch = state.contacts.ifEmpty { emptyList() }
        if (smsBatch.isEmpty() && callBatch.isEmpty() && contactsBatch.isEmpty()) {
            _snackbarMessage.value = "هنوز پیامک، تماس یا مخاطبی برای همگام‌سازی ثبت نشده است."
            return
        }

        when (state.transferMode) {
            TransferMode.DIRECT_RFCOMM -> {
                viewModelScope.launch {
                    val rawPayload = repository.buildRfcommJsonPayload(smsBatch, callBatch, contactsBatch)
                    val wirePayload = prepareWirePayload(rawPayload)
                    val result = bluetoothManager.sendOverRfcomm(device, wirePayload)
                    repository.recordTransferHistory(
                        TransferHistoryEntity(
                            targetDeviceName = device.name,
                            targetAddress = device.address,
                            targetPlatform = device.platform.labelFa,
                            channelMethod = "اتصال مستقیم RFCOMM",
                            exportFormat = if (_encryptionEnabled.value) "AES-GCM" else "JSON",
                            smsCount = smsBatch.size,
                            callCount = callBatch.size,
                            bytesTransferred = result.getOrDefault(0L),
                            status = if (result.isSuccess) "موفق" else "ناموفق"
                        )
                    )
                }
            }
            TransferMode.BLE_GATT_BRIDGE -> {
                viewModelScope.launch {
                    val rawPayload = repository.buildRfcommJsonPayload(smsBatch, callBatch, contactsBatch)
                    val wirePayload = prepareWirePayload(rawPayload)
                    val result = bluetoothManager.relayData(
                        payload = wirePayload,
                        targetDevice = device,
                        transport = RelayTransport.BLE_GATT
                    )
                    repository.recordTransferHistory(
                        TransferHistoryEntity(
                            targetDeviceName = device.name,
                            targetAddress = device.address,
                            targetPlatform = device.platform.labelFa,
                            channelMethod = "پل زنده BLE GATT",
                            exportFormat = if (_encryptionEnabled.value) "AES-GCM" else "JSON",
                            smsCount = smsBatch.size,
                            callCount = callBatch.size,
                            bytesTransferred = result.getOrDefault(0L),
                            status = if (result.isSuccess) "موفق (BLE)" else "ناموفق"
                        )
                    )
                }
            }
            TransferMode.CROSS_PLATFORM_OPP -> {
                sendViaStandardBluetoothExport(
                    targetName = device.name,
                    targetAddress = device.address,
                    targetPlatform = device.platform
                )
            }
        }
    }

    fun sendViaStandardBluetoothExport(
        targetName: String = "آیفون / دستگاه بلوتوثی",
        targetAddress: String = "OPP/SHARE",
        targetPlatform: DevicePlatform = DevicePlatform.IPHONE
    ) {
        val state = uiState.value
        val smsBatch = state.smsRecords
        val callBatch = state.callRecords
        if (smsBatch.isEmpty() && callBatch.isEmpty()) {
            _snackbarMessage.value = "هنوز پیامک یا تماسی برای خروجی گرفتن ثبت نشده است."
            return
        }
        viewModelScope.launch {
            val exportFile = repository.createExportFile(
                format = state.exportFormat,
                smsList = smsBatch,
                callList = callBatch
            )
            val shareIntent = bluetoothManager.buildBluetoothShareIntent(exportFile)
            repository.recordTransferHistory(
                TransferHistoryEntity(
                    targetDeviceName = targetName,
                    targetAddress = targetAddress,
                    targetPlatform = targetPlatform.labelFa,
                    channelMethod = "ارسال فایل استاندارد (بلوتوث / اشتراک)",
                    exportFormat = state.exportFormat.extension.uppercase(),
                    smsCount = smsBatch.size,
                    callCount = callBatch.size,
                    bytesTransferred = exportFile.byteSize,
                    status = "آماده ارسال"
                )
            )
            _shareIntentEvent.emit(shareIntent)
        }
    }

    fun clearTransferBanner() {
        bluetoothManager.clearTransferState()
    }

    // --- Cloud Account & Admin Studio ---
    fun updateUserSimLabels(sim1Label: String, sim2Label: String) {
        viewModelScope.launch {
            _isCloudSaving.value = true
            val res = cloudAdminRepository.updateSimLabels(sim1Label, sim2Label)
            _isCloudSaving.value = false
            _snackbarMessage.value = if (res.isSuccess) {
                "نام سیم‌کارت‌ها ذخیره و همگام‌سازی شد."
            } else {
                "خطا در ذخیره نام سیم‌کارت‌ها."
            }
        }
    }

    fun saveAdminGlobalConfig(config: AppGlobalConfig) {
        if (!isAdmin) return
        viewModelScope.launch {
            _isCloudSaving.value = true
            val res = cloudAdminRepository.saveGlobalConfig(config)
            _isCloudSaving.value = false
            _snackbarMessage.value = if (res.isSuccess) {
                "تنظیمات کل سیستم با موفقیت منتشر شد."
            } else {
                "خطا در انتشار تنظیمات: ${res.exceptionOrNull()?.localizedMessage ?: "دسترسی محدود"}"
            }
        }
    }

    fun saveAdminAnnouncement(
        existingId: String?,
        title: String,
        summary: String,
        category: AnnouncementCategory,
        actionLabel: String,
        actionValue: String,
        published: Boolean
    ) {
        if (!isAdmin) return
        viewModelScope.launch {
            _isCloudSaving.value = true
            val res = cloudAdminRepository.saveAnnouncement(
                existingId = existingId,
                title = title,
                summary = summary,
                category = category,
                actionLabel = actionLabel,
                actionValue = actionValue,
                published = published
            )
            _isCloudSaving.value = false
            _snackbarMessage.value = if (res.isSuccess) {
                if (existingId == null) "امکان/اطلاعیه جدید منتشر شد." else "ویرایش ذخیره شد."
            } else {
                "خطا در ذخیره اطلاعیه: ${res.exceptionOrNull()?.localizedMessage ?: ""}"
            }
        }
    }

    fun toggleAdminAnnouncementPublished(id: String, published: Boolean) {
        if (!isAdmin) return
        viewModelScope.launch {
            val res = cloudAdminRepository.toggleAnnouncementPublished(id, published)
            _snackbarMessage.value = if (res.isSuccess) {
                if (published) "منتشر شد." else "از دید کاربران پنهان شد."
            } else {
                "خطا در تغییر وضعیت انتشار."
            }
        }
    }

    fun deleteAdminAnnouncement(id: String) {
        if (!isAdmin) return
        viewModelScope.launch {
            val res = cloudAdminRepository.deleteAnnouncement(id)
            _snackbarMessage.value = if (res.isSuccess) "ماژول حذف شد." else "خطا در حذف."
        }
    }

    fun setAdminUserSubscription(targetUserId: String, hasActive: Boolean, tier: SubscriptionTier) {
        if (!isAdmin) return
        viewModelScope.launch {
            _isCloudSaving.value = true
            val res = cloudAdminRepository.setUserSubscription(targetUserId, hasActive, tier)
            _isCloudSaving.value = false
            _snackbarMessage.value = if (res.isSuccess) {
                "وضعیت اشتراک کاربر به‌روزرسانی شد."
            } else {
                "خطا در تغییر اشتراک کاربر."
            }
        }
    }

    fun submitPaymentRequest(
        requestedTier: SubscriptionTier,
        amountToman: Long,
        paymentMethod: PaymentMethodType,
        targetAccountLabel: String,
        trackingCode: String,
        receiptImageBase64: String,
        userNote: String,
        onSubmitted: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isCloudSaving.value = true
            val res = cloudAdminRepository.submitPaymentRequest(
                requestedTier = requestedTier,
                amountToman = amountToman,
                paymentMethod = paymentMethod,
                targetAccountLabel = targetAccountLabel,
                trackingCode = trackingCode,
                receiptImageBase64 = receiptImageBase64,
                userNote = userNote
            )
            _isCloudSaving.value = false
            if (res.isSuccess) {
                _snackbarMessage.value = "درخواست پرداخت شما با موفقیت ثبت شد و پس از تایید مدیریت فعال می‌شود."
                onSubmitted()
            } else {
                _snackbarMessage.value = res.exceptionOrNull()?.localizedMessage
                    ?: "خطا در ثبت درخواست پرداخت."
            }
        }
    }

    fun reviewPaymentRequest(
        request: PaymentRequest,
        approve: Boolean,
        adminNote: String = ""
    ) {
        if (!isAdmin) return
        viewModelScope.launch {
            _isCloudSaving.value = true
            val res = cloudAdminRepository.reviewPaymentRequest(
                request = request,
                approve = approve,
                adminNote = adminNote
            )
            _isCloudSaving.value = false
            _snackbarMessage.value = if (res.isSuccess) {
                if (approve) "پرداخت تایید شد و اشتراک کاربر به طور خودکار ارتقا یافت."
                else "درخواست پرداخت رد شد."
            } else {
                "خطا در بررسی درخواست: ${res.exceptionOrNull()?.localizedMessage ?: ""}"
            }
        }
    }

    fun hasNotificationPermission(): Boolean = notificationManager.hasNotificationPermission()

    fun setRenewalNotificationsEnabled(enabled: Boolean) {
        notificationManager.setRenewalReminderEnabled(enabled)
        _renewalNotificationsEnabled.value = enabled
        _snackbarMessage.value = if (enabled) {
            "اعلان‌های یادآوری تمدید اشتراک فعال شد."
        } else {
            "اعلان‌های یادآوری تمدید غیرفعال شد."
        }
    }

    fun sendTestRenewalNotification() {
        val state = uiState.value
        val ok = notificationManager.sendTestOrImmediateRenewalNotification(
            profile = state.userProfile,
            config = state.appConfig
        )
        _snackbarMessage.value = if (ok) {
            "نوتیفیکیشن یادآوری تمدید اشتراک ارسال شد."
        } else {
            "لطفاً ابتدا مجوز نمایش اعلان (Notifications) را تأیید کنید."
        }
    }

    fun sendAdminRenewalReminder(targetUserId: String, message: String) {
        if (!isAdmin) return
        viewModelScope.launch {
            _isCloudSaving.value = true
            val res = cloudAdminRepository.sendUserRenewalNotification(targetUserId, message)
            _isCloudSaving.value = false
            _snackbarMessage.value = if (res.isSuccess) {
                "نوتیفیکیشن یادآوری تمدید برای کاربر ارسال شد."
            } else {
                "خطا در ارسال یادآور تمدید: ${res.exceptionOrNull()?.localizedMessage ?: ""}"
            }
        }
    }

    fun openExternalAction(rawValue: String) {
        val clean = rawValue.trim()
        if (clean.isEmpty()) return
        val allowed = clean.startsWith("https://", ignoreCase = true) ||
            clean.startsWith("http://", ignoreCase = true) ||
            clean.startsWith("tel:", ignoreCase = true)
        if (!allowed) {
            _snackbarMessage.value = "لینک یا کد اقدام معتبر نیست."
            return
        }
        viewModelScope.launch {
            _shareIntentEvent.emit(Intent(Intent.ACTION_VIEW, Uri.parse(clean)))
        }
    }

    fun consumeSnackbar() {
        _snackbarMessage.value = null
    }

    override fun onCleared() {
        super.onCleared()
        bluetoothManager.release()
    }

    private data class BtStatusTuple(
        val supported: Boolean,
        val enabled: Boolean,
        val perms: Boolean,
        val scanning: Boolean,
        val permSummary: PermissionSummary
    )

    private data class BtDevicesTuple(
        val paired: List<DiscoveredBluetoothDevice>,
        val nearby: List<DiscoveredBluetoothDevice>,
        val state: TransferSessionState,
        val sourceAddr: String?,
        val incoming: IncomingBridgeEvent?,
        val relayRole: RelayDeviceRole,
        val relayState: RelayConnectionState,
        val autoReconnectEnabled: Boolean
    )

    private data class LocalDataTuple(
        val sms: List<SmsRecordEntity>,
        val calls: List<CallRecordEntity>,
        val contacts: List<ContactEntity>,
        val history: List<TransferHistoryEntity>,
        val selectedThreadPhone: String?,
        val cachedBridgeSmsCount: Int,
        val cachedBridgeContactsCount: Int
    )

    private data class DialerTuple(
        val dark: Boolean,
        val simSlot: Int,
        val dialerNumber: String,
        val activeCall: ActiveBridgeCall?,
        val quietHours: Boolean
    )

    private data class BridgePrefsTuple(
        val mode: TransferMode,
        val format: ExportFormat,
        val pin: String,
        val encryption: Boolean,
        val showPaywall: Boolean
    )

    private data class CloudTuple(
        val config: AppGlobalConfig,
        val profile: UserProfile?,
        val published: List<AnnouncementModule>,
        val adminModules: List<AnnouncementModule>,
        val users: List<UserProfile>,
        val userPayments: List<PaymentRequest>,
        val adminPayments: List<PaymentRequest>,
        val saving: Boolean
    )

    companion object {
        fun factory(
            context: Context,
            userId: String,
            userEmail: String,
            isAdmin: Boolean
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                val appCtx = context.applicationContext
                val db = TransferDatabase.getInstance(appCtx)
                val repo = TransferRepository(appCtx, db.transferDao())
                val btMgr = BluetoothTransferManager(appCtx)
                val cloudRepo = CloudAdminRepository(appCtx)
                return MainViewModel(
                    appContext = appCtx,
                    repository = repo,
                    bluetoothManager = btMgr,
                    cloudAdminRepository = cloudRepo,
                    currentUserId = userId,
                    currentUserEmail = userEmail,
                    isAdmin = isAdmin
                ) as T
            }
        }
    }
}
