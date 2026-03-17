package com.parishod.watomatic

import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.parishod.watomatic.activity.main.MainActivity
import com.parishod.watomatic.model.preferences.PreferencesManager
import org.junit.Assert.assertNotNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [MainActivity].
 *
 * Runs on an Android device or emulator. Tests here verify that the activity
 * launches correctly and the primary UI elements are visible.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {

    @get:Rule
    val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @Test
    fun mainActivityLaunchesSuccessfully() {
        activityRule.scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }

    @Test
    fun mainFrameLayoutIsDisplayed() {
        onView(withId(R.id.main_frame_layout)).check(matches(isDisplayed()))
    }

    @Test
    fun autoRepliesSwitchIsDisplayed() {
        onView(withId(R.id.switch_auto_replies)).check(matches(isDisplayed()))
    }

    @Test
    fun activityCanBeRecreated() {
        activityRule.scenario.recreate()
        activityRule.scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }

    @Test
    fun aiReplyTextIsDisplayed() {
        onView(withId(R.id.ai_reply_text)).check(matches(isDisplayed()))
    }

    @Test
    fun editButtonIsDisplayed() {
        onView(withId(R.id.btn_edit)).check(matches(isDisplayed()))
    }

    @Test
    fun bottomNavIsDisplayed() {
        onView(withId(R.id.bottom_nav)).check(matches(isDisplayed()))
    }

    @Test
    fun filterContactsRowIsDisplayed() {
        onView(withId(R.id.filter_contacts)).perform(scrollTo()).check(matches(isDisplayed()))
    }

    @Test
    fun filterMessageTypeRowIsDisplayed() {
        onView(withId(R.id.filter_message_type)).perform(scrollTo()).check(matches(isDisplayed()))
    }

    @Test
    fun filterAppsRowIsDisplayed() {
        onView(withId(R.id.filter_apps)).perform(scrollTo()).check(matches(isDisplayed()))
    }

    @Test
    fun filterReplyCooldownRowIsDisplayed() {
        onView(withId(R.id.filter_reply_cooldown)).perform(scrollTo()).check(matches(isDisplayed()))
    }

    @Test
    fun autoReplySwitchStateMatchesPreference() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val prefs = PreferencesManager.getPreferencesInstance(context)
        if (prefs.isServiceEnabled) {
            onView(withId(R.id.switch_auto_replies)).check(matches(isChecked()))
        } else {
            onView(withId(R.id.switch_auto_replies)).check(matches(isNotChecked()))
        }
    }
}
