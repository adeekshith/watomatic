package com.parishod.watomatic.service

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.*
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.parishod.watomatic.model.preferences.PreferencesManager
import com.parishod.watomatic.model.utils.ContactsHelper
import com.parishod.watomatic.model.utils.DbUtils

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NotificationReplyDeciderTest {

    private lateinit var prefs: PreferencesManager
    private lateinit var dbUtils: DbUtils
    private lateinit var contactsHelper: ContactsHelper
    private lateinit var decider: NotificationReplyDecider

    @Before
    fun setUp() {
        prefs = mock()
        dbUtils = mock()
        contactsHelper = mock()
        decider = NotificationReplyDecider(prefs, dbUtils, contactsHelper)
    }

    // --- Helper to build mock StatusBarNotification ---

    private fun buildMockSbn(
        packageName: String = "com.whatsapp",
        title: String? = "John",
        text: String? = "Hello",
        isGroup: Boolean = false,
        selfDisplayName: String? = "Me",
        notificationTime: Long = System.currentTimeMillis()
    ): StatusBarNotification {
        val extras = Bundle().apply {
            putString("android.title", title)
            putCharSequence("android.text", text)
            putBoolean("android.isGroupConversation", isGroup)
            putString("android.selfDisplayName", selfDisplayName)
        }
        val notification = Notification().apply {
            this.extras = extras
            `when` = notificationTime
        }
        val sbn = mock<StatusBarNotification>()
        whenever(sbn.notification).thenReturn(notification)
        whenever(sbn.packageName).thenReturn(packageName)
        whenever(sbn.postTime).thenReturn(notificationTime)
        return sbn
    }

    private fun setupDefaultPrefs() {
        whenever(prefs.isServiceEnabled()).thenReturn(true)
        whenever(prefs.getEnabledApps()).thenReturn(setOf("com.whatsapp"))
        whenever(prefs.isGroupReplyEnabled()).thenReturn(false)
        whenever(prefs.getAutoReplyDelay()).thenReturn(0L)
        whenever(prefs.isContactReplyEnabled()).thenReturn(false)
    }

    // ========== canReply tests ==========

    @Test
    fun `canReply returns false when service is disabled`() {
        whenever(prefs.isServiceEnabled()).thenReturn(false)
        val sbn = buildMockSbn()
        assertFalse(decider.canReply(sbn))
    }

    @Test
    fun `canReply returns false for unsupported package`() {
        whenever(prefs.isServiceEnabled()).thenReturn(true)
        whenever(prefs.getEnabledApps()).thenReturn(setOf("com.whatsapp"))
        val sbn = buildMockSbn(packageName = "com.unknown.app")
        assertFalse(decider.canReply(sbn))
    }

    @Test
    fun `canReply returns false for old notification`() {
        whenever(prefs.isServiceEnabled()).thenReturn(true)
        whenever(prefs.getEnabledApps()).thenReturn(setOf("com.whatsapp"))
        // Notification older than 2 minutes
        val oldTime = System.currentTimeMillis() - 3 * 60 * 1000
        val sbn = buildMockSbn(notificationTime = oldTime)
        assertFalse(decider.canReply(sbn))
    }

    @Test
    fun `canReply returns false for group message when group reply disabled`() {
        whenever(prefs.isServiceEnabled()).thenReturn(true)
        whenever(prefs.getEnabledApps()).thenReturn(setOf("com.whatsapp"))
        whenever(prefs.isGroupReplyEnabled()).thenReturn(false)
        whenever(prefs.getAutoReplyDelay()).thenReturn(0L)
        whenever(dbUtils.getLastRepliedTime(any(), any())).thenReturn(0L)
        val sbn = buildMockSbn(isGroup = true)
        assertFalse(decider.canReply(sbn))
    }

    @Test
    fun `canReply returns false when cooldown not elapsed`() {
        whenever(prefs.isServiceEnabled()).thenReturn(true)
        whenever(prefs.getEnabledApps()).thenReturn(setOf("com.whatsapp"))
        whenever(prefs.isGroupReplyEnabled()).thenReturn(false)
        whenever(prefs.getAutoReplyDelay()).thenReturn(60 * 1000L) // 1 minute cooldown
        // Last reply was just now
        whenever(dbUtils.getLastRepliedTime(eq("com.whatsapp"), eq("John"))).thenReturn(System.currentTimeMillis())
        val sbn = buildMockSbn()
        assertFalse(decider.canReply(sbn))
    }

    @Test
    fun `canReply returns false when title matches selfDisplayName`() {
        whenever(prefs.isServiceEnabled()).thenReturn(true)
        whenever(prefs.getEnabledApps()).thenReturn(setOf("com.whatsapp"))
        whenever(prefs.isGroupReplyEnabled()).thenReturn(false)
        whenever(prefs.getAutoReplyDelay()).thenReturn(0L)
        val sbn = buildMockSbn(title = "Me", selfDisplayName = "Me")
        assertFalse(decider.canReply(sbn))
    }

    @Test
    fun `canReply returns true for valid WhatsApp notification`() {
        setupDefaultPrefs()
        whenever(dbUtils.getLastRepliedTime(any(), any())).thenReturn(0L)
        val sbn = buildMockSbn()
        assertTrue(decider.canReply(sbn))
    }

    @Test
    fun `canReply returns true for group message when group reply enabled`() {
        setupDefaultPrefs()
        whenever(prefs.isGroupReplyEnabled()).thenReturn(true)
        whenever(dbUtils.getLastRepliedTime(any(), any())).thenReturn(0L)
        val sbn = buildMockSbn(isGroup = true)
        assertTrue(decider.canReply(sbn))
    }

    // ========== shouldReply tests ==========

    @Test
    fun `shouldReply returns true when contact filter is disabled`() {
        whenever(prefs.isContactReplyEnabled()).thenReturn(false)
        val sbn = buildMockSbn()
        assertTrue(decider.shouldReply(sbn))
    }

    @Test
    fun `shouldReply returns false in whitelist mode when sender not in list`() {
        whenever(prefs.isContactReplyEnabled()).thenReturn(true)
        whenever(prefs.isContactReplyBlacklistMode()).thenReturn(false)
        whenever(prefs.getReplyToNames()).thenReturn(emptySet())
        whenever(prefs.getCustomReplyNames()).thenReturn(emptySet())
        whenever(contactsHelper.hasContactPermission()).thenReturn(true)
        val sbn = buildMockSbn(title = "Unknown Person")
        assertFalse(decider.shouldReply(sbn))
    }

    @Test
    fun `shouldReply returns true in whitelist mode when sender is in list`() {
        whenever(prefs.isContactReplyEnabled()).thenReturn(true)
        whenever(prefs.isContactReplyBlacklistMode()).thenReturn(false)
        whenever(prefs.getReplyToNames()).thenReturn(setOf("John"))
        whenever(prefs.getCustomReplyNames()).thenReturn(emptySet())
        whenever(contactsHelper.hasContactPermission()).thenReturn(true)
        val sbn = buildMockSbn(title = "John")
        assertTrue(decider.shouldReply(sbn))
    }

    @Test
    fun `shouldReply returns false in blacklist mode when sender is in list`() {
        whenever(prefs.isContactReplyEnabled()).thenReturn(true)
        whenever(prefs.isContactReplyBlacklistMode()).thenReturn(true)
        whenever(prefs.getReplyToNames()).thenReturn(setOf("Spammer"))
        whenever(prefs.getCustomReplyNames()).thenReturn(emptySet())
        whenever(contactsHelper.hasContactPermission()).thenReturn(true)
        val sbn = buildMockSbn(title = "Spammer")
        assertFalse(decider.shouldReply(sbn))
    }

    @Test
    fun `shouldReply returns true in blacklist mode when sender not in list`() {
        whenever(prefs.isContactReplyEnabled()).thenReturn(true)
        whenever(prefs.isContactReplyBlacklistMode()).thenReturn(true)
        whenever(prefs.getReplyToNames()).thenReturn(setOf("Spammer"))
        whenever(prefs.getCustomReplyNames()).thenReturn(emptySet())
        whenever(contactsHelper.hasContactPermission()).thenReturn(true)
        val sbn = buildMockSbn(title = "Friend")
        assertTrue(decider.shouldReply(sbn))
    }

    @Test
    fun `shouldReply returns true for group messages regardless of contact filter`() {
        whenever(prefs.isContactReplyEnabled()).thenReturn(true)
        whenever(prefs.isContactReplyBlacklistMode()).thenReturn(false)
        whenever(prefs.getReplyToNames()).thenReturn(emptySet())
        whenever(prefs.getCustomReplyNames()).thenReturn(emptySet())
        whenever(contactsHelper.hasContactPermission()).thenReturn(true)
        val sbn = buildMockSbn(title = "Group Chat", isGroup = true)
        assertTrue(decider.shouldReply(sbn))
    }

    @Test
    fun `shouldReply returns true when custom reply name matches in whitelist mode`() {
        whenever(prefs.isContactReplyEnabled()).thenReturn(true)
        whenever(prefs.isContactReplyBlacklistMode()).thenReturn(false)
        whenever(prefs.getReplyToNames()).thenReturn(emptySet())
        whenever(prefs.getCustomReplyNames()).thenReturn(setOf("Custom Contact"))
        whenever(contactsHelper.hasContactPermission()).thenReturn(false)
        val sbn = buildMockSbn(title = "Custom Contact")
        assertTrue(decider.shouldReply(sbn))
    }

    @Test
    fun `shouldReply returns true when contact permission matches name in whitelist`() {
        whenever(prefs.isContactReplyEnabled()).thenReturn(true)
        whenever(prefs.isContactReplyBlacklistMode()).thenReturn(false)
        whenever(prefs.getReplyToNames()).thenReturn(setOf("John"))
        whenever(prefs.getCustomReplyNames()).thenReturn(emptySet())
        whenever(contactsHelper.hasContactPermission()).thenReturn(true)
        val sbn = buildMockSbn(title = "John")
        assertTrue(decider.shouldReply(sbn))
    }

    // ========== isSupportedPackage tests ==========

    @Test
    fun `isSupportedPackage returns true for WhatsApp`() {
        whenever(prefs.getEnabledApps()).thenReturn(setOf("com.whatsapp"))
        val sbn = buildMockSbn(packageName = "com.whatsapp")
        assertTrue(decider.isSupportedPackage(sbn))
    }

    @Test
    fun `isSupportedPackage returns false for unknown package`() {
        whenever(prefs.getEnabledApps()).thenReturn(setOf("com.whatsapp"))
        val sbn = buildMockSbn(packageName = "com.random.app")
        assertFalse(decider.isSupportedPackage(sbn))
    }

    @Test
    fun `isSupportedPackage returns true for multiple enabled apps`() {
        whenever(prefs.getEnabledApps()).thenReturn(setOf("com.whatsapp", "org.telegram.messenger"))
        val sbn = buildMockSbn(packageName = "org.telegram.messenger")
        assertTrue(decider.isSupportedPackage(sbn))
    }

    // ========== canSendReplyNow tests ==========

    @Test
    fun `canSendReplyNow returns false within minimum cooldown`() {
        whenever(prefs.getAutoReplyDelay()).thenReturn(0L)
        // Last replied 5 seconds ago (within 10s min delay)
        whenever(dbUtils.getLastRepliedTime(eq("com.whatsapp"), eq("John")))
            .thenReturn(System.currentTimeMillis() - 5000)
        val sbn = buildMockSbn()
        assertFalse(decider.canSendReplyNow(sbn))
    }

    @Test
    fun `canSendReplyNow returns true after cooldown elapsed`() {
        whenever(prefs.getAutoReplyDelay()).thenReturn(0L)
        // Last replied 15 seconds ago (beyond 10s min delay)
        whenever(dbUtils.getLastRepliedTime(eq("com.whatsapp"), eq("John")))
            .thenReturn(System.currentTimeMillis() - 15000)
        val sbn = buildMockSbn()
        assertTrue(decider.canSendReplyNow(sbn))
    }

    @Test
    fun `canSendReplyNow returns false when title matches selfDisplayName`() {
        whenever(prefs.getAutoReplyDelay()).thenReturn(0L)
        val sbn = buildMockSbn(title = "Me", selfDisplayName = "Me")
        assertFalse(decider.canSendReplyNow(sbn))
    }

    @Test
    fun `canSendReplyNow uses custom delay from preferences`() {
        whenever(prefs.getAutoReplyDelay()).thenReturn(60 * 1000L) // 1 minute
        // Last replied 30 seconds ago (within 1-minute custom delay)
        whenever(dbUtils.getLastRepliedTime(eq("com.whatsapp"), eq("John")))
            .thenReturn(System.currentTimeMillis() - 30000)
        val sbn = buildMockSbn()
        assertFalse(decider.canSendReplyNow(sbn))
    }

    // ========== isGroupMessageAndReplyAllowed tests ==========

    @Test
    fun `isGroupMessageAndReplyAllowed returns true for non-group message`() {
        val sbn = buildMockSbn(isGroup = false, title = "John", text = "Hello")
        assertTrue(decider.isGroupMessageAndReplyAllowed(sbn))
    }

    @Test
    fun `isGroupMessageAndReplyAllowed returns true for group when enabled`() {
        whenever(prefs.isGroupReplyEnabled()).thenReturn(true)
        val sbn = buildMockSbn(isGroup = true, title = "Group Chat")
        assertTrue(decider.isGroupMessageAndReplyAllowed(sbn))
    }

    @Test
    fun `isGroupMessageAndReplyAllowed returns false for group when disabled`() {
        whenever(prefs.isGroupReplyEnabled()).thenReturn(false)
        val sbn = buildMockSbn(isGroup = true, title = "Group Chat")
        assertFalse(decider.isGroupMessageAndReplyAllowed(sbn))
    }

    // ========== isServiceEnabled tests ==========

    @Test
    fun `isServiceEnabled delegates to preferences`() {
        whenever(prefs.isServiceEnabled()).thenReturn(true)
        assertTrue(decider.isServiceEnabled())

        whenever(prefs.isServiceEnabled()).thenReturn(false)
        assertFalse(decider.isServiceEnabled())
    }
}
