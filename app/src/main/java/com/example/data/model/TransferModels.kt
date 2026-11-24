package com.example.data.model

enum class SmsDirection(val labelFa: String) {
    INCOMING("دریافتی"),
    OUTGOING("ارسالی"),
    DRAFT("پیش‌نویس")
}

enum class CallDirection(val labelFa: String) {
    INCOMING("ورودی"),
    OUTGOING("خروجی"),
    MISSED("بی‌پاسخ"),
    REJECTED("رد شده")
}

enum class ExportFormat(
    val extension: String,
    val mimeType: String,
    val titleFa: String,
    val subtitleFa: String
) {
    HTML_REPORT(
        extension = "html",
        mimeType = "text/html",
        titleFa = "گزارش تعاملی (HTML)",
        subtitleFa = "مناسب آیفون و مرورگر — نمایش کامل پیامک‌ها و تماس‌ها"
    ),
    VCARD_CONTACTS(
        extension = "vcf",
        mimeType = "text/vcard",
        titleFa = "کارت مخاطبین (vCard)",
        subtitleFa = "افزودن مستقیم شماره‌های تماس به مخاطبین آیفون"
    ),
    JSON_BUNDLE(
        extension = "json",
        mimeType = "application/json",
        titleFa = "بسته داده (JSON)",
        subtitleFa = "انتقال ساختاریافته بین دستگاه‌ها و نرم‌افزارها"
    ),
    CSV_TABLE(
        extension = "csv",
        mimeType = "text/csv",
        titleFa = "جدول اکسل (CSV)",
        subtitleFa = "باز شدن در اپلیکیشن Numbers آیفون و اکسل"
    ),
    PLAIN_TEXT(
        extension = "txt",
        mimeType = "text/plain",
        titleFa = "متن ساده (TXT)",
        subtitleFa = "فایل سبک متنی قابل خواندن در همه گوشی‌ها"
    )
}

enum class TransferMode(val titleFa: String, val subtitleFa: String) {
    CROSS_PLATFORM_OPP(
        titleFa = "ارسال به آیفون و همه گوشی‌ها",
        subtitleFa = "ارسال فایل استاندارد از طریق بلوتوث (OPP) یا اشتراک‌گذاری به iOS و اندروید"
    ),
    BLE_GATT_BRIDGE(
        titleFa = "پل زنده بلوتوث (BLE GATT)",
        subtitleFa = "ارتباط کم‌مصرف و دوطرفه بلوتوث برای همگام‌سازی سریع با آیفون و اندروید"
    ),
    DIRECT_RFCOMM(
        titleFa = "اتصال مستقیم RFCOMM",
        subtitleFa = "همگام‌سازی پرسرعت تماس و پیامک بین دو گوشی دارای eSkyTell"
    )
}

enum class DevicePlatform(val labelFa: String) {
    IPHONE("آیفون / اپل"),
    ANDROID("اندروید / موبایل"),
    COMPUTER("لپ‌تاپ / دسکتاپ"),
    OTHER("سایر دستگاه‌ها")
}

data class DiscoveredBluetoothDevice(
    val name: String,
    val address: String,
    val isPaired: Boolean,
    val rssi: Int? = null,
    val platform: DevicePlatform = DevicePlatform.OTHER,
    val isBle: Boolean = false,
    val isConnected: Boolean = false
)

enum class BridgeCommandType {
    CALL_TRANSFER,
    SMS_RELAY,
    CONTACT_SYNC
}

data class IncomingBridgeEvent(
    val type: BridgeCommandType,
    val sourceDeviceName: String,
    val contactName: String,
    val phoneNumber: String,
    val messageBody: String = "",
    val simSlot: Int = 1,
    val timestampMillis: Long = System.currentTimeMillis()
)

data class PermissionSummary(
    val bluetoothGranted: Boolean = false,
    val contactsGranted: Boolean = false,
    val notificationsGranted: Boolean = false
) {
    val allGranted: Boolean
        get() = bluetoothGranted && contactsGranted && notificationsGranted
}

data class ActiveBridgeCall(
    val contactName: String,
    val phoneNumber: String,
    val simSlot: Int = 1,
    val targetDeviceName: String = "پل بلوتوث eSkyTell",
    val startedAtMillis: Long = System.currentTimeMillis(),
    val isMuted: Boolean = false,
    val isSpeaker: Boolean = false,
    val isBluetoothAudio: Boolean = true
)

data class SmsThreadSummary(
    val phoneNumber: String,
    val displayName: String,
    val lastMessage: String,
    val lastTimestampMillis: Long,
    val unreadCount: Int,
    val messageCount: Int,
    val simSlot: Int
)

sealed class TransferSessionState {
    data object Idle : TransferSessionState()
    data class Listening(val deviceName: String) : TransferSessionState()
    data class Connecting(val targetName: String) : TransferSessionState()
    data class Reconnecting(
        val targetName: String,
        val attempt: Int,
        val maxAttempts: Int
    ) : TransferSessionState()
    data class Transferring(
        val targetName: String,
        val progress: Float,
        val bytesSent: Long,
        val totalBytes: Long
    ) : TransferSessionState()
    data class Success(val message: String) : TransferSessionState()
    data class Error(val message: String) : TransferSessionState()
}

data class ImportResult(
    val smsAdded: Int,
    val callsAdded: Int,
    val sourceName: String,
    val contactsAdded: Int = 0
)

enum class RelayDeviceRole(val labelFa: String, val subtitleFa: String) {
    PRIMARY(
        labelFa = "دستگاه اصلی",
        subtitleFa = "گوشی مبدأ دارای سیم‌کارت برای رله تماس و پیامک"
    ),
    SECONDARY(
        labelFa = "دستگاه دوم",
        subtitleFa = "گوشی مقصد / آیفون متصل به پل بلوتوث"
    )
}

enum class RelayTransport(val labelFa: String) {
    AUTO("خودکار (RFCOMM / BLE)"),
    RFCOMM("کانال مستقیم RFCOMM"),
    BLE_GATT("کانال کم‌مصرف BLE GATT")
}

sealed class RelayConnectionState {
    data object Disconnected : RelayConnectionState()
    data class Listening(
        val role: RelayDeviceRole,
        val localDeviceName: String
    ) : RelayConnectionState()
    data class Connecting(
        val deviceName: String,
        val address: String
    ) : RelayConnectionState()
    data class Reconnecting(
        val deviceName: String,
        val address: String,
        val attempt: Int,
        val maxAttempts: Int,
        val nextRetrySeconds: Int
    ) : RelayConnectionState()
    data class Connected(
        val deviceName: String,
        val address: String,
        val role: RelayDeviceRole,
        val transport: RelayTransport
    ) : RelayConnectionState()
    data class Relaying(
        val deviceName: String,
        val progress: Float,
        val bytesTransferred: Long,
        val totalBytes: Long
    ) : RelayConnectionState()
    data class Error(val message: String) : RelayConnectionState()
}

data class RelayedEnvelope(
    val id: String,
    val senderAddress: String,
    val senderName: String,
    val senderRole: RelayDeviceRole,
    val transport: RelayTransport,
    val payload: String,
    val encrypted: Boolean,
    val timestampMillis: Long = System.currentTimeMillis()
)

