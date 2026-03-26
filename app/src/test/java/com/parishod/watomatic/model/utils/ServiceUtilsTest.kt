package com.parishod.watomatic.model.utils

import android.content.ComponentName
import android.provider.Settings
import androidx.test.core.app.ApplicationProvider
import com.parishod.watomatic.service.NotificationService
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [ServiceUtils] singleton and the notification listener
 * enabled check logic (mirrors MainFragment.isListenerEnabled).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ServiceUtilsTest {

    // --- Singleton ---

    @Test
    fun `getInstance returns non-null`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val instance = ServiceUtils.getInstance(context)
        assertNotNull(instance)
    }

    @Test
    fun `getInstance returns same singleton`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val first = ServiceUtils.getInstance(context)
        val second = ServiceUtils.getInstance(context)
        assertSame(first, second)
    }

    // --- Notification listener enabled check ---
    // This tests the same logic as MainFragment.isListenerEnabled() using
    // Settings.Secure "enabled_notification_listeners" which is the standard
    // Android mechanism for checking notification listener permission.

    @Test
    fun `notification listener is disabled when setting is null`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        // Settings.Secure is empty by default in Robolectric
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        // Should be null or not contain our component
        val cn = ComponentName(context, NotificationService::class.java)
        val isEnabled = flat != null && flat.contains(cn.flattenToString())
        assertFalse("Listener should be disabled by default", isEnabled)
    }

    @Test
    fun `notification listener is enabled when component is in setting`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val cn = ComponentName(context, NotificationService::class.java)
        Settings.Secure.putString(
            context.contentResolver,
            "enabled_notification_listeners",
            cn.flattenToString()
        )
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val isEnabled = flat != null && flat.contains(cn.flattenToString())
        assertTrue("Listener should be enabled", isEnabled)
    }

    @Test
    fun `notification listener is disabled when different component is in setting`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        Settings.Secure.putString(
            context.contentResolver,
            "enabled_notification_listeners",
            "com.other.app/com.other.app.SomeService"
        )
        val cn = ComponentName(context, NotificationService::class.java)
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val isEnabled = flat != null && flat.contains(cn.flattenToString())
        assertFalse("Listener should not be enabled for different component", isEnabled)
    }

    @Test
    fun `notification listener found among multiple listeners`() {
        val context = ApplicationProvider.getApplicationContext<android.app.Application>()
        val cn = ComponentName(context, NotificationService::class.java)
        val multipleListeners = "com.other.app/com.other.SomeService:${cn.flattenToString()}"
        Settings.Secure.putString(
            context.contentResolver,
            "enabled_notification_listeners",
            multipleListeners
        )
        val flat = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners")
        val isEnabled = flat != null && flat.contains(cn.flattenToString())
        assertTrue("Should find our listener among multiple", isEnabled)
    }
}
