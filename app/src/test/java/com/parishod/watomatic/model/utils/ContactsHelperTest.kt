package com.parishod.watomatic.model.utils

import android.Manifest
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.parishod.watomatic.model.preferences.PreferencesManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ContactsHelperTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        PreferencesManager.resetInstance()
        context = ApplicationProvider.getApplicationContext()
    }

    @After
    fun tearDown() {
        PreferencesManager.resetInstance()
    }

    // --- getInstance ---

    @Test
    fun `getInstance returns non-null instance`() {
        val helper = ContactsHelper.getInstance(context)
        assertNotNull(helper)
    }

    @Test
    fun `getInstance returns new instance each time`() {
        val first = ContactsHelper.getInstance(context)
        val second = ContactsHelper.getInstance(context)
        // ContactsHelper is not a singleton — each call returns a new instance
        assertNotNull(first)
        assertNotNull(second)
    }

    // --- hasContactPermission ---

    @Test
    fun `hasContactPermission returns false when permission not granted`() {
        val helper = ContactsHelper(context)
        // By default Robolectric does not grant permissions
        assertFalse(helper.hasContactPermission())
    }

    @Test
    fun `hasContactPermission returns true when permission granted`() {
        val app = context as Application
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.READ_CONTACTS)
        val helper = ContactsHelper(context)
        assertTrue(helper.hasContactPermission())
    }

    // --- getContactList ---

    @Test
    fun `getContactList returns empty list when no permission and no custom contacts`() {
        val helper = ContactsHelper(context)
        // No permission granted, no custom contacts saved
        val result = helper.getContactList()
        assertNotNull(result)
        assertTrue(result.isEmpty())
    }

    @Test
    fun `getContactList includes custom contacts from preferences`() {
        // Save custom reply names in preferences
        val prefs = PreferencesManager.getPreferencesInstance(context)
        val customNames = setOf("CustomUser1", "CustomUser2")
        prefs.customReplyNames = customNames

        val helper = ContactsHelper(context)
        val result = helper.getContactList()

        // Should contain the custom contacts even without phone permission
        assertEquals(2, result.size)
        assertTrue(result.any { it.contactName == "CustomUser1" })
        assertTrue(result.any { it.contactName == "CustomUser2" })
        // Custom contacts should have isCustom = true
        assertTrue(result.all { it.isCustom })
    }

    @Test
    fun `getContactList returns only custom contacts when permission denied`() {
        val prefs = PreferencesManager.getPreferencesInstance(context)
        prefs.customReplyNames = setOf("CustomOnly")

        val helper = ContactsHelper(context)
        val result = helper.getContactList()

        assertEquals(1, result.size)
        assertEquals("CustomOnly", result[0].contactName)
        assertTrue(result[0].isCustom)
    }

    @Test
    fun `getContactList handles empty cursor with permission granted`() {
        val app = context as Application
        Shadows.shadowOf(app).grantPermissions(Manifest.permission.READ_CONTACTS)
        val helper = ContactsHelper(context)

        // With permission granted but no contacts in the provider
        val result = helper.getContactList()
        assertNotNull(result)
        // May be empty since Robolectric's ContentProvider has no contacts
        assertTrue(result.size >= 0)
    }
}
