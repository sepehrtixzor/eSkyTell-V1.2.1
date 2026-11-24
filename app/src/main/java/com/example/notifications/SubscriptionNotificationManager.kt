package com.example.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.model.AppGlobalConfig
import com.example.data.model.SubscriptionTier
import com.example.data.model.UserProfile
import com.example.data.model.formatTomanAmount
import kotlin.math.ceil

class SubscriptionNotificationManager(private val context: Context) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    companion object {
        const val CHANNEL_ID = "eskytell_subscription_renewal"
        const val EXTRA_OPEN_PAYWALL = "extra_open_paywall"
        private const val PREFS_NAME = "eskytell_notification_prefs"
        private const val KEY_RENEWAL_ENABLED = "renewal_notifications_enabled"
        private const val KEY_LAST_EXPIRY_NOTIFY_MS = "last_expiry_notify_ms"
        private const val KEY_LAST_ADMIN_NOTICE_MS = "last_admin_notice_ms"
        private const val EXPIRY_NOTIFY_COOLDOWN_MS = 24L * 60L * 60L * 1000L
        private const val EXPIRY_WARNING_THRESHOLD_DAYS = 5
        private const val NOTIFICATION_ID_RENEWAL = 4102
        private const val NOTIFICATION_ID_ADMIN_NOTICE = 4103
    }

    fun isRenewalReminderEnabled(): Boolean = prefs.getBoolean(KEY_RENEWAL_ENABLED, true)

    fun setRenewalReminderEnabled(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_RENEWAL_ENABLED, enabled).apply()
    }

    fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                appContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(appContext).areNotificationsEnabled()
        }
    }

    fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                "یادآوری تمدید اشتراک eSkyTell",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "اعلان‌های زمان سررسید و تمدید اشتراک eSkyTell Pro"
            }
            manager.createNotificationChannel(channel)
        }
    }

    fun daysRemaining(expiresAtMillis: Long): Int? {
        if (expiresAtMillis <= 0L) return null
        val diff = expiresAtMillis - System.currentTimeMillis()
        if (diff <= 0L) return 0
        return ceil(diff.toDouble() / 86_400_000.0).toInt().coerceAtLeast(1)
    }

    fun evaluateAndNotifyIfNeeded(profile: UserProfile?, config: AppGlobalConfig) {
        if (profile == null) return
        if (!isRenewalReminderEnabled() || !hasNotificationPermission()) return

        // 1. Admin-pushed renewal alert for this user
        val noticeAt = profile.renewalNoticeAtMillis
        val lastSeenNotice = prefs.getLong(KEY_LAST_ADMIN_NOTICE_MS, 0L)
        if (noticeAt > lastSeenNotice && profile.renewalNoticeMessage.isNotBlank()) {
            val posted = showRenewalNotification(
                notificationId = NOTIFICATION_ID_ADMIN_NOTICE,
                title = "یادآوری تمدید اشتراک eSkyTell",
                message = profile.renewalNoticeMessage
            )
            if (posted) {
                prefs.edit().putLong(KEY_LAST_ADMIN_NOTICE_MS, noticeAt).apply()
            }
        }

        // 2. Automatic countdown alert when subscription is within 5 days of expiry or expired
        val tier = SubscriptionTier.fromRaw(profile.subscriptionTier)
        if (!config.subscriptionRequired && !profile.hasActiveSubscription) return
        if (tier == SubscriptionTier.LIFETIME || profile.subscriptionExpiresAtMillis <= 0L) return

        val remaining = daysRemaining(profile.subscriptionExpiresAtMillis) ?: return
        val now = System.currentTimeMillis()
        val lastExpiryAlert = prefs.getLong(KEY_LAST_EXPIRY_NOTIFY_MS, 0L)
        if (remaining <= EXPIRY_WARNING_THRESHOLD_DAYS && (now - lastExpiryAlert) >= EXPIRY_NOTIFY_COOLDOWN_MS) {
            val priceLabel = if (config.monthlyPriceToman > 0L) {
                formatTomanAmount(config.monthlyPriceToman)
            } else {
                config.monthlyPriceLabel
            }
            val body = if (remaining == 0) {
                "مهلت اشتراک eSkyTell Pro شما به پایان رسیده است. برای ادامه استفاده از پل بلوتوث، اشتراک خود را تمدید کنید ($priceLabel)."
            } else {
                "تنها $remaining روز تا پایان اشتراک ${tier.labelFa} باقی مانده است. برای تمدید و ارسال فیش اقدام کنید."
            }
            val posted = showRenewalNotification(
                notificationId = NOTIFICATION_ID_RENEWAL,
                title = if (remaining == 0) "اشتراک شما منقضی شده است" else "یادآوری تمدید اشتراک ($remaining روز مانده)",
                message = body
            )
            if (posted) {
                prefs.edit().putLong(KEY_LAST_EXPIRY_NOTIFY_MS, now).apply()
            }
        }
    }

    fun sendTestOrImmediateRenewalNotification(profile: UserProfile?, config: AppGlobalConfig): Boolean {
        if (!hasNotificationPermission()) return false
        val remaining = profile?.subscriptionExpiresAtMillis?.let { daysRemaining(it) }
        val priceText = if (config.monthlyPriceToman > 0L) {
            formatTomanAmount(config.monthlyPriceToman)
        } else {
            config.monthlyPriceLabel
        }
        val body = when {
            remaining != null && remaining > 0 ->
                "یادآوری تمدید: $remaining روز تا پایان اشتراک شما باقی مانده است. تعرفه تمدید ماهانه: $priceText"
            else ->
                "اشتراک eSkyTell Pro آماده تمدید است. برای تمدید سریع و آپلود فیش واریزی ضربه بزنید ($priceText)."
        }
        return showRenewalNotification(
            notificationId = NOTIFICATION_ID_RENEWAL,
            title = "یادآوری تمدید اشتراک eSkyTell",
            message = body
        )
    }

    fun showRenewalNotification(
        notificationId: Int = NOTIFICATION_ID_RENEWAL,
        title: String,
        message: String
    ): Boolean {
        if (!hasNotificationPermission()) return false
        ensureNotificationChannel()

        val launchIntent = Intent(appContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_OPEN_PAYWALL, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            appContext,
            notificationId,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        return runCatching {
            NotificationManagerCompat.from(appContext).notify(notificationId, notification)
            true
        }.getOrDefault(false)
    }
}
