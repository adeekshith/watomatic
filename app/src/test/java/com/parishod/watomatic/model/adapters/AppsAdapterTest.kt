package com.parishod.watomatic.model.adapters

import com.parishod.watomatic.model.data.AppItem
import com.parishod.watomatic.model.data.DialogListItem
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Unit tests for [AppsAdapter].
 *
 * Tests view type resolution, item count, and data handling.
 * ViewHolder binding is not tested here as it requires inflated views.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class AppsAdapterTest {

    private fun createAdapter(items: List<DialogListItem>): AppsAdapter {
        return AppsAdapter(items) { _, _ -> }
    }

    // --- Item count ---

    @Test
    fun `empty list has zero items`() {
        val adapter = createAdapter(emptyList())
        assertEquals(0, adapter.itemCount)
    }

    @Test
    fun `item count matches list size`() {
        val items = listOf(
            DialogListItem.SectionHeader("Supported"),
            DialogListItem.AppItemWrapper(AppItem(0, "WhatsApp", "com.whatsapp")),
            DialogListItem.AppItemWrapper(AppItem(0, "Telegram", "org.telegram.messenger"))
        )
        val adapter = createAdapter(items)
        assertEquals(3, adapter.itemCount)
    }

    // --- View types ---

    @Test
    fun `section header returns VIEW_TYPE_SECTION_HEADER`() {
        val items = listOf(DialogListItem.SectionHeader("Header"))
        val adapter = createAdapter(items)
        assertEquals(0, adapter.getItemViewType(0)) // VIEW_TYPE_SECTION_HEADER = 0
    }

    @Test
    fun `app item returns VIEW_TYPE_APP`() {
        val items = listOf(
            DialogListItem.AppItemWrapper(AppItem(0, "WhatsApp", "com.whatsapp"))
        )
        val adapter = createAdapter(items)
        assertEquals(1, adapter.getItemViewType(0)) // VIEW_TYPE_APP = 1
    }

    @Test
    fun `mixed list returns correct view types`() {
        val items = listOf(
            DialogListItem.SectionHeader("Section"),
            DialogListItem.AppItemWrapper(AppItem(0, "WhatsApp", "com.whatsapp")),
            DialogListItem.SectionHeader("Another"),
            DialogListItem.AppItemWrapper(AppItem(0, "Telegram", "org.telegram.messenger"))
        )
        val adapter = createAdapter(items)
        assertEquals(0, adapter.getItemViewType(0)) // header
        assertEquals(1, adapter.getItemViewType(1)) // app
        assertEquals(0, adapter.getItemViewType(2)) // header
        assertEquals(1, adapter.getItemViewType(3)) // app
    }

    // --- Callback ---

    @Test
    fun `onToggle callback is stored`() {
        var callbackCalled = false
        val adapter = AppsAdapter(emptyList()) { _, _ -> callbackCalled = true }
        assertNotNull(adapter)
        // Callback invocation requires ViewHolder binding which needs inflation
    }

    // --- AppItem data ---

    @Test
    fun `AppItemWrapper preserves AppItem data`() {
        val appItem = AppItem(42, "TestApp", "com.test.app", isEnabled = true, isExperimental = true)
        val wrapper = DialogListItem.AppItemWrapper(appItem)
        assertEquals("TestApp", wrapper.appItem.name)
        assertEquals("com.test.app", wrapper.appItem.packageName)
        assertEquals(42, wrapper.appItem.iconRes)
        assertTrue(wrapper.appItem.isEnabled)
        assertTrue(wrapper.appItem.isExperimental)
    }

    @Test
    fun `SectionHeader preserves title`() {
        val header = DialogListItem.SectionHeader("My Section")
        assertEquals("My Section", header.title)
    }
}
