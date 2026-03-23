package com.parishod.watomatic

import android.content.Intent
import androidx.preference.PreferenceManager
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.platform.app.InstrumentationRegistry
import com.parishod.watomatic.activity.notification.NotificationIntentActivity
import com.parishod.watomatic.model.preferences.PreferencesManager
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for [NotificationIntentActivity].
 *
 * This activity is launched from notification actions. It reads extras
 * to determine which app to launch, or falls back to the home screen.
 * Since it calls finish() in onCreate, tests verify it launches and
 * finishes correctly for various intent configurations.
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class NotificationIntentActivityTest {

    @Before
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
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

    // NotificationIntentActivity calls finish() in onCreate after processing
    // the intent, so we can't reliably use onActivity{} — the activity may
    // already be destroyed. Instead we verify that launch() doesn't crash.

    @Test
    fun launchWithNoExtras_doesNotCrash() {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            NotificationIntentActivity::class.java
        )
        // Activity redirects to MainActivity and finishes — just verify no crash
        val scenario = ActivityScenario.launch<NotificationIntentActivity>(intent)
        scenario.close()
    }

    @Test
    fun launchWithPackageExtra_doesNotCrash() {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            NotificationIntentActivity::class.java
        ).apply {
            putExtra("package", "com.whatsapp")
        }
        // WhatsApp isn't installed on test device, so getLaunchIntentForPackage
        // returns null and the activity returns without launching — no crash
        val scenario = ActivityScenario.launch<NotificationIntentActivity>(intent)
        scenario.close()
    }

    @Test
    fun launchWithNullPackageExtra_doesNotCrash() {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            NotificationIntentActivity::class.java
        ).apply {
            putExtra("package", null as String?)
        }
        val scenario = ActivityScenario.launch<NotificationIntentActivity>(intent)
        scenario.close()
    }

    @Test
    fun launchWithEmptyPackageExtra_doesNotCrash() {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            NotificationIntentActivity::class.java
        ).apply {
            putExtra("package", "")
        }
        val scenario = ActivityScenario.launch<NotificationIntentActivity>(intent)
        scenario.close()
    }
}
