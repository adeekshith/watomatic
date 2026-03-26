package com.parishod.watomatic.model.adapters

import android.app.Application
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.parishod.watomatic.model.data.ContactHolder
import com.parishod.watomatic.model.preferences.PreferencesManager
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [ContactListAdapter].
 *
 * Tests item count, view types, filtering, checkpoint/restore,
 * and custom name management.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class ContactListAdapterTest {

    private lateinit var context: Application

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        PreferencesManager.resetInstance()
        PreferencesManager.getPreferencesInstance(context)
    }

    @After
    fun tearDown() {
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        PreferencesManager.resetInstance()
    }

    private fun createAdapter(contacts: ArrayList<ContactHolder>): ContactListAdapter {
        return ContactListAdapter(context, contacts)
    }

    // --- Item count ---

    @Test
    fun `empty list has zero items`() {
        val adapter = createAdapter(ArrayList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `item count matches contact list size`() {
        val contacts = arrayListOf(
            ContactHolder("Alice", false, false),
            ContactHolder("Bob", false, false),
            ContactHolder("Charlie", false, false)
        )
        val adapter = createAdapter(contacts)
        assertEquals(3, adapter.itemCount)
    }

    // --- View types ---

    @Test
    fun `regular contact returns ITEM_TYPE_CONTACT`() {
        val contacts = arrayListOf(ContactHolder("Alice", false, false))
        val adapter = createAdapter(contacts)
        assertEquals(ContactListAdapter.ITEM_TYPE_CONTACT, adapter.getItemViewType(0))
    }

    @Test
    fun `custom contact returns ITEM_TYPE_CUSTOM`() {
        val contacts = arrayListOf(ContactHolder("Custom Name", true, true))
        val adapter = createAdapter(contacts)
        assertEquals(ContactListAdapter.ITEM_TYPE_CUSTOM, adapter.getItemViewType(0))
    }

    // --- Item ID ---

    @Test
    fun `getItemId returns position`() {
        val contacts = arrayListOf(
            ContactHolder("A", false, false),
            ContactHolder("B", false, false)
        )
        val adapter = createAdapter(contacts)
        assertEquals(0L, adapter.getItemId(0))
        assertEquals(1L, adapter.getItemId(1))
    }

    // --- Checkpoint and restore ---

    @Test
    fun `checkpoint saves checked state`() {
        val contacts = arrayListOf(
            ContactHolder("Alice", true, false),  // checked=true, custom=false
            ContactHolder("Bob", false, false)     // checked=false, custom=false
        )
        val adapter = createAdapter(contacts)
        adapter.createCheckpoint()

        // Modify state
        contacts[0].isChecked = false
        contacts[1].isChecked = true

        // Restore should revert
        adapter.restoreCheckpoint()
        assertTrue("Alice should be checked after restore", contacts[0].isChecked)
        assertFalse("Bob should be unchecked after restore", contacts[1].isChecked)
    }

    @Test
    fun `checkpoint with no checked contacts`() {
        val contacts = arrayListOf(
            ContactHolder("Alice", false, false),
            ContactHolder("Bob", false, false)
        )
        val adapter = createAdapter(contacts)
        adapter.createCheckpoint()

        contacts[0].isChecked = true
        adapter.restoreCheckpoint()
        assertFalse("Alice should be unchecked after restore", contacts[0].isChecked)
    }

    // --- Add custom name ---

    @Test
    fun `addCustomName increases item count`() {
        val contacts = arrayListOf(ContactHolder("Alice", false, false))
        val adapter = createAdapter(contacts)
        assertEquals(1, adapter.itemCount)

        adapter.addCustomName("VIP")
        assertEquals(2, adapter.itemCount)
    }

    @Test
    fun `addCustomName inserts at position 0`() {
        val contacts = arrayListOf(ContactHolder("Alice", false, false))
        val adapter = createAdapter(contacts)

        adapter.addCustomName("VIP")
        assertEquals(ContactListAdapter.ITEM_TYPE_CUSTOM, adapter.getItemViewType(0))
        assertEquals(ContactListAdapter.ITEM_TYPE_CONTACT, adapter.getItemViewType(1))
    }

    // --- Filtering ---

    @Test
    fun `filter returns non-null`() {
        val adapter = createAdapter(ArrayList())
        assertNotNull(adapter.filter)
    }

    @Test
    fun `getFilter is consistent`() {
        val adapter = createAdapter(ArrayList())
        val filter1 = adapter.filter
        val filter2 = adapter.filter
        // Each call creates a new Filter, but both should be non-null
        assertNotNull(filter1)
        assertNotNull(filter2)
    }
}
