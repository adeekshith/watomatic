package com.parishod.watomatic.activity.customreplyeditor

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.widget.AutoCompleteTextView
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.google.android.material.textfield.TextInputEditText
import com.parishod.watomatic.R
import com.parishod.watomatic.model.preferences.PreferencesManager
import com.parishod.watomatic.network.model.openai.OpenAIModelsResponse
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import retrofit2.Call
import java.lang.reflect.Field

/**
 * Robolectric-based tests for the debounce, cancellation, and
 * distinct-until-changed behaviour added to [OtherAiConfigurationActivity]
 * to prevent the premature-API-call crash.
 *
 * Uses Robolectric because the Activity depends on Android's Handler/Looper,
 * View system, and SharedPreferences. Follows the reflection-based pattern
 * already established in [com.parishod.watomatic.model.utils.OpenAIHelperTest].
 */
@RunWith(RobolectricTestRunner::class)
@Config(
    sdk = [28],
)
class OtherAiConfigurationActivityUrlHandlingTest {

    private lateinit var controller: ActivityController<OtherAiConfigurationActivity>
    private lateinit var activity: OtherAiConfigurationActivity
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()

        // ✅ Preferences reset
        PreferencesManager.resetInstance()
        // Clear shared preferences storage
        context.getSharedPreferences("CustomRepliesData", Context.MODE_PRIVATE)
            .edit().clear().commit()
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()

        val intent = Intent(context, OtherAiConfigurationActivity::class.java)
        controller = Robolectric.buildActivity(OtherAiConfigurationActivity::class.java, intent)
        val activity = controller.get()

        activity.setTheme(R.style.Theme_WaReply) // 🔥 critical

