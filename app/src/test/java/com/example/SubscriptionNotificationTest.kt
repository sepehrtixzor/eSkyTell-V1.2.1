package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AppGlobalConfig
import com.example.data.model.SubscriptionTier
import com.example.data.model.UserProfile
import com.example.notifications.SubscriptionNotificationManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SubscriptionNotificationTest {

    @Test
    fun `daysRemaining calculates positive days, zero when expired, and null when unset`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = SubscriptionNotificationManager(context)

        assertNull(manager.daysRemaining(0L))
        assertEquals(0, manager.daysRemaining(System.currentTimeMillis() - 60_000L))

        val threeDaysLater = System.currentTimeMillis() + (3L * 86_400_000L) - 10_000L
        assertEquals(3, manager.daysRemaining(threeDaysLater))
    }

    @Test
    fun `renewal preference toggles and persists in shared preferences`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = SubscriptionNotificationManager(context)

        assertTrue(manager.isRenewalReminderEnabled())
        manager.setRenewalReminderEnabled(false)
        assertFalse(manager.isRenewalReminderEnabled())
        manager.setRenewalReminderEnabled(true)
        assertTrue(manager.isRenewalReminderEnabled())
    }

    @Test
    fun `evaluateAndNotifyIfNeeded does not crash on free or expiring profiles`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = SubscriptionNotificationManager(context)
        manager.ensureNotificationChannel()

        val expiringProfile = UserProfile(
            userId = "u1",
            email = "user@example.com",
            displayName = "Test User",
            hasActiveSubscription = true,
            subscriptionTier = SubscriptionTier.MONTHLY.name,
            subscriptionExpiresAtMillis = System.currentTimeMillis() + (2L * 86_400_000L),
            renewalNoticeMessage = "یادآوری تست",
            renewalNoticeAtMillis = System.currentTimeMillis()
        )
        manager.evaluateAndNotifyIfNeeded(expiringProfile, AppGlobalConfig(subscriptionRequired = true))
    }
}
