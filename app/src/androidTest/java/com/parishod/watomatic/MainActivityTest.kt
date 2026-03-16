package com.parishod.watomatic

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.parishod.watomatic.activity.main.MainActivity
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
    fun activityLifecycleTransitionsSuccessfully() {
        // Verify activity goes through lifecycle states without crashing
        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            scenario.onActivity { activity ->
                assertNotNull(activity)
            }
        }
    }
}
