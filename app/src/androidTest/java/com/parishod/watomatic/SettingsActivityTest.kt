package com.parishod.watomatic

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.parishod.watomatic.activity.settings.SettingsActivity
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [SettingsActivity].
 *
 * Verifies that the activity launches correctly, displays the toolbar and
 * settings fragment container, and survives configuration changes.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class SettingsActivityTest {

    private fun launchActivity(): ActivityScenario<SettingsActivity> {
        val intent = Intent(ApplicationProvider.getApplicationContext(), SettingsActivity::class.java)
        return ActivityScenario.launch(intent)
    }

    @Test
    fun settingsActivityLaunchesSuccessfully() {
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
    fun settingsFragmentContainerIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.setting_fragment_container)).check(matches(isDisplayed()))
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
    fun supportActionBarHasUpButton() {
        val scenario = launchActivity()
        scenario.onActivity { activity ->
            assertNotNull(activity.supportActionBar)
            assert(activity.supportActionBar!!.displayOptions and
                    androidx.appcompat.app.ActionBar.DISPLAY_HOME_AS_UP != 0)
        }
        scenario.close()
    }
}
