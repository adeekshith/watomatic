package com.parishod.watomatic

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.parishod.watomatic.activity.about.AboutActivity
import org.hamcrest.Matchers.containsString
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [AboutActivity].
 *
 * Verifies the about screen displays version info, privacy policy link,
 * and developer link.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class AboutActivityTest {

    private fun launchActivity(): ActivityScenario<AboutActivity> {
        val intent = Intent(ApplicationProvider.getApplicationContext(), AboutActivity::class.java)
        return ActivityScenario.launch(intent)
    }

    @Test
    fun aboutActivityLaunchesSuccessfully() {
        val scenario = launchActivity()
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
        scenario.close()
    }

    @Test
    fun appVersionIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.appVersion)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun appVersionContainsVersionName() {
        val scenario = launchActivity()
        onView(withId(R.id.appVersion))
            .check(matches(withText(containsString(BuildConfig.VERSION_NAME))))
        scenario.close()
    }

    @Test
    fun privacyPolicyCardIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.privacyPolicyCardView)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun developerLinkIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.developerLink)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun scrollViewIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.about_scroll_view)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun appTitleCardIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.appTitleCardView)).check(matches(isDisplayed()))
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
}
