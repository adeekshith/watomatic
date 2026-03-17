package com.parishod.watomatic

import android.content.Intent
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.parishod.watomatic.activity.customreplyeditor.CustomReplyEditorActivity
import com.parishod.watomatic.model.CustomRepliesData
import com.parishod.watomatic.model.preferences.PreferencesManager
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [CustomReplyEditorActivity].
 *
 * Verifies the reply editor screen launches correctly and displays the
 * three reply method cards (manual, automatic AI, BYOK) along with the
 * save button and other key UI elements.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class CustomReplyEditorActivityTest {

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        PreferencesManager.resetInstance()
        CustomRepliesData.resetInstance()
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        PreferencesManager.resetInstance()
        CustomRepliesData.resetInstance()
    }

    private fun launchActivity(): ActivityScenario<CustomReplyEditorActivity> {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            CustomReplyEditorActivity::class.java
        )
        return ActivityScenario.launch(intent)
    }

    @Test
    fun activityLaunchesSuccessfully() {
        val scenario = launchActivity()
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
        scenario.close()
    }

    @Test
    fun toolbarIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.toolbar)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun manualRepliesCardIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.manual_replies_card)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun automaticAiProviderCardIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.automatic_ai_provider_card))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun otherAiProviderCardIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.other_ai_provider_card))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun saveButtonIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.saveCustomReplyBtn)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun scrollViewIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.custom_reply_editor_scroll_view)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun communityTemplatesLinkExists() {
        val scenario = launchActivity()
        scenario.onActivity { activity ->
            val view = activity.findViewById<android.view.View>(R.id.tip_wato_message)
            assertNotNull("Community templates link should exist", view)
        }
        scenario.close()
    }

    @Test
    fun activityCanBeRecreated() {
        val scenario = launchActivity()
        scenario.recreate()
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
        scenario.close()
    }

    @Test
    fun manualReplyTextInputExists() {
        val scenario = launchActivity()
        // Manual is the default selected method, so the text input should exist
        scenario.onActivity { activity ->
            val view = activity.findViewById<android.view.View>(R.id.autoReplyTextInputEditText)
            assertNotNull("Manual reply text input should exist", view)
        }
        scenario.close()
    }
}
