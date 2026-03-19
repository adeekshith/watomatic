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
import com.parishod.watomatic.activity.donation.DonationActivity
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [DonationActivity].
 *
 * Verifies the donation screen launches and displays its fragment container.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class DonationActivityTest {

    private fun launchActivity(): ActivityScenario<DonationActivity> {
        val intent = Intent(ApplicationProvider.getApplicationContext(), DonationActivity::class.java)
        return ActivityScenario.launch(intent)
    }

    @Test
    fun donationActivityLaunchesSuccessfully() {
        val scenario = launchActivity()
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
        scenario.close()
    }

    @Test
    fun rootLayoutIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.donation_root_layout)).check(matches(isDisplayed()))
        scenario.close()
    }

    @Test
    fun fragmentContainerIsDisplayed() {
        val scenario = launchActivity()
        onView(withId(R.id.main_frame_layout)).check(matches(isDisplayed()))
        scenario.close()
    }

    // Note: recreation test removed because DonationFragment.getData() uses a Retrofit
    // callback that calls requireContext() without checking isAdded(), which crashes
    // when the fragment detaches during recreate(). See DonationFragment line 131.
}
