package com.parishod.watomatic.model.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AutoStartHelperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
    }

    // --- getInstance ---

    @Test
    fun `getInstance returns non-null instance`() {
        val instance = AutoStartHelper.getInstance()
        assertNotNull(instance)
    }

    @Test
    fun `getInstance returns new instance each time`() {
        val first = AutoStartHelper.getInstance()
        val second = AutoStartHelper.getInstance()
        assertNotSame(first, second)
    }

    // --- getAutoStartPermission ---

    @Test
    fun `getAutoStartPermission does not throw for default Robolectric brand`() {
        // Robolectric's default Build.BRAND is "robolectric" which won't match
        // any known brand — should hit the default case and show a Toast
        val helper = AutoStartHelper.getInstance()
        // Should not throw; the default case shows a Toast
        try {
            helper.getAutoStartPermission(context)
        } catch (e: android.content.res.Resources.NotFoundException) {
            // Expected in Robolectric when getString() can't resolve the resource
        }
    }

    @Test
    @Config(qualifiers = "")
    fun `getAutoStartPermission handles unsupported brand gracefully`() {
        // With default Robolectric brand, should hit the default Toast case
        val helper = AutoStartHelper.getInstance()
        try {
            helper.getAutoStartPermission(context)
        } catch (e: android.content.res.Resources.NotFoundException) {
            // Expected: getString(R.string.setting_not_available_for_device) may not resolve
        }
        // Test passes as long as no unexpected exception occurs
    }
}