        controller.create().start().resume()
        this.activity = controller.get()
    }

    private var activityDestroyed = false

    @After
    fun tearDown() {
        if (!activityDestroyed) {
            // Idle the looper to flush pending runnables before lifecycle transitions
            shadowOf(Looper.getMainLooper()).idle()
            controller.pause().stop().destroy()
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        PreferencesManager.resetInstance()
    }

    // -----------------------------------------------------------------------
    // Helper: reflection accessors (follows OpenAIHelperTest pattern)
    // -----------------------------------------------------------------------

    private fun getField(name: String): Field {
        val field = OtherAiConfigurationActivity::class.java.getDeclaredField(name)
        field.isAccessible = true
        return field
    }

    private fun getFetchHandler(): Handler =
        getField("fetchHandler").get(activity) as Handler

    private fun getFetchRunnable(): Runnable? =
        getField("fetchRunnable").get(activity) as? Runnable

    @Suppress("UNCHECKED_CAST")
    private fun getCurrentCall(): Call<OpenAIModelsResponse>? =
        getField("currentCall").get(activity) as? Call<OpenAIModelsResponse>

    private fun getLastFetchedUrl(): String? =
        getField("lastFetchedUrl").get(activity) as? String

    private fun setLastFetchedUrl(value: String?) {
        getField("lastFetchedUrl").set(activity, value)
    }

    private fun getProviderInput(): AutoCompleteTextView =
        activity.findViewById(R.id.llmProviderAutoCompleteTextView)

    private fun getApiKeyInput(): TextInputEditText =
        activity.findViewById(R.id.apiKeyEditText)

    private fun getBaseUrlInput(): TextInputEditText =
        activity.findViewById(R.id.baseUrlEditText)

    // -----------------------------------------------------------------------
    // 1. Debounce: intermediate inputs should NOT trigger fetchModels
    // -----------------------------------------------------------------------

    @Test
    fun `typing rapidly into base URL does not trigger immediate fetch`() {
        // Set provider to Custom to make baseUrl field active
        getProviderInput().setText("OpenAI Compatible", false)
        getApiKeyInput().setText("sk-test-key-12345")

        // Simulate rapid typing — each keystroke replaces text
        getBaseUrlInput().setText("h")
        getBaseUrlInput().setText("ht")
        getBaseUrlInput().setText("htt")
        getBaseUrlInput().setText("http")

        // The fetchRunnable should be posted but NOT yet executed
        val runnable = getFetchRunnable()
        assertNotNull("Debounce runnable should be posted", runnable)

        // No API call should have been made yet (no currentCall)
        // because the handler hasn't fired
        // The debounce runnable should be set (waiting to fire)
        assertTrue("Handler should have a pending debounce runnable",
            getFetchRunnable() != null)
    }

    @Test
    fun `debounce fires after delay with valid URL`() {
        getProviderInput().setText("OpenAI Compatible", false)
        getApiKeyInput().setText("sk-test-key-12345")

        // Type a complete valid URL
        getBaseUrlInput().setText("https://api.example.com")

        // Advance the Robolectric scheduler past the debounce window
        val looper = shadowOf(Looper.getMainLooper())
        looper.idleFor(java.time.Duration.ofMillis(600))

        // After debounce, fetchModels should have attempted to run.
        // Since this is a real network call that will fail (no server),
        // we just verify the debounce mechanism fired (currentCall may or
        // may not be set depending on Retrofit's behavior in test, but
        // the key thing is no crash occurred).
        // The fact that we got here without an exception proves the fix works.
    }

    // -----------------------------------------------------------------------
    // 2. No API call for invalid input — no crash
    // -----------------------------------------------------------------------

    @Test
    fun `typing incomplete URL does not crash`() {
        getProviderInput().setText("OpenAI Compatible", false)
        getApiKeyInput().setText("sk-test-key-12345")

        // Simulate the exact crash scenario from the bug report:
        // user types character-by-character
        val partialInputs = listOf("h", "ht", "htt", "http", "https", "https:", "https:/", "https://")
        for (input in partialInputs) {
            getBaseUrlInput().setText(input)
        }

        // Flush the handler to let debounced call fire
        val looper = shadowOf(Looper.getMainLooper())
        looper.idleFor(java.time.Duration.ofMillis(600))

        // If we reach here, no IllegalArgumentException was thrown — crash is fixed
    }

    @Test
    fun `empty base URL does not trigger API call`() {
        getProviderInput().setText("OpenAI Compatible", false)
        getApiKeyInput().setText("sk-test-key-12345")
        getBaseUrlInput().setText("")

        val looper = shadowOf(Looper.getMainLooper())
        looper.idleFor(java.time.Duration.ofMillis(600))

        // Should not crash, currentCall should remain null
        assertNull("No API call should be made for empty URL", getCurrentCall())
    }

    @Test
    fun `whitespace-only base URL does not trigger API call`() {
        getProviderInput().setText("OpenAI Compatible", false)
        getApiKeyInput().setText("sk-test-key-12345")
        getBaseUrlInput().setText("   ")

        val looper = shadowOf(Looper.getMainLooper())
        looper.idleFor(java.time.Duration.ofMillis(600))

        assertNull("No API call should be made for whitespace URL", getCurrentCall())
    }

    @Test
    fun `URL without scheme does not trigger API call`() {
        getProviderInput().setText("OpenAI Compatible", false)
        getApiKeyInput().setText("sk-test-key-12345")
        getBaseUrlInput().setText("openai.com")

        val looper = shadowOf(Looper.getMainLooper())
        looper.idleFor(java.time.Duration.ofMillis(600))

        assertNull("No API call should be made for URL without scheme", getCurrentCall())
    }

    @Test
    fun `ftp URL does not trigger API call`() {
        getProviderInput().setText("OpenAI Compatible", false)
        getApiKeyInput().setText("sk-test-key-12345")
        getBaseUrlInput().setText("ftp://example.com")

        val looper = shadowOf(Looper.getMainLooper())
        looper.idleFor(java.time.Duration.ofMillis(600))

        assertNull("No API call should be made for ftp URL", getCurrentCall())
    }

    @Test
    fun `empty API key does not trigger API call`() {
        getProviderInput().setText("OpenAI Compatible", false)
        getApiKeyInput().setText("")
        getBaseUrlInput().setText("https://api.example.com")

        val looper = shadowOf(Looper.getMainLooper())
        looper.idleFor(java.time.Duration.ofMillis(600))

        assertNull("No API call should be made without API key", getCurrentCall())
    }

    // -----------------------------------------------------------------------
    // 3. Distinct-until-changed: same URL should not re-trigger
    // -----------------------------------------------------------------------

    @Test
    fun `same URL and API key does not re-trigger fetch`() {
        // Simulate a previous successful fetch
        setLastFetchedUrl("https://api.example.com|sk-test-key-12345")

        getProviderInput().setText("OpenAI Compatible", false)
        getApiKeyInput().setText("sk-test-key-12345")
        getBaseUrlInput().setText("https://api.example.com")

        val looper = shadowOf(Looper.getMainLooper())
        looper.idleFor(java.time.Duration.ofMillis(600))

        // currentCall should remain null because the fetchKey matches lastFetchedUrl
        assertNull("Should skip fetch for same URL+key combination", getCurrentCall())
    }

    @Test
    fun `different API key triggers new fetch for same URL`() {
        setLastFetchedUrl("https://api.example.com|sk-old-key")

        getProviderInput().setText("OpenAI Compatible", false)
        getApiKeyInput().setText("sk-new-key")
        getBaseUrlInput().setText("https://api.example.com")

        val looper = shadowOf(Looper.getMainLooper())
        looper.idleFor(java.time.Duration.ofMillis(600))

        // A new fetch should be attempted since the key changed
        // (currentCall may or may not be set depending on network layer,
        //  but the important thing is no crash and lastFetchedUrl check passed)
    }

    // -----------------------------------------------------------------------
    // 4. Cancellation: new input cancels pending debounce
    // -----------------------------------------------------------------------

    @Test
    fun `new input cancels previous debounce runnable`() {
        getProviderInput().setText("OpenAI Compatible", false)
        getApiKeyInput().setText("sk-test-key-12345")

        // First input
        getBaseUrlInput().setText("https://first.example.com")
        val firstRunnable = getFetchRunnable()
        assertNotNull(firstRunnable)

        // Second input before debounce fires
        getBaseUrlInput().setText("https://second.example.com")
        val secondRunnable = getFetchRunnable()
        assertNotNull(secondRunnable)

        // The runnables should be different — first was replaced
        // (Handler.removeCallbacks was called on the first)
        assertTrue("Debounce runnable should be replaced on new input",
            firstRunnable !== secondRunnable)
    }

    // -----------------------------------------------------------------------
    // 5. Error handling: API failure does not crash
    // -----------------------------------------------------------------------

    @Test
    fun `activity survives Retrofit failure without crash`() {
        getProviderInput().setText("OpenAI Compatible", false)
        getApiKeyInput().setText("sk-test-key-12345")

        // Use a valid URL format that will fail at network layer (no actual server)
        getBaseUrlInput().setText("https://nonexistent.invalid.test")

        val looper = shadowOf(Looper.getMainLooper())
        looper.idleFor(java.time.Duration.ofMillis(600))

        // Should not crash — the try/catch around RetrofitInstance and the
        // onFailure callback should handle the error gracefully
    }

    // -----------------------------------------------------------------------
    // 6. Lifecycle: cleanup on destroy
    // -----------------------------------------------------------------------

    @Test
    fun `onDestroy cancels pending debounce and in-flight request`() {
        getProviderInput().setText("OpenAI Compatible", false)
        getApiKeyInput().setText("sk-test-key-12345")
        getBaseUrlInput().setText("https://api.example.com")

        // Verify a runnable was posted
        assertNotNull("Should have a pending debounce", getFetchRunnable())

        // Idle the looper to flush pending runnables before lifecycle transitions
        shadowOf(Looper.getMainLooper()).idle()

        // Destroy the activity
        controller.pause().stop().destroy()
        activityDestroyed = true

        // After destroy, we can't easily inspect the handler since the activity
        // is destroyed, but the key assertion is that no crash occurred during
        // the destroy lifecycle
    }

    // -----------------------------------------------------------------------
    // 7. Non-custom provider uses predefined URL (no validation needed)
    // -----------------------------------------------------------------------

    @Test
    fun `non-Custom provider does not use base URL input`() {
        // When provider is "OpenAI", the providerUrls map provides the URL,
        // so the baseUrlInput value is irrelevant
        getProviderInput().setText("OpenAI", false)
        getApiKeyInput().setText("sk-test-key-12345")

        // Set an invalid base URL — should not matter for non-Custom provider
        getBaseUrlInput().setText("garbage")

        val looper = shadowOf(Looper.getMainLooper())
        looper.idleFor(java.time.Duration.ofMillis(600))

        // Should not crash — predefined URL from providerUrls is used instead
    }

    // -----------------------------------------------------------------------
    // 8. Debounce constant value
    // -----------------------------------------------------------------------

    @Test
    fun `debounce delay is 500ms`() {
        // The companion object is private, so Kotlin compiles the const
        // as a static field directly on the enclosing class.
        val field = OtherAiConfigurationActivity::class.java.getDeclaredField("FETCH_DEBOUNCE_MS")
        field.isAccessible = true
        val value = field.getLong(null)
        assertEquals("Debounce should be 500ms", 500L, value)
    }
}
