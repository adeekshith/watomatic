package com.parishod.watomatic.service

import android.app.Notification
import android.os.Bundle
import android.os.Process
import android.os.UserHandle
import android.service.notification.StatusBarNotification
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.platform.app.InstrumentationRegistry
import com.parishod.watomatic.model.CustomRepliesData
import com.parishod.watomatic.model.logs.AppPackage
import com.parishod.watomatic.model.logs.MessageLog
import com.parishod.watomatic.model.logs.MessageLogsDB
import com.parishod.watomatic.model.preferences.PreferencesManager
import com.parishod.watomatic.model.utils.ContactsHelper
import com.parishod.watomatic.model.utils.DbUtils
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Integration tests for [NotificationReplyDecider] using real Android components:
 * real SharedPreferences, real Room database, real ContactsHelper.
 *
 * Uses real [StatusBarNotification] objects (no mocking) to test the full
 * notification reply decision pipeline on a device/emulator.
 */
@RunWith(AndroidJUnit4::class)
@MediumTest
class NotificationReplyDeciderInstrumentedTest {

    private lateinit var prefs: PreferencesManager
    private lateinit var dbUtils: DbUtils
    private lateinit var contactsHelper: ContactsHelper
    private lateinit var decider: NotificationReplyDecider
    private lateinit var db: MessageLogsDB

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Clear SharedPreferences
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        PreferencesManager.resetInstance()
        CustomRepliesData.resetInstance()

        prefs = PreferencesManager.getPreferencesInstance(context)
        dbUtils = DbUtils(context)
        contactsHelper = ContactsHelper.getInstance(context)
        db = MessageLogsDB.getInstance(context)

        // Clear the message logs DB
        db.logsDao().purgeMessageLogs()

