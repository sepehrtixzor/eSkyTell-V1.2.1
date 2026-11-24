package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import java.text.NumberFormat
import java.util.Locale

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

enum class AnnouncementCategory(val labelFa: String) {
    FEATURE("قابلیت جدید"),
    UPDATE("آپدیت سیستم"),
    TIP("راهنما و ترفند"),
    USSD("کد دستوری");

    companion object {
        fun fromRaw(raw: String): AnnouncementCategory =
            entries.firstOrNull { it.name == raw } ?: FEATURE
    }
}

enum class SubscriptionTier(val labelFa: String) {
    FREE("رایگان"),
    MONTHLY("اشتراک ماهانه"),
    YEARLY("اشتراک سالانه"),
    LIFETIME("اشتراک دائمی");

    companion object {
        fun fromRaw(raw: String): SubscriptionTier =
            entries.firstOrNull { it.name == raw } ?: FREE
    }
}

enum class PaymentRequestStatus(val labelFa: String) {
    PENDING("در انتظار بررسی"),
    APPROVED("تایید شده"),
    REJECTED("رد شده");

    companion object {
        fun fromRaw(raw: String): PaymentRequestStatus =
            entries.firstOrNull { it.name == raw } ?: PENDING
    }
}

enum class PaymentMethodType(val labelFa: String) {
    CARD_TO_CARD("کارت به کارت بانکی"),
    CRYPTO("کریپتوکارنسی / رمزارز"),
    GATEWAY("درگاه آنلاین / لینک پرداخت");

    companion object {
        fun fromRaw(raw: String): PaymentMethodType =
            entries.firstOrNull { it.name == raw } ?: CARD_TO_CARD
    }
}

@Keep
@IgnoreExtraProperties
data class BankCardInfo(
    val bankName: String = "",
    val cardNumber: String = "",
    val holderName: String = ""
)

@Keep
@IgnoreExtraProperties
data class CryptoWalletInfo(
    val networkName: String = "",
    val address: String = ""
)

@Keep
@IgnoreExtraProperties
data class AppGlobalConfig(
    @DocumentId val id: String = "global",
    val subscriptionRequired: Boolean = false,
    val monthlyPriceToman: Long = 99_000L,
    val yearlyPriceToman: Long = 790_000L,
    val monthlyPriceLabel: String = "۹۹,۰۰۰ تومان / ماه",
    val yearlyPriceLabel: String = "۷۹۰,۰۰۰ تومان / سال",
    val freeTrialDays: Long = 7L,
    val paywallMessage: String = "برای استفاده نامحدود از پل بلوتوثی تماس و پیامک، اشتراک eSkyTell Pro را فعال کنید.",
    val paymentUrl: String = "",
    val bankCards: List<BankCardInfo> = emptyList(),
    val cryptoWallets: List<CryptoWalletInfo> = emptyList(),
    val latestVersionName: String = "1.0.0",
    val latestVersionCode: Long = 1L,
    val updateNotes: String = "",
    val updateUrl: String = "",
    val forceUpdate: Boolean = false,
    val enableDualSim: Boolean = true,
    val enableQuickTemplates: Boolean = true,
    val enableQuietHours: Boolean = true,
    val enableBleGattBridge: Boolean = true,
    val updatedBy: String = "system",
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PaymentRequest(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val userDisplayName: String = "",
    val requestedTier: String = SubscriptionTier.MONTHLY.name,
    val amountToman: Long = 0L,
    val paymentMethod: String = PaymentMethodType.CARD_TO_CARD.name,
    val targetAccountLabel: String = "",
    val trackingCode: String = "",
    val receiptImageBase64: String = "",
    val userNote: String = "",
    val status: String = PaymentRequestStatus.PENDING.name,
    val adminNote: String = "",
    val reviewedBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AnnouncementModule(
    @DocumentId val id: String = "",
    val title: String = "",
    val summary: String = "",
    val category: String = AnnouncementCategory.FEATURE.name,
    val actionLabel: String = "",
    val actionValue: String = "",
    val published: Boolean = true,
    val createdBy: String = "",
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String = "",
    val sim1Label: String = "SIM 1 • خط کاری",
    val sim2Label: String = "SIM 2 • خط شخصی",
    val hasActiveSubscription: Boolean = false,
    val subscriptionTier: String = SubscriptionTier.FREE.name,
    val subscriptionExpiresAtMillis: Long = 0L,
    val renewalNoticeMessage: String = "",
    val renewalNoticeAtMillis: Long = 0L,
    val createdAt: Timestamp? = null,
    val updatedAt: Timestamp? = null
)

fun formatTomanAmount(amount: Long, suffix: String = ""): String {
    val formatter = NumberFormat.getIntegerInstance(Locale.US)
    val base = "${formatter.format(amount.coerceAtLeast(0L))} تومان"
    return if (suffix.isBlank()) base else "$base $suffix"
}
