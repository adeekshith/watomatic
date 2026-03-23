package com.parishod.watomatic

import android.content.Intent
import android.os.Build
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.parishod.watomatic.activity.contactselector.ContactSelectorActivity
import com.parishod.watomatic.model.preferences.PreferencesManager
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [ContactSelectorActivity].
 *
 * Verifies the contact selector screen launches correctly,
 * displays toolbar and fragment container.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class ContactSelectorActivityTest {

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        if (Build.VERSION.SDK_INT >= 33) {
            InstrumentationRegistry.getInstrumentation().uiAutomation
                .grantRuntimePermission(
                    context.packageName,
                    "android.permission.POST_NOTIFICATIONS"
                )
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        PreferencesManager.resetInstance()
        val prefs = PreferencesManager.getPreferencesInstance(context)
        prefs.setGuestMode(true)
    }

    @After
    fun tearDown() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        PreferencesManager.resetInstance()
    }

    private fun launchActivity(): ActivityScenario<ContactSelectorActivity> {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            ContactSelectorActivity::class.java
        )
        return ActivityScenario.launch(intent)
    }

    // --- Launch ---

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
    fun toolbarExists() {
        val scenario = launchActivity()
        scenario.onActivity { activity ->
            val toolbar = activity.findViewById<android.view.View>(R.id.toolbar)
            assertNotNull("Toolbar should exist", toolbar)
        }
        scenario.close()
    }

    // --- Root layout ---

    @Test
    fun rootLayoutExists() {
        val scenario = launchActivity()
        scenario.onActivity { activity ->
            val root = activity.findViewById<android.view.View>(R.id.contact_selector_root)
            assertNotNull("Root layout should exist", root)
        }
        scenario.close()
    }

    // --- Fragment container ---

    @Test
    fun fragmentContainerExists() {
        val scenario = launchActivity()
        scenario.onActivity { activity ->
            val container = activity.findViewById<android.view.View>(R.id.contact_selector_layout)
            assertNotNull("Fragment container should exist", container)
        }
        scenario.close()
    }

    // --- Fragment loaded ---

    @Test
    fun contactSelectorFragmentIsLoaded() {
        val scenario = launchActivity()
        scenario.onActivity { activity ->
            val fragment = activity.supportFragmentManager.findFragmentById(R.id.contact_selector_layout)
            assertNotNull("ContactSelectorFragment should be loaded", fragment)
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
