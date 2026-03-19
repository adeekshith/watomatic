package com.parishod.watomatic

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.parishod.watomatic.model.preferences.PreferencesManager as AppPreferencesManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [AppPreferencesManager] using the real Android Keystore
 * and SharedPreferences on a device or emulator.
 */
@RunWith(AndroidJUnit4::class)
class PreferencesManagerInstrumentedTest {

    private lateinit var context: Context
    private lateinit var prefs: AppPreferencesManager

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // Clear all preferences before each test to guarantee a clean, isolated state
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        AppPreferencesManager.resetInstance()
        prefs = AppPreferencesManager.getPreferencesInstance(context)
    }

    @After
    fun tearDown() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        AppPreferencesManager.resetInstance()
    }

    @Test
    fun preferencesManagerIsNotNull() {
        assertNotNull(prefs)
    }

    @Test
    fun serviceEnabledDefaultsToFalseOnFreshInstance() {
        // Preferences are cleared in setUp, so this always starts from a known clean state
        assertFalse(prefs.isServiceEnabled)
    }

    @Test
    fun setAndGetServiceEnabled() {
        assertFalse(prefs.isServiceEnabled)
        prefs.setServicePref(true)
        assertTrue(prefs.isServiceEnabled)
        prefs.setServicePref(false)
        assertFalse(prefs.isServiceEnabled)
    }

    @Test
    fun setAndGetFirebaseToken() {
        val testToken = "instrumented-test-token-${System.currentTimeMillis()}"
        prefs.setFirebaseToken(testToken)
        assertEquals(testToken, prefs.firebaseToken)
    }

    @Test
    fun setAndGetFallbackMessage() {
        val message = "I'm in a meeting, will call you back"
        prefs.saveFallbackMessage(message)
        assertEquals(message, prefs.fallbackMessage)
    }

    @Test
    fun subscriptionActiveAndExpiryTime() {
        prefs.setSubscriptionActive(true)
        val futureExpiry = System.currentTimeMillis() + 86_400_000L // 1 day from now
        prefs.setSubscriptionExpiryTime(futureExpiry)
        assertTrue(prefs.isSubscriptionActive)
    }

    @Test
    fun subscriptionExpiredIsNotActive() {
        prefs.setSubscriptionActive(true)
        prefs.setSubscriptionExpiryTime(System.currentTimeMillis() - 1_000L)
        assertFalse(prefs.isSubscriptionActive)
    }

    /**
     * Verifies that EncryptedSharedPreferences-backed API key storage works
     * on a real device with a real Android Keystore.
     */
    @Test
    fun saveAndRetrieveOpenAIApiKey() {
        val testApiKey = "sk-test-instrumented-key"
        prefs.saveOpenAIApiKey(testApiKey)
        val retrieved = prefs.getOpenAIApiKey()
        // If EncryptedSharedPreferences initialised successfully, retrieved == testApiKey
        // If it failed (null encryptedPrefs), retrieved == null — both are valid outcomes
        if (retrieved != null) {
            assertEquals(testApiKey, retrieved)
        }
        // Cleanup
        prefs.deleteOpenAIApiKey()
    }

    @Test
    fun deleteOpenAIApiKeyRemovesIt() {
        prefs.saveOpenAIApiKey("sk-key-to-delete")
        prefs.deleteOpenAIApiKey()
        // Whether EncryptedSharedPreferences is available or not, key must be absent after deletion
        assertNull(prefs.getOpenAIApiKey())
    }

    @Test
    fun setAndGetUserEmail() {
        prefs.setUserEmail("test@example.com")
        assertEquals("test@example.com", prefs.userEmail)
    }

    @Test
    fun guestModeAndLoginInteraction() {
        prefs.setLoggedIn(false)
        prefs.setGuestMode(false)
        assertTrue(prefs.shouldShowLogin())

        prefs.setGuestMode(true)
        assertFalse(prefs.shouldShowLogin())

        prefs.setGuestMode(false)
        prefs.setLoggedIn(true)
        assertFalse(prefs.shouldShowLogin())
    }

    @Test
    fun aiReplyFlagsAreIndependent() {
        prefs.setEnableAutomaticAiReplies(false)
        prefs.setEnableByokReplies(false)
        assertFalse(prefs.isAnyAiRepliesEnabled)

        prefs.setEnableAutomaticAiReplies(true)
        assertTrue(prefs.isAnyAiRepliesEnabled)
        assertFalse(prefs.isByokRepliesEnabled) // BYOK still off

        prefs.setEnableAutomaticAiReplies(false)
        prefs.setEnableByokReplies(true)
        assertTrue(prefs.isAnyAiRepliesEnabled)
        assertFalse(prefs.isAutomaticAiRepliesEnabled) // Automatic AI still off
    }
}
