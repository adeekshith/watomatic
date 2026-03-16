package com.parishod.watomatic.model.preferences

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class PreferencesManagerTest {

    private lateinit var context: Context
    private lateinit var prefs: PreferencesManager

    @Before
    fun setUp() {
        PreferencesManager.resetInstance()
        context = ApplicationProvider.getApplicationContext()
        // Clear default shared prefs to get a truly fresh state each test
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        prefs = PreferencesManager.getPreferencesInstance(context)
    }

    @After
    fun tearDown() {
        PreferencesManager.resetInstance()
    }

    // --- Service enabled ---

    @Test
    fun `isServiceEnabled defaults to false`() {
        assertFalse(prefs.isServiceEnabled)
    }

    @Test
    fun `setServicePref to true enables service`() {
        prefs.setServicePref(true)
        assertTrue(prefs.isServiceEnabled)
    }

    @Test
    fun `setServicePref to false disables service`() {
        prefs.setServicePref(true)
        prefs.setServicePref(false)
        assertFalse(prefs.isServiceEnabled)
    }

    // --- Group reply ---

    @Test
    fun `isGroupReplyEnabled defaults to false`() {
        assertFalse(prefs.isGroupReplyEnabled)
    }

    @Test
    fun `setGroupReplyPref to true enables group reply`() {
        prefs.setGroupReplyPref(true)
        assertTrue(prefs.isGroupReplyEnabled)
    }

    @Test
    fun `setGroupReplyPref to false disables group reply`() {
        prefs.setGroupReplyPref(true)
        prefs.setGroupReplyPref(false)
        assertFalse(prefs.isGroupReplyEnabled)
    }

    // --- Auto reply delay ---

    @Test
    fun `getAutoReplyDelay defaults to 0`() {
        assertEquals(0L, prefs.autoReplyDelay)
    }

    @Test
    fun `setAutoReplyDelay persists the value`() {
        prefs.setAutoReplyDelay(5000L)
        assertEquals(5000L, prefs.autoReplyDelay)
    }

    @Test
    fun `setAutoReplyDelay stores zero correctly`() {
        prefs.setAutoReplyDelay(5000L)
        prefs.setAutoReplyDelay(0L)
        assertEquals(0L, prefs.autoReplyDelay)
    }

    // --- Watomatic attribution ---

    @Test
    fun `isAppendWatomaticAttributionEnabled defaults to false`() {
        assertFalse(prefs.isAppendWatomaticAttributionEnabled)
    }

    @Test
    fun `setAppendWatomaticAttribution to true enables it`() {
        prefs.setAppendWatomaticAttribution(true)
        assertTrue(prefs.isAppendWatomaticAttributionEnabled)
    }

    // --- Contact reply ---

    @Test
    fun `isContactReplyEnabled defaults to false`() {
        assertFalse(prefs.isContactReplyEnabled)
    }

    @Test
    fun `setContactReplyEnabled persists value`() {
        prefs.setContactReplyEnabled(true)
        assertTrue(prefs.isContactReplyEnabled)
    }

    // --- Contact reply mode (blacklist vs whitelist) ---

    @Test
    fun `isContactReplyBlacklistMode defaults to true`() {
        assertTrue(prefs.isContactReplyBlacklistMode)
    }

    @Test
    fun `setContactReplyBlacklistMode false switches to whitelist mode`() {
        prefs.setContactReplyBlacklistMode(false)
        assertFalse(prefs.isContactReplyBlacklistMode)
    }

    @Test
    fun `setContactReplyBlacklistMode true restores blacklist mode`() {
        prefs.setContactReplyBlacklistMode(false)
        prefs.setContactReplyBlacklistMode(true)
        assertTrue(prefs.isContactReplyBlacklistMode)
    }

    // --- AI replies ---

    @Test
    fun `isAutomaticAiRepliesEnabled defaults to false`() {
        assertFalse(prefs.isAutomaticAiRepliesEnabled)
    }

    @Test
    fun `setEnableAutomaticAiReplies enables automatic AI`() {
        prefs.setEnableAutomaticAiReplies(true)
        assertTrue(prefs.isAutomaticAiRepliesEnabled)
    }

    @Test
    fun `setEnableAutomaticAiReplies disables automatic AI`() {
        prefs.setEnableAutomaticAiReplies(true)
        prefs.setEnableAutomaticAiReplies(false)
        assertFalse(prefs.isAutomaticAiRepliesEnabled)
    }

    @Test
    fun `isByokRepliesEnabled defaults to false`() {
        assertFalse(prefs.isByokRepliesEnabled)
    }

    @Test
    fun `setEnableByokReplies enables BYOK`() {
        prefs.setEnableByokReplies(true)
        assertTrue(prefs.isByokRepliesEnabled)
    }

    @Test
    fun `isAnyAiRepliesEnabled returns false when both disabled`() {
        assertFalse(prefs.isAnyAiRepliesEnabled)
    }

    @Test
    fun `isAnyAiRepliesEnabled returns true when automatic AI enabled`() {
        prefs.setEnableAutomaticAiReplies(true)
        assertTrue(prefs.isAnyAiRepliesEnabled)
    }

    @Test
    fun `isAnyAiRepliesEnabled returns true when BYOK enabled`() {
        prefs.setEnableByokReplies(true)
        assertTrue(prefs.isAnyAiRepliesEnabled)
    }

    @Test
    fun `isAnyAiRepliesEnabled returns true when both enabled`() {
        prefs.setEnableAutomaticAiReplies(true)
        prefs.setEnableByokReplies(true)
        assertTrue(prefs.isAnyAiRepliesEnabled)
    }

    // --- Login state ---

    @Test
    fun `isLoggedIn defaults to false`() {
        assertFalse(prefs.isLoggedIn)
    }

    @Test
    fun `setLoggedIn to true persists`() {
        prefs.setLoggedIn(true)
        assertTrue(prefs.isLoggedIn)
    }

    @Test
    fun `setLoggedIn to false persists`() {
        prefs.setLoggedIn(true)
        prefs.setLoggedIn(false)
        assertFalse(prefs.isLoggedIn)
    }

    // --- Guest mode ---

    @Test
    fun `isGuestMode defaults to false`() {
        assertFalse(prefs.isGuestMode)
    }

    @Test
    fun `setGuestMode to true persists`() {
        prefs.setGuestMode(true)
        assertTrue(prefs.isGuestMode)
    }

    // --- shouldShowLogin ---

    @Test
    fun `shouldShowLogin returns true when not logged in and not in guest mode`() {
        assertTrue(prefs.shouldShowLogin())
    }

    @Test
    fun `shouldShowLogin returns false when logged in`() {
        prefs.setLoggedIn(true)
        assertFalse(prefs.shouldShowLogin())
    }

    @Test
    fun `shouldShowLogin returns false in guest mode`() {
        prefs.setGuestMode(true)
        assertFalse(prefs.shouldShowLogin())
    }

    // --- Subscription ---

    @Test
    fun `isSubscriptionActive defaults to false`() {
        assertFalse(prefs.isSubscriptionActive)
    }

    @Test
    fun `isSubscriptionActive returns true when active with zero expiry time`() {
        prefs.setSubscriptionActive(true)
        // expiry = 0 means lifetime/unknown → considered active
        assertTrue(prefs.isSubscriptionActive)
    }

    @Test
    fun `isSubscriptionActive returns true when active and not yet expired`() {
        prefs.setSubscriptionActive(true)
        prefs.setSubscriptionExpiryTime(System.currentTimeMillis() + 60_000L) // expires in 1 min
        assertTrue(prefs.isSubscriptionActive)
    }

    @Test
    fun `isSubscriptionActive returns false when active but already expired`() {
        prefs.setSubscriptionActive(true)
        prefs.setSubscriptionExpiryTime(System.currentTimeMillis() - 1_000L) // expired 1 sec ago
        assertFalse(prefs.isSubscriptionActive)
    }

    @Test
    fun `isSubscriptionActive returns false when inactive regardless of expiry`() {
        prefs.setSubscriptionActive(false)
        prefs.setSubscriptionExpiryTime(System.currentTimeMillis() + 60_000L)
        assertFalse(prefs.isSubscriptionActive)
    }

    @Test
    fun `getSubscriptionExpiryTime defaults to 0`() {
        assertEquals(0L, prefs.subscriptionExpiryTime)
    }

    @Test
    fun `setSubscriptionExpiryTime persists value`() {
        val time = System.currentTimeMillis() + 86_400_000L
        prefs.setSubscriptionExpiryTime(time)
        assertEquals(time, prefs.subscriptionExpiryTime)
    }

    @Test
    fun `getSubscriptionProductId defaults to empty string`() {
        assertEquals("", prefs.subscriptionProductId)
    }

    @Test
    fun `setSubscriptionProductId persists value`() {
        prefs.setSubscriptionProductId("pro_monthly")
        assertEquals("pro_monthly", prefs.subscriptionProductId)
    }

    @Test
    fun `isSubscriptionAutoRenewing defaults to false`() {
        assertFalse(prefs.isSubscriptionAutoRenewing)
    }

    @Test
    fun `setSubscriptionAutoRenewing persists value`() {
        prefs.setSubscriptionAutoRenewing(true)
        assertTrue(prefs.isSubscriptionAutoRenewing)
    }

    // --- Remaining atoms ---

    @Test
    fun `getRemainingAtoms defaults to minus one`() {
        assertEquals(-1, prefs.getRemainingAtoms())
    }

    @Test
    fun `setRemainingAtoms persists value`() {
        prefs.setRemainingAtoms(100)
        assertEquals(100, prefs.getRemainingAtoms())
    }

    @Test
    fun `setRemainingAtoms persists zero`() {
        prefs.setRemainingAtoms(0)
        assertEquals(0, prefs.getRemainingAtoms())
    }

    // --- Quota notification ---

    @Test
    fun `getQuotaNotificationLastShown defaults to 0`() {
        assertEquals(0L, prefs.getQuotaNotificationLastShown())
    }

    @Test
    fun `setQuotaNotificationLastShown persists value`() {
        val time = System.currentTimeMillis()
        prefs.setQuotaNotificationLastShown(time)
        assertEquals(time, prefs.getQuotaNotificationLastShown())
    }

    // --- Firebase token ---

    @Test
    fun `getFirebaseToken defaults to empty string`() {
        assertEquals("", prefs.firebaseToken)
    }

    @Test
    fun `setFirebaseToken persists value`() {
        prefs.setFirebaseToken("test-firebase-token-123")
        assertEquals("test-firebase-token-123", prefs.firebaseToken)
    }

    // --- Fallback message ---

    @Test
    fun `getFallbackMessage defaults to empty string`() {
        assertEquals("", prefs.fallbackMessage)
    }

    @Test
    fun `saveFallbackMessage persists value`() {
        prefs.saveFallbackMessage("Sorry, I'm away right now")
        assertEquals("Sorry, I'm away right now", prefs.fallbackMessage)
    }

    // --- AI custom prompts ---

    @Test
    fun `getOpenAICustomPrompt returns null by default`() {
        assertNull(prefs.openAICustomPrompt)
    }

    @Test
    fun `saveOpenAICustomPrompt persists value`() {
        prefs.saveOpenAICustomPrompt("Reply concisely")
        assertEquals("Reply concisely", prefs.openAICustomPrompt)
    }

    @Test
    fun `getAtomaticAICustomPrompt defaults to empty string`() {
        assertEquals("", prefs.atomaticAICustomPrompt)
    }

    @Test
    fun `saveAtomaticAICustomPrompt persists value`() {
        prefs.saveAtomaticAICustomPrompt("Be brief and friendly")
        assertEquals("Be brief and friendly", prefs.atomaticAICustomPrompt)
    }

    // --- AI provider source ---

    @Test
    fun `getOpenApiSource defaults to openai`() {
        assertEquals("openai", prefs.openApiSource)
    }

    @Test
    fun `saveOpenApiSource persists value`() {
        prefs.saveOpenApiSource("Claude")
        assertEquals("Claude", prefs.openApiSource)
    }

    // --- Custom OpenAI URL ---

    @Test
    fun `getCustomOpenAIApiUrl defaults to null`() {
        assertNull(prefs.customOpenAIApiUrl)
    }

    @Test
    fun `saveCustomOpenAIApiUrl persists value`() {
        prefs.saveCustomOpenAIApiUrl("https://my-api.example.com/")
        assertEquals("https://my-api.example.com/", prefs.customOpenAIApiUrl)
    }

    // --- OpenAI model ---

    @Test
    fun `getSelectedOpenAIModel defaults to null`() {
        assertNull(prefs.selectedOpenAIModel)
    }

    @Test
    fun `saveSelectedOpenAIModel persists value`() {
        prefs.saveSelectedOpenAIModel("gpt-4o")
        assertEquals("gpt-4o", prefs.selectedOpenAIModel)
    }

    // --- User email ---

    @Test
    fun `getUserEmail defaults to empty string`() {
        assertEquals("", prefs.userEmail)
    }

    @Test
    fun `setUserEmail persists value`() {
        prefs.setUserEmail("user@example.com")
        assertEquals("user@example.com", prefs.userEmail)
    }

    // --- Locale parsing ---

    @Test
    fun `getSelectedLocale returns device default when no language set`() {
        val locale = prefs.selectedLocale
        assertNotNull(locale)
        assertEquals(Locale.getDefault(), locale)
    }

    @Test
    fun `getSelectedLocale parses language-region format correctly`() {
        prefs.setLanguageStr("en-US")
        val locale = prefs.selectedLocale
        assertEquals("en", locale.language)
        assertEquals("US", locale.country)
    }

    @Test
    fun `getSelectedLocale parses language-only format correctly`() {
        prefs.setLanguageStr("de")
        val locale = prefs.selectedLocale
        assertEquals("de", locale.language)
    }

    @Test
    fun `getSelectedLocale parses Chinese simplified correctly`() {
        prefs.setLanguageStr("zh-CN")
        val locale = prefs.selectedLocale
        assertEquals("zh", locale.language)
        assertEquals("CN", locale.country)
    }

    // --- Legacy language key migration ---

    @Test
    fun `updateLegacyLanguageKey migrates old format with r prefix`() {
        prefs.setLanguageStr("zh-rCN")
        prefs.updateLegacyLanguageKey()
        assertEquals("zh-CN", prefs.getSelectedLanguageStr(null))
    }

    @Test
    fun `updateLegacyLanguageKey does not change modern language-country format`() {
        prefs.setLanguageStr("zh-CN")
        prefs.updateLegacyLanguageKey()
        assertEquals("zh-CN", prefs.getSelectedLanguageStr(null))
    }

    @Test
    fun `updateLegacyLanguageKey does nothing when no language set`() {
        // Should not throw when called with no language stored
        prefs.updateLegacyLanguageKey()
        assertNull(prefs.getSelectedLanguageStr(null))
    }

    @Test
    fun `updateLegacyLanguageKey does not change language-only value`() {
        prefs.setLanguageStr("de")
        prefs.updateLegacyLanguageKey()
        assertEquals("de", prefs.getSelectedLanguageStr(null))
    }

    // --- Persistent AI errors ---

    @Test
    fun `getOpenAILastPersistentErrorMessage returns null by default`() {
        assertNull(prefs.openAILastPersistentErrorMessage)
    }

    @Test
    fun `saveOpenAILastPersistentError persists message and timestamp`() {
        val ts = System.currentTimeMillis()
        prefs.saveOpenAILastPersistentError("Rate limit exceeded", ts)
        assertEquals("Rate limit exceeded", prefs.openAILastPersistentErrorMessage)
        assertEquals(ts, prefs.openAILastPersistentErrorTimestamp)
    }

    @Test
    fun `clearOpenAILastPersistentError removes both message and timestamp`() {
        prefs.saveOpenAILastPersistentError("Some error", System.currentTimeMillis())
        prefs.clearOpenAILastPersistentError()
        assertNull(prefs.openAILastPersistentErrorMessage)
        assertEquals(0L, prefs.openAILastPersistentErrorTimestamp)
    }

    // --- Last verified time ---

    @Test
    fun `getLastVerifiedTime defaults to 0`() {
        assertEquals(0L, prefs.lastVerifiedTime)
    }

    @Test
    fun `setLastVerifiedTime persists value`() {
        val time = System.currentTimeMillis()
        prefs.setLastVerifiedTime(time)
        assertEquals(time, prefs.lastVerifiedTime)
    }

    // --- Subscription plan ---

    @Test
    fun `getSubscriptionPlanType defaults to empty string`() {
        assertEquals("", prefs.subscriptionPlanType)
    }

    @Test
    fun `setSubscriptionPlanType persists value`() {
        prefs.setSubscriptionPlanType("pro")
        assertEquals("pro", prefs.subscriptionPlanType)
    }
}
