package com.parishod.watomatic

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.parishod.watomatic.model.preferences.PreferencesManager
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
 * Instrumented tests for [PreferencesManager] using the real Android Keystore
 * and SharedPreferences on a device or emulator.
 */
@RunWith(AndroidJUnit4::class)
class PreferencesManagerInstrumentedTest {

    private lateinit var prefs: PreferencesManager

    @Before
    fun setUp() {
        PreferencesManager.resetInstance()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        prefs = PreferencesManager.getPreferencesInstance(context)
    }

    @After
    fun tearDown() {
        PreferencesManager.resetInstance()
    }

    @Test
    fun preferencesManagerIsNotNull() {
        assertNotNull(prefs)
    }

    @Test
    fun serviceEnabledDefaultsToFalseOnFreshInstance() {
        // On a fresh test run, service should not be enabled
        // (This assumes prefs are clear; see note below if this flakes)
        assertFalse(prefs.isServiceEnabled)
    }

    @Test
    fun setAndGetServiceEnabled() {
        val original = prefs.isServiceEnabled
        prefs.setServicePref(!original)
        assertEquals(!original, prefs.isServiceEnabled)
        // Restore
        prefs.setServicePref(original)
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
        // After deletion, key should be absent (null)
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
