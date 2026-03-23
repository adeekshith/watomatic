package com.parishod.watomatic.model.utils

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NotificationUtilsTest {

    private lateinit var mockSbn: StatusBarNotification
    private lateinit var context: Context

    @Before
    fun setUp() {
        mockSbn = mock()
        context = ApplicationProvider.getApplicationContext()
    }

    // --- isNewNotification ---

    @Test
    fun `isNewNotification returns true when notification when is 0`() {
        val notification = Notification()
        notification.`when` = 0
        whenever(mockSbn.notification).thenReturn(notification)

        assertTrue(NotificationUtils.isNewNotification(mockSbn))
    }

    @Test
    fun `isNewNotification returns true for notification within 2 minutes`() {
        val notification = Notification()
        notification.`when` = System.currentTimeMillis() - 60_000L // 1 minute ago
        whenever(mockSbn.notification).thenReturn(notification)

        assertTrue(NotificationUtils.isNewNotification(mockSbn))
    }

    @Test
    fun `isNewNotification returns true for very recent notification`() {
        val notification = Notification()
        notification.`when` = System.currentTimeMillis() - 1_000L // 1 second ago
        whenever(mockSbn.notification).thenReturn(notification)

        assertTrue(NotificationUtils.isNewNotification(mockSbn))
    }

    @Test
    fun `isNewNotification returns false for notification older than 2 minutes`() {
        val notification = Notification()
        notification.`when` = System.currentTimeMillis() - (3 * 60 * 1_000L) // 3 minutes ago
        whenever(mockSbn.notification).thenReturn(notification)

        assertFalse(NotificationUtils.isNewNotification(mockSbn))
    }

    @Test
    fun `isNewNotification returns false for very old notification`() {
        val notification = Notification()
        notification.`when` = System.currentTimeMillis() - (60 * 60 * 1_000L) // 1 hour ago
        whenever(mockSbn.notification).thenReturn(notification)

        assertFalse(NotificationUtils.isNewNotification(mockSbn))
    }

    // --- getTitle for non-group conversations ---

    @Test
    fun `getTitle returns android title for non-group conversation`() {
        val extras = Bundle()
        extras.putBoolean("android.isGroupConversation", false)
        extras.putString("android.title", "John Doe")

        val notification = Notification()
        notification.extras = extras
        whenever(mockSbn.notification).thenReturn(notification)

        assertEquals("John Doe", NotificationUtils.getTitle(mockSbn))
    }

    @Test
    fun `getTitle returns null when android title is not set for non-group`() {
        val extras = Bundle()
        extras.putBoolean("android.isGroupConversation", false)
        // No android.title set

        val notification = Notification()
        notification.extras = extras
        whenever(mockSbn.notification).thenReturn(notification)

        assertNull(NotificationUtils.getTitle(mockSbn))
    }

    // --- getTitle for group conversations ---

    @Test
    fun `getTitle returns hiddenConversationTitle for group conversation`() {
        val extras = Bundle()
        extras.putBoolean("android.isGroupConversation", true)
        extras.putString("android.hiddenConversationTitle", "Family Group")

        val notification = Notification()
        notification.extras = extras
        whenever(mockSbn.notification).thenReturn(notification)

        assertEquals("Family Group", NotificationUtils.getTitle(mockSbn))
    }

    @Test
    fun `getTitle extracts group name before colon when hiddenConversationTitle is null`() {
        val extras = Bundle()
        extras.putBoolean("android.isGroupConversation", true)
        extras.putString("android.hiddenConversationTitle", null)
        extras.putString("android.title", "Family Group: John")

        val notification = Notification()
        notification.extras = extras
        whenever(mockSbn.notification).thenReturn(notification)

        assertEquals("Family Group", NotificationUtils.getTitle(mockSbn))
    }

    @Test
    fun `getTitle returns full title when no colon in group title`() {
        val extras = Bundle()
        extras.putBoolean("android.isGroupConversation", true)
        extras.putString("android.hiddenConversationTitle", null)
        extras.putString("android.title", "WorkTeam")

        val notification = Notification()
        notification.extras = extras
        whenever(mockSbn.notification).thenReturn(notification)

        assertEquals("WorkTeam", NotificationUtils.getTitle(mockSbn))
    }

    // --- getTitleRaw ---

    @Test
    fun `getTitleRaw returns raw android title string`() {
        val extras = Bundle()
        extras.putString("android.title", "Raw Title Value")

        val notification = Notification()
        notification.extras = extras
        whenever(mockSbn.notification).thenReturn(notification)

        assertEquals("Raw Title Value", NotificationUtils.getTitleRaw(mockSbn))
    }

    @Test
    fun `getTitleRaw returns null when android title not set`() {
        val extras = Bundle()
        // No android.title

        val notification = Notification()
        notification.extras = extras
        whenever(mockSbn.notification).thenReturn(notification)

        assertNull(NotificationUtils.getTitleRaw(mockSbn))
    }

    // --- getTitle: group title with message count trimming ---

    @Test
    fun `getTitle strips message count suffix from group title when multiple messages`() {
        val extras = Bundle()
        extras.putBoolean("android.isGroupConversation", true)
        extras.putString("android.hiddenConversationTitle", null)
        // title with count like "Family Group(3 messages)"
        extras.putString("android.title", "Family Group(3 messages)")
        // Simulate 2 messages in the bundle so the trimming branch is hit
        val fakeMessages = arrayOfNulls<Parcelable>(2)
        extras.putParcelableArray("android.messages", fakeMessages)

        val notification = Notification()
        notification.extras = extras
        whenever(mockSbn.notification).thenReturn(notification)

        val title = NotificationUtils.getTitle(mockSbn)
        // Should strip from the last '(' onward
        assertEquals("Family Group", title)
    }

    @Test
    fun `getTitle does not strip suffix when only one message`() {
        val extras = Bundle()
        extras.putBoolean("android.isGroupConversation", true)
        extras.putString("android.hiddenConversationTitle", null)
        extras.putString("android.title", "Family Group(1 message)")
        // Only 1 message — trimming branch not taken
        val fakeMessages = arrayOfNulls<Parcelable>(1)
        extras.putParcelableArray("android.messages", fakeMessages)

        val notification = Notification()
        notification.extras = extras
        whenever(mockSbn.notification).thenReturn(notification)

        val title = NotificationUtils.getTitle(mockSbn)
        // No colon, so full title returned (trimming skipped because b.length == 1)
        assertEquals("Family Group(1 message)", title)
    }

    // --- extractWearNotification ---

    @Test
    fun `extractWearNotification returns correct packageName`() {
        val notification = NotificationCompat.Builder(context, "test_channel").build()
        whenever(mockSbn.notification).thenReturn(notification)
        whenever(mockSbn.packageName).thenReturn("com.whatsapp")
        whenever(mockSbn.tag).thenReturn(null)

        val result = NotificationUtils.extractWearNotification(mockSbn)

        assertEquals("com.whatsapp", result.packageName)
    }

    @Test
    fun `extractWearNotification returns empty remoteInputs when notification has no actions`() {
        val notification = NotificationCompat.Builder(context, "test_channel").build()
        whenever(mockSbn.notification).thenReturn(notification)
        whenever(mockSbn.packageName).thenReturn("com.whatsapp")
        whenever(mockSbn.tag).thenReturn(null)

        val result = NotificationUtils.extractWearNotification(mockSbn)

        assertTrue(result.remoteInputs.isEmpty())
        assertNull(result.pendingIntent)
    }

    @Test
    fun `extractWearNotification captures tag from sbn`() {
        val notification = NotificationCompat.Builder(context, "test_channel").build()
        whenever(mockSbn.notification).thenReturn(notification)
        whenever(mockSbn.packageName).thenReturn("com.whatsapp")
        whenever(mockSbn.tag).thenReturn("my_notification_tag")

        val result = NotificationUtils.extractWearNotification(mockSbn)

        assertEquals("my_notification_tag", result.tag)
    }

    @Test
    fun `extractWearNotification assigns non-null unique id`() {
        val notification = NotificationCompat.Builder(context, "test_channel").build()
        whenever(mockSbn.notification).thenReturn(notification)
        whenever(mockSbn.packageName).thenReturn("com.whatsapp")
        whenever(mockSbn.tag).thenReturn(null)

        val result1 = NotificationUtils.extractWearNotification(mockSbn)
        val result2 = NotificationUtils.extractWearNotification(mockSbn)

        assertNotNull(result1.id)
        assertNotNull(result2.id)
        assertTrue(result1.id != result2.id)
    }

    @Test
    fun `extractWearNotification picks action with RemoteInput`() {
        val intent = Intent("test_action")
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        val remoteInput = RemoteInput.Builder("key_text_reply")
            .setLabel("Reply")
            .build()
        val action = NotificationCompat.Action.Builder(0, "Reply", pendingIntent)
            .addRemoteInput(remoteInput)
            .build()

        val notification = NotificationCompat.Builder(context, "test_channel")
            .addAction(action)
            .build()

        whenever(mockSbn.notification).thenReturn(notification)
        whenever(mockSbn.packageName).thenReturn("com.whatsapp")
        whenever(mockSbn.tag).thenReturn(null)

        val result = NotificationUtils.extractWearNotification(mockSbn)

        assertEquals(1, result.remoteInputs.size)
        assertNotNull(result.pendingIntent)
    }

    @Test
    fun `extractWearNotification prefers action with reply in title`() {
        val intent = Intent("test_action")
        val pendingIntent1 = PendingIntent.getBroadcast(
            context, 1, intent, PendingIntent.FLAG_IMMUTABLE
        )
        val pendingIntent2 = PendingIntent.getBroadcast(
            context, 2, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val remoteInput1 = RemoteInput.Builder("key_mark_read").build()
        val actionMarkRead = NotificationCompat.Action.Builder(0, "Mark as Read", pendingIntent1)
            .addRemoteInput(remoteInput1)
            .build()

        val remoteInput2 = RemoteInput.Builder("key_reply").build()
        val actionReply = NotificationCompat.Action.Builder(0, "Reply", pendingIntent2)
            .addRemoteInput(remoteInput2)
            .build()

        val notification = NotificationCompat.Builder(context, "test_channel")
            .addAction(actionMarkRead)
            .addAction(actionReply)
            .build()

        whenever(mockSbn.notification).thenReturn(notification)
        whenever(mockSbn.packageName).thenReturn("com.whatsapp")
        whenever(mockSbn.tag).thenReturn(null)

        val result = NotificationUtils.extractWearNotification(mockSbn)

        // Should pick the "Reply" action over "Mark as Read"
        assertEquals(1, result.remoteInputs.size)
        assertEquals("key_reply", result.remoteInputs[0].resultKey)
    }

    @Test
    fun `extractWearNotification ignores actions without free-form RemoteInput`() {
        val intent = Intent("test_action")
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        // Action with no RemoteInput
        val action = NotificationCompat.Action.Builder(0, "Dismiss", pendingIntent).build()

        val notification = NotificationCompat.Builder(context, "test_channel")
            .addAction(action)
            .build()

        whenever(mockSbn.notification).thenReturn(notification)
        whenever(mockSbn.packageName).thenReturn("com.whatsapp")
        whenever(mockSbn.tag).thenReturn(null)

        val result = NotificationUtils.extractWearNotification(mockSbn)

        assertTrue(result.remoteInputs.isEmpty())
        assertNull(result.pendingIntent)
    }

    @Test
    fun `extractWearNotification picks action from WearableExtender when no standard actions`() {
        val intent = Intent("test_action")
        val pendingIntent = PendingIntent.getBroadcast(
            context, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )
        val remoteInput = RemoteInput.Builder("key_wear_reply")
            .setLabel("Reply")
            .build()
        val wearAction = NotificationCompat.Action.Builder(0, "Reply", pendingIntent)
            .addRemoteInput(remoteInput)
            .build()

        // Only add via WearableExtender, not as a standard action
        val notification = NotificationCompat.Builder(context, "test_channel")
            .extend(NotificationCompat.WearableExtender().addAction(wearAction))
            .build()

        whenever(mockSbn.notification).thenReturn(notification)
        whenever(mockSbn.packageName).thenReturn("com.whatsapp")
        whenever(mockSbn.tag).thenReturn(null)

        val result = NotificationUtils.extractWearNotification(mockSbn)

        assertEquals(1, result.remoteInputs.size)
        assertEquals("key_wear_reply", result.remoteInputs[0].resultKey)
        assertNotNull(result.pendingIntent)
    }

    // --- showAccessRevokedNotification ---

    @Test
    fun `showAccessRevokedNotification does not throw`() {
        // Smoke test: just verify no exception is thrown
        NotificationUtils.showAccessRevokedNotification(context)
    }

    @Test
    fun `showAccessRevokedNotification creates notification channel`() {
        NotificationUtils.showAccessRevokedNotification(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channel = nm.getNotificationChannel("nls_health_channel")
        assertNotNull(channel)
        assertEquals("Notification Access Alerts", channel.name.toString())
    }

    @Test
    fun `showAccessRevokedNotification posts a notification`() {
        NotificationUtils.showAccessRevokedNotification(context)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notifications = Shadows.shadowOf(nm).allNotifications
        assertTrue(notifications.isNotEmpty())
    }

    // --- Edge cases ---

    @Test
    fun `getTitle handles emoji in group title`() {
        val extras = Bundle()
        extras.putBoolean("android.isGroupConversation", true)
        extras.putString("android.hiddenConversationTitle", "\uD83D\uDE00 Fun Group")

        val notification = Notification()
        notification.extras = extras
        whenever(mockSbn.notification).thenReturn(notification)

        assertEquals("\uD83D\uDE00 Fun Group", NotificationUtils.getTitle(mockSbn))
    }

    @Test
    fun `getTitle handles RTL characters in title`() {
        val extras = Bundle()
        extras.putBoolean("android.isGroupConversation", false)
        extras.putString("android.title", "\u0645\u062D\u0645\u062F") // Arabic name

        val notification = Notification()
        notification.extras = extras
        whenever(mockSbn.notification).thenReturn(notification)

        assertEquals("\u0645\u062D\u0645\u062F", NotificationUtils.getTitle(mockSbn))
    }

    @Test
    fun `getTitle returns null for group conversation with null title and null hiddenTitle`() {
        val extras = Bundle()
        extras.putBoolean("android.isGroupConversation", true)

        val notification = Notification()
        notification.extras = extras
        whenever(mockSbn.notification).thenReturn(notification)

        assertNull(NotificationUtils.getTitle(mockSbn))
    }

    @Test
    fun `extractWearNotification returns empty remoteInputs when all actions lack RemoteInput`() {
        val intent = Intent("test_action")
        val pi = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE)
        val action1 = NotificationCompat.Action.Builder(0, "Archive", pi).build()
        val action2 = NotificationCompat.Action.Builder(0, "Delete", pi).build()

        val notification = NotificationCompat.Builder(context, "test_channel")
            .addAction(action1)
            .addAction(action2)
            .build()

        whenever(mockSbn.notification).thenReturn(notification)
        whenever(mockSbn.packageName).thenReturn("com.whatsapp")
        whenever(mockSbn.tag).thenReturn(null)

        val result = NotificationUtils.extractWearNotification(mockSbn)
        assertTrue(result.remoteInputs.isEmpty())
        assertNull(result.pendingIntent)
    }

    @Test
    fun `isNewNotification returns true for future notification timestamp`() {
        val notification = Notification()
        notification.`when` = System.currentTimeMillis() + 60_000L // 1 minute in the future
        whenever(mockSbn.notification).thenReturn(notification)

        assertTrue(NotificationUtils.isNewNotification(mockSbn))
    }
}
