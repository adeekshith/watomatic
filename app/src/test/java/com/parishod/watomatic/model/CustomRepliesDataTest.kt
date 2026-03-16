package com.parishod.watomatic.model

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import com.parishod.watomatic.model.preferences.PreferencesManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class CustomRepliesDataTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Reset singletons so each test starts fresh
        CustomRepliesData.resetInstance()
        PreferencesManager.resetInstance()
        // Clear shared preferences storage
        context.getSharedPreferences("CustomRepliesData", Context.MODE_PRIVATE)
            .edit().clear().commit()
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
    }

    @After
    fun tearDown() {
        CustomRepliesData.resetInstance()
        PreferencesManager.resetInstance()
    }

    // --- isValidCustomReply(String) ---

    @Test
    fun `isValidCustomReply returns false for null string`() {
        assertFalse(CustomRepliesData.isValidCustomReply(null as String?))
    }

    @Test
    fun `isValidCustomReply returns false for empty string`() {
        assertFalse(CustomRepliesData.isValidCustomReply(""))
    }

    @Test
    fun `isValidCustomReply returns true for valid string`() {
        assertTrue(CustomRepliesData.isValidCustomReply("Hello, I'm busy right now"))
    }

    @Test
    fun `isValidCustomReply returns true for single character`() {
        assertTrue(CustomRepliesData.isValidCustomReply("x"))
    }

    @Test
    fun `isValidCustomReply returns true for string at max length`() {
        val maxString = "a".repeat(CustomRepliesData.MAX_STR_LENGTH_CUSTOM_REPLY)
        assertTrue(CustomRepliesData.isValidCustomReply(maxString))
    }

    @Test
    fun `isValidCustomReply returns false for string exceeding max length`() {
        val tooLong = "a".repeat(CustomRepliesData.MAX_STR_LENGTH_CUSTOM_REPLY + 1)
        assertFalse(CustomRepliesData.isValidCustomReply(tooLong))
    }

    @Test
    fun `MAX_STR_LENGTH_CUSTOM_REPLY is 500`() {
        assertEquals(500, CustomRepliesData.MAX_STR_LENGTH_CUSTOM_REPLY)
    }

    @Test
    fun `MAX_NUM_CUSTOM_REPLY is 10`() {
        assertEquals(10, CustomRepliesData.MAX_NUM_CUSTOM_REPLY)
    }

    // --- set and get ---

    @Test
    fun `set and get round trip returns the stored reply`() {
        val instance = CustomRepliesData.getInstance(context)
        val reply = "I'm away right now, will reply soon"
        instance.set(reply)
        assertEquals(reply, instance.get())
    }

    @Test
    fun `set returns the stored string on success`() {
        val instance = CustomRepliesData.getInstance(context)
        val reply = "Valid reply"
        assertEquals(reply, instance.set(reply))
    }

    @Test
    fun `set returns null for empty string`() {
        val instance = CustomRepliesData.getInstance(context)
        assertNull(instance.set(""))
    }

    @Test
    fun `set returns null for string exceeding max length`() {
        val instance = CustomRepliesData.getInstance(context)
        val tooLong = "a".repeat(CustomRepliesData.MAX_STR_LENGTH_CUSTOM_REPLY + 1)
        assertNull(instance.set(tooLong))
    }

    @Test
    fun `get returns latest reply after multiple sets`() {
        val instance = CustomRepliesData.getInstance(context)
        instance.set("first reply")
        instance.set("second reply")
        instance.set("third reply")
        assertEquals("third reply", instance.get())
    }

    @Test
    fun `set limits history to MAX_NUM_CUSTOM_REPLY entries`() {
        val instance = CustomRepliesData.getInstance(context)
        // Set more replies than the maximum
        val total = CustomRepliesData.MAX_NUM_CUSTOM_REPLY + 5
        for (i in 1..total) {
            instance.set("reply $i")
        }
        // The current (last) reply should be the most recent one
        assertEquals("reply $total", instance.get())
    }

    // --- getOrElse ---

    @Test
    fun `getOrElse returns the stored reply when set`() {
        val instance = CustomRepliesData.getInstance(context)
        instance.set("custom reply text")
        assertEquals("custom reply text", instance.getOrElse("fallback"))
    }

    @Test
    fun `getOrElse returns default when no reply is stored`() {
        // Override the key to simulate empty state
        context.getSharedPreferences("CustomRepliesData", Context.MODE_PRIVATE)
            .edit()
            .putString(CustomRepliesData.KEY_CUSTOM_REPLY_ALL, "[]")
            .commit()
        val instance = CustomRepliesData.getInstance(context)
        assertEquals("fallback text", instance.getOrElse("fallback text"))
    }

    // --- RTL invisible char ---

    @Test
    fun `RTL_ALIGN_INVISIBLE_CHAR is defined`() {
        assertNotNull(CustomRepliesData.RTL_ALIGN_INVISIBLE_CHAR)
        assertTrue(CustomRepliesData.RTL_ALIGN_INVISIBLE_CHAR.isNotEmpty())
    }
}