        decider = NotificationReplyDecider(prefs, dbUtils, contactsHelper)
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        PreferencesManager.resetInstance()
        CustomRepliesData.resetInstance()
    }

    // --- Build a real StatusBarNotification (no mocking required) ---

    private fun buildSbn(
        packageName: String = "com.whatsapp",
        title: String? = "John",
        text: String? = "Hello there!",
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
        val notification = Notification.Builder(
            InstrumentationRegistry.getInstrumentation().targetContext,
            "test_channel"
        ).setSmallIcon(android.R.drawable.ic_dialog_info)
            .setExtras(extras)
            .setWhen(notificationTime)
            .build()

        return StatusBarNotification(
            packageName,                    // pkg
            packageName,                    // opPkg
            1,                              // id
            null,                           // tag
            Process.myUid(),                // uid
            0,                              // initialPid
            0,                              // score
            notification,                   // notification
            UserHandle.getUserHandleForUid(Process.myUid()), // user
            notificationTime                // postTime
        )
    }

    // ========== Full pipeline integration tests ==========

    @Test
    fun serviceDisabledByDefault_canReplyReturnsFalse() {
        val sbn = buildSbn()
        assertFalse(decider.canReply(sbn))
    }

    @Test
    fun serviceEnabled_whatsAppEnabled_canReplyReturnsTrue() {
        prefs.setServicePref(true)
        prefs.saveEnabledApps("com.whatsapp", true)

        val sbn = buildSbn()
        assertTrue(decider.canReply(sbn))
    }

    @Test
    fun serviceEnabled_unsupportedApp_canReplyReturnsFalse() {
        prefs.setServicePref(true)
        prefs.saveEnabledApps("com.whatsapp", true)

        val sbn = buildSbn(packageName = "com.facebook.orca")
        assertFalse(decider.canReply(sbn))
    }

    @Test
    fun oldNotification_canReplyReturnsFalse() {
        prefs.setServicePref(true)
        prefs.saveEnabledApps("com.whatsapp", true)

        val oldTime = System.currentTimeMillis() - 5 * 60 * 1000
        val sbn = buildSbn(notificationTime = oldTime)
        assertFalse(decider.canReply(sbn))
    }

    @Test
    fun groupMessage_groupReplyDisabled_canReplyReturnsFalse() {
        prefs.setServicePref(true)
        prefs.saveEnabledApps("com.whatsapp", true)
        prefs.setGroupReplyPref(false)

        val sbn = buildSbn(isGroup = true, title = "Family Group")
        assertFalse(decider.canReply(sbn))
    }

    @Test
    fun groupMessage_groupReplyEnabled_canReplyReturnsTrue() {
        prefs.setServicePref(true)
        prefs.saveEnabledApps("com.whatsapp", true)
        prefs.setGroupReplyPref(true)

        val sbn = buildSbn(isGroup = true, title = "Family Group")
        assertTrue(decider.canReply(sbn))
    }

    @Test
    fun selfDisplayNameMatchesTitle_canReplyReturnsFalse() {
        prefs.setServicePref(true)
        prefs.saveEnabledApps("com.whatsapp", true)

        val sbn = buildSbn(title = "Me", selfDisplayName = "Me")
        assertFalse(decider.canReply(sbn))
    }

    // ========== Cooldown with real DB ==========

    @Test
    fun recentReplyInDb_cooldownBlocksReply() {
        prefs.setServicePref(true)
        prefs.saveEnabledApps("com.whatsapp", true)

        val appPackage = AppPackage("com.whatsapp")
        db.appPackageDao().insertAppPackage(appPackage)
        val packageIndex = db.appPackageDao().getPackageIndex("com.whatsapp")
        val recentLog = MessageLog(
            packageIndex, "John", System.currentTimeMillis(),
            "Auto reply", System.currentTimeMillis()
        )
        db.logsDao().logReply(recentLog)

        val sbn = buildSbn(title = "John")
        assertFalse(decider.canSendReplyNow(sbn))
    }

    @Test
    fun noReplyInDb_cooldownAllowsReply() {
        prefs.setServicePref(true)
        prefs.saveEnabledApps("com.whatsapp", true)

        val sbn = buildSbn(title = "NewPerson")
        assertTrue(decider.canSendReplyNow(sbn))
    }

    @Test
    fun customCooldownDelay_blocksWithinWindow() {
        prefs.setServicePref(true)
        prefs.saveEnabledApps("com.whatsapp", true)
        prefs.setAutoReplyDelay(5 * 60 * 1000L)

        val appPackage = AppPackage("com.whatsapp")
        db.appPackageDao().insertAppPackage(appPackage)
        val packageIndex = db.appPackageDao().getPackageIndex("com.whatsapp")
        val recentLog = MessageLog(
            packageIndex, "John", System.currentTimeMillis(),
            "Auto reply", System.currentTimeMillis() - 60 * 1000
        )
        db.logsDao().logReply(recentLog)

        val sbn = buildSbn(title = "John")
        assertFalse(decider.canSendReplyNow(sbn))
    }

    // ========== Contact filter with real prefs ==========

    @Test
    fun contactFilterDisabled_shouldReplyReturnsTrue() {
        prefs.setContactReplyEnabled(false)
        val sbn = buildSbn(title = "Anyone")
        assertTrue(decider.shouldReply(sbn))
    }

    @Test
    fun whitelistMode_senderNotInList_shouldReplyReturnsFalse() {
        prefs.setContactReplyEnabled(true)
        prefs.setContactReplyBlacklistMode(false)

        val sbn = buildSbn(title = "Unknown Person")
        assertFalse(decider.shouldReply(sbn))
    }

    @Test
    fun whitelistMode_customContactInList_shouldReplyReturnsTrue() {
        prefs.setContactReplyEnabled(true)
        prefs.setContactReplyBlacklistMode(false)
        prefs.setCustomReplyNames(setOf("VIP Customer"))

        val sbn = buildSbn(title = "VIP Customer")
        assertTrue(decider.shouldReply(sbn))
    }

    @Test
    fun blacklistMode_senderInList_shouldReplyReturnsFalse() {
        prefs.setContactReplyEnabled(true)
        prefs.setContactReplyBlacklistMode(true)
        prefs.setCustomReplyNames(setOf("Spammer"))

        val sbn = buildSbn(title = "Spammer")
        assertFalse(decider.shouldReply(sbn))
    }

    @Test
    fun blacklistMode_senderNotInList_shouldReplyReturnsTrue() {
        prefs.setContactReplyEnabled(true)
        prefs.setContactReplyBlacklistMode(true)
        prefs.setCustomReplyNames(setOf("Spammer"))

        val sbn = buildSbn(title = "Friend")
        assertTrue(decider.shouldReply(sbn))
    }

    @Test
    fun groupMessage_bypassesContactFilter() {
        prefs.setContactReplyEnabled(true)
        prefs.setContactReplyBlacklistMode(false)

        val sbn = buildSbn(title = "Group Chat", isGroup = true)
        assertTrue(decider.shouldReply(sbn))
    }

    // ========== Full canReply + shouldReply pipeline ==========

    @Test
    fun fullPipeline_enabledWhatsApp_noFilters_repliesSuccessfully() {
        prefs.setServicePref(true)
        prefs.saveEnabledApps("com.whatsapp", true)
        prefs.setContactReplyEnabled(false)

        val sbn = buildSbn(title = "Alice", text = "Hey, are you free?")
        assertTrue("canReply should pass", decider.canReply(sbn))
        assertTrue("shouldReply should pass", decider.shouldReply(sbn))
    }

    @Test
    fun fullPipeline_enabledWhatsApp_whitelistBlocks() {
        prefs.setServicePref(true)
        prefs.saveEnabledApps("com.whatsapp", true)
        prefs.setContactReplyEnabled(true)
        prefs.setContactReplyBlacklistMode(false)
        prefs.setCustomReplyNames(setOf("Boss"))

        val sbnAllowed = buildSbn(title = "Boss", text = "Meeting at 3?")
        assertTrue("canReply should pass for Boss", decider.canReply(sbnAllowed))
        assertTrue("shouldReply should pass for Boss", decider.shouldReply(sbnAllowed))

        val sbnBlocked = buildSbn(title = "Random", text = "Hi")
        assertTrue("canReply should pass for Random", decider.canReply(sbnBlocked))
        assertFalse("shouldReply should block Random", decider.shouldReply(sbnBlocked))
    }

    @Test
    fun fullPipeline_multipleAppsEnabled() {
        prefs.setServicePref(true)
        prefs.saveEnabledApps("com.whatsapp", true)
        prefs.saveEnabledApps("org.telegram.messenger", true)

        val whatsappSbn = buildSbn(packageName = "com.whatsapp", title = "Alice")
        val telegramSbn = buildSbn(packageName = "org.telegram.messenger", title = "Bob")
        val signalSbn = buildSbn(packageName = "org.thoughtcrime.securesms", title = "Carol")

        assertTrue("WhatsApp should be supported", decider.isSupportedPackage(whatsappSbn))
        assertTrue("Telegram should be supported", decider.isSupportedPackage(telegramSbn))
        assertFalse("Signal should not be supported", decider.isSupportedPackage(signalSbn))
    }
}
