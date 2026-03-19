package com.parishod.watomatic.model

import android.content.Context
import android.text.Editable
import android.text.SpannableStringBuilder
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

    // --- getTextToSendOrElse ---

    @Test
    fun `getTextToSendOrElse returns custom reply when AI not enabled`() {
        val instance = CustomRepliesData.getInstance(context)
        instance.set("My custom reply")
        val result = instance.getTextToSendOrElse()
        assertEquals("My custom reply", result)
    }

    @Test
    fun `getTextToSendOrElse returns non-empty string when automatic AI enabled`() {
        val prefsInstance = PreferencesManager.getPreferencesInstance(context)
        prefsInstance.setEnableAutomaticAiReplies(true)
        val instance = CustomRepliesData.getInstance(context)
        val result = instance.getTextToSendOrElse()
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `getTextToSendOrElse returns non-empty string when BYOK enabled`() {
        val prefsInstance = PreferencesManager.getPreferencesInstance(context)
        prefsInstance.setEnableByokReplies(true)
        val instance = CustomRepliesData.getInstance(context)
        val result = instance.getTextToSendOrElse()
        assertNotNull(result)
        assertTrue(result.isNotEmpty())
    }

    @Test
    fun `getTextToSendOrElse appends RTL attribution when attribution enabled`() {
        val prefsInstance = PreferencesManager.getPreferencesInstance(context)
        prefsInstance.setAppendWatomaticAttribution(true)
        val instance = CustomRepliesData.getInstance(context)
        instance.set("My reply")
        val result = instance.getTextToSendOrElse()
        assertTrue(result.contains(CustomRepliesData.RTL_ALIGN_INVISIBLE_CHAR))
    }

    @Test
    fun `getTextToSendOrElse does not append attribution when attribution disabled`() {
        val prefsInstance = PreferencesManager.getPreferencesInstance(context)
        prefsInstance.setAppendWatomaticAttribution(false)
        val instance = CustomRepliesData.getInstance(context)
        instance.set("My reply")
        val result = instance.getTextToSendOrElse()
        assertFalse(result.contains(CustomRepliesData.RTL_ALIGN_INVISIBLE_CHAR))
    }

    @Test
    fun `getTextToSendOrElse AI result does not equal custom reply text`() {
        val prefsInstance = PreferencesManager.getPreferencesInstance(context)
        prefsInstance.setEnableAutomaticAiReplies(true)
        val instance = CustomRepliesData.getInstance(context)
        instance.set("My very specific custom reply 12345")
        val aiResult = instance.getTextToSendOrElse()
        // When AI is enabled, should return AI default message, not the stored custom reply
        assertFalse(aiResult == "My very specific custom reply 12345")
    }

    // --- set(Editable) overload ---

    @Test
    fun `set with null Editable returns null`() {
        val instance = CustomRepliesData.getInstance(context)
        assertNull(instance.set(null as Editable?))
    }

    @Test
    fun `set with valid Editable stores and returns string`() {
        val instance = CustomRepliesData.getInstance(context)
        val editable: Editable = SpannableStringBuilder("Valid editable reply")
        val result = instance.set(editable)
        assertEquals("Valid editable reply", result)
        assertEquals("Valid editable reply", instance.get())
    }

    @Test
    fun `set with empty Editable returns null`() {
        val instance = CustomRepliesData.getInstance(context)
        val editable: Editable = SpannableStringBuilder("")
        assertNull(instance.set(editable))
    }

    // --- isValidCustomReply(Editable) overload ---

    @Test
    fun `isValidCustomReply returns false for null Editable`() {
        assertFalse(CustomRepliesData.isValidCustomReply(null as Editable?))
    }

    @Test
    fun `isValidCustomReply returns true for valid Editable`() {
        val editable: Editable = SpannableStringBuilder("Valid input")
        assertTrue(CustomRepliesData.isValidCustomReply(editable))
    }

    @Test
    fun `isValidCustomReply returns false for empty Editable`() {
        val editable: Editable = SpannableStringBuilder("")
        assertFalse(CustomRepliesData.isValidCustomReply(editable))
    }

    @Test
    fun `isValidCustomReply returns false for Editable exceeding max length`() {
        val tooLong: Editable = SpannableStringBuilder("a".repeat(CustomRepliesData.MAX_STR_LENGTH_CUSTOM_REPLY + 1))
        assertFalse(CustomRepliesData.isValidCustomReply(tooLong))
    }

    @Test
    fun `isValidCustomReply returns true for Editable at max length`() {
        val atMax: Editable = SpannableStringBuilder("a".repeat(CustomRepliesData.MAX_STR_LENGTH_CUSTOM_REPLY))
        assertTrue(CustomRepliesData.isValidCustomReply(atMax))
    }

    // --- Edge cases ---

    @Test
    fun `get returns non-null default when fresh instance created`() {
        val instance = CustomRepliesData.getInstance(context)
        // init() sets a default reply, so get() returns non-null on fresh instance
        assertNotNull(instance.get())
    }

    @Test
    fun `set with MAX_STR_LENGTH_CUSTOM_REPLY characters succeeds`() {
        val instance = CustomRepliesData.getInstance(context)
        val longReply = "a".repeat(CustomRepliesData.MAX_STR_LENGTH_CUSTOM_REPLY)
        val result = instance.set(longReply)
        assertEquals(longReply, result)
        assertEquals(longReply, instance.get())
    }
}
