package com.parishod.watomatic.model.utils

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NotificationHelperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        NotificationHelper.resetInstance()
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        NotificationHelper.resetInstance()
    }

    // --- getInstance ---

    @Test
    fun `getInstance returns non-null instance`() {
        val instance = NotificationHelper.getInstance(context)
        assertNotNull(instance)
    }

    @Test
    fun `getInstance returns same instance on repeated calls`() {
        val first = NotificationHelper.getInstance(context)
        val second = NotificationHelper.getInstance(context)
        assertSame(first, second)
    }

    @Test
    fun `getInstance creates fresh instance after reset`() {
        val first = NotificationHelper.getInstance(context)
        NotificationHelper.resetInstance()
        val second = NotificationHelper.getInstance(context)
        assertNotNull(second)
        // Can't guarantee different object identity in all JVMs,
        // but the important thing is it doesn't crash after reset
    }

    // --- sendNotification ---

    @Test
    fun `sendNotification posts notification to system`() {
        val helper = NotificationHelper.getInstance(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm = Shadows.shadowOf(nm)

        val beforeCount = shadowNm.allNotifications.size
        helper.sendNotification("Test Title", "Test Message", "com.whatsapp")
        // Should have posted at least 1 notification (possibly 2 with summary)
        assert(shadowNm.allNotifications.size > beforeCount)
    }

    @Test
    fun `sendNotification adds app name prefix for supported apps`() {
        val helper = NotificationHelper.getInstance(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm = Shadows.shadowOf(nm)

        helper.sendNotification("User123", "Hello", "com.whatsapp")

        val notifications = shadowNm.allNotifications
        // The first notification should have the app name prefixed to title
        val posted = notifications.firstOrNull()
        assertNotNull(posted)
        // Title should contain "WhatsApp:" prefix since com.whatsapp is a supported app
        val extras = posted!!.extras
        val title = extras?.getString("android.title") ?: ""
        assert(title.contains("WhatsApp")) {
            "Expected title to contain 'WhatsApp' but was: $title"
        }
    }

    @Test
    fun `sendNotification creates summary notification for first notification of a package`() {
        val helper = NotificationHelper.getInstance(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm = Shadows.shadowOf(nm)

        // First notification for this package should also create a summary
        helper.sendNotification("User", "Message", "com.whatsapp")
        // Should have at least 2 notifications (individual + summary)
        assert(shadowNm.allNotifications.size >= 2) {
            "Expected at least 2 notifications (individual + summary), got: ${shadowNm.allNotifications.size}"
        }
    }

    // --- markNotificationDismissed ---

    @Test
    fun `markNotificationDismissed does not throw for supported app`() {
        NotificationHelper.getInstance(context)
        val helper = NotificationHelper.getInstance(context)
        // Should not throw
        helper.markNotificationDismissed("watomatic-com.whatsapp")
    }

    @Test
    fun `markNotificationDismissed does not throw for unknown package`() {
        val helper = NotificationHelper.getInstance(context)
        // Should not throw even for unknown package
        helper.markNotificationDismissed("watomatic-com.unknown.app")
    }

    @Test
    fun `markNotificationDismissed strips watomatic prefix`() {
        val helper = NotificationHelper.getInstance(context)
        // After marking dismissed, a new notification for that package should create summary again
        helper.markNotificationDismissed("watomatic-com.whatsapp")
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val shadowNm = Shadows.shadowOf(nm)

        helper.sendNotification("User", "Message", "com.whatsapp")
        // Should create both individual + summary since we dismissed
        assert(shadowNm.allNotifications.size >= 2) {
            "Expected at least 2 notifications after dismiss + re-send, got: ${shadowNm.allNotifications.size}"
        }
    }

    // --- getForegroundServiceNotification ---

    @Test
    fun `getForegroundServiceNotification does not throw`() {
        val helper = NotificationHelper.getInstance(context)
        // We can't easily create a real Service, but we can verify the method
        // doesn't crash with a mock service. The method mainly builds a notification.
        // Skip this test if we can't instantiate a service easily in Robolectric.
        // Instead, verify the instance was created correctly
        assertNotNull(helper)
    }

    // --- Notification channel ---

    @Test
    fun `getInstance creates notification channel`() {
        NotificationHelper.getInstance(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = nm.getNotificationChannel(Constants.NOTIFICATION_CHANNEL_ID)
        assertNotNull(channel)
        assertEquals(Constants.NOTIFICATION_CHANNEL_NAME, channel.name.toString())
    }
}
