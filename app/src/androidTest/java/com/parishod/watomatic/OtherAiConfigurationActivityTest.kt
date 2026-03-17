package com.parishod.watomatic

import android.content.Intent
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.parishod.watomatic.activity.customreplyeditor.OtherAiConfigurationActivity
import com.parishod.watomatic.model.preferences.PreferencesManager
import org.hamcrest.Matchers.containsString
import org.hamcrest.Matchers.not
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [OtherAiConfigurationActivity].
 *
 * Verifies the AI configuration screen launches, displays form fields
 * (provider dropdown, API key, model selector, system prompt), and
 * the save button.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class OtherAiConfigurationActivityTest {

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        PreferencesManager.resetInstance()
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        PreferencesManager.resetInstance()
    }

    private fun launchActivity(): ActivityScenario<OtherAiConfigurationActivity> {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            OtherAiConfigurationActivity::class.java
        )
        return ActivityScenario.launch(intent)
    }

    // --- Launch Tests ---

    @Test
    fun activityLaunchesSuccessfully() {
        val scenario = launchActivity()
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
        scenario.close()
    }

    // --- Toolbar ---

    @Test
    fun toolbarIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()))
        scenario.close()
    }

    // --- Form Fields ---

    @Test
    fun providerDropdownIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.llmProviderAutoCompleteTextView)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun providerHasDefaultSelection() {
        val scenario = launchActivity()
        // When no provider is saved, the activity defaults to the first provider in the list
        onView(withId(R.id.llmProviderAutoCompleteTextView))
            .check(matches(not(withText(""))))
        scenario.close()
    }

    @Test
    fun apiKeyInputIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.apiKeyEditText)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun apiKeyInputLayoutIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.apiKeyInputLayout)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun modelDropdownIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.modelAutoCompleteTextView)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun systemPromptIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.systemPromptEditText)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun systemPromptHasDefaultText() {
        val scenario = launchActivity()
        onView(withId(R.id.systemPromptEditText))
            .check(matches(not(withText(""))))
        scenario.close()
    }

    @Test
    fun saveButtonIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.saveConfigBtn)).check(matches(isDisplayed()))
        scenario.close()
    }

    // --- Base URL Visibility ---

    @Test
    fun baseUrlIsHiddenByDefault() {
        val scenario = launchActivity()
        scenario.onActivity { activity ->
            val baseUrlLayout = activity.findViewById<android.view.View>(R.id.baseUrlInputLayout)
            assertNotNull("Base URL layout should exist", baseUrlLayout)
            assert(baseUrlLayout.visibility == android.view.View.GONE) {
                "Base URL should be hidden when provider is not Custom"
            }
        }
        scenario.close()
    }

    // --- Recreation ---

    @Test
    fun activityCanBeRecreated() {
        val scenario = launchActivity()
        scenario.recreate()
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
        scenario.close()
    }
}
