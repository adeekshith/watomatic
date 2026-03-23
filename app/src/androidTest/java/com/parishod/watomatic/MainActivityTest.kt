package com.parishod.watomatic

import android.content.Intent
import android.os.Build
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.scrollTo
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isDisplayed
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.withId
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.parishod.watomatic.activity.main.MainActivity
import com.parishod.watomatic.model.preferences.PreferencesManager
import org.hamcrest.Matchers.not
import org.junit.Assert.assertNotNull
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [MainActivity].
 *
 * Runs on an Android device or emulator. Tests here verify that the activity
 * launches correctly, the primary UI elements are visible, and key interactions
 * work as expected.
 *
 * Note: In the GooglePlay flavor, MainActivity redirects to LoginActivity when
 * the user is not logged in and not in guest mode. We set guest mode before
 * launching to bypass this redirect.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class MainActivityTest {

    private lateinit var scenario: ActivityScenario<MainActivity>

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext

        // Grant POST_NOTIFICATIONS permission on Android 13+ (API 33+) to prevent
        // the system permission dialog from blocking Espresso tests.
        if (Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(
                    context.packageName,
                    "android.permission.POST_NOTIFICATIONS"
                )
        }

        // Reset to clean state
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        PreferencesManager.resetInstance()
        val prefs = PreferencesManager.getPreferencesInstance(context)
        // Set guest mode to bypass GooglePlay flavor's login redirect
        prefs.setGuestMode(true)
        // Ensure service is disabled so switch starts unchecked
        prefs.setServicePref(false)

        // Launch activity AFTER prefs are configured
        val intent = Intent(ApplicationProvider.getApplicationContext(), MainActivity::class.java)
        scenario = ActivityScenario.launch(intent)
    }

    @After
    fun tearDown() {
        scenario.close()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        PreferencesManager.resetInstance()
    }

    // --- Launch Tests ---

    @Test
    fun mainActivityLaunchesSuccessfully() {
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }

    @Test
    fun mainFrameLayoutIsDisplayed() {
        onView(withId(R.id.main_frame_layout)).check(matches(isDisplayed()))
    }

    @Test
    fun activityCanBeRecreated() {
        scenario.recreate()
        scenario.onActivity { activity ->
            assertNotNull(activity)
        }
    }

    // --- Auto-Reply Switch ---

    @Test
    fun autoRepliesSwitchIsDisplayed() {
        onView(withId(R.id.switch_auto_replies)).check(matches(isDisplayed()))
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

    // --- Auto-Reply Card ---

    @Test
    fun aiReplyTextIsDisplayed() {
        onView(withId(R.id.ai_reply_text)).check(matches(isDisplayed()))
    }

    @Test
    fun editButtonIsDisplayed() {
        onView(withId(R.id.btn_edit)).check(matches(isDisplayed()))
    }

    @Test
    fun aiReplyTextIsNotEmpty() {
        onView(withId(R.id.ai_reply_text))
            .check(matches(not(withText(""))))
    }

    // --- Bottom Navigation ---

    @Test
    fun bottomNavIsDisplayed() {
        onView(withId(R.id.bottom_nav)).check(matches(isDisplayed()))
    }

    @Test
    fun bottomNavAtomaticItemIsDisplayed() {
        onView(withId(R.id.navigation_atomatic)).check(matches(isDisplayed()))
    }

    @Test
    fun bottomNavCommunityItemIsDisplayed() {
        onView(withId(R.id.navigation_community)).check(matches(isDisplayed()))
    }

    @Test
    fun bottomNavSettingsItemIsDisplayed() {
        onView(withId(R.id.navigation_settings)).check(matches(isDisplayed()))
    }

    // --- Filters Section ---

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
    fun contactsFilterDescriptionIsDisplayed() {
        onView(withId(R.id.contacts_filter_description))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun messageTypeDescriptionIsDisplayed() {
        onView(withId(R.id.message_type_desc))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun enabledAppsCountIsDisplayed() {
        onView(withId(R.id.enabled_apps_count))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    @Test
    fun replyCooldownDescriptionIsDisplayed() {
        onView(withId(R.id.reply_cooldown_description))
            .perform(scrollTo())
            .check(matches(isDisplayed()))
    }

    // --- Filter descriptions have content ---

    @Test
    fun contactsFilterDescriptionIsNotEmpty() {
        onView(withId(R.id.contacts_filter_description))
            .perform(scrollTo())
            .check(matches(not(withText(""))))
    }

    @Test
    fun messageTypeDescriptionIsNotEmpty() {
        onView(withId(R.id.message_type_desc))
            .perform(scrollTo())
            .check(matches(not(withText(""))))
    }

    @Test
    fun enabledAppsCountIsNotEmpty() {
        onView(withId(R.id.enabled_apps_count))
            .perform(scrollTo())
            .check(matches(not(withText(""))))
    }

    @Test
    fun replyCooldownDescriptionIsNotEmpty() {
        onView(withId(R.id.reply_cooldown_description))
            .perform(scrollTo())
            .check(matches(not(withText(""))))
    }
}
