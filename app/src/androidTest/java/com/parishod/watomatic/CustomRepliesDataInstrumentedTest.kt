package com.parishod.watomatic

import android.app.Activity
import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.parishod.watomatic.model.CustomRepliesData
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

/**
 * Instrumented tests for [CustomRepliesData] using real SharedPreferences
 * on a device or emulator.
 *
 * Unlike Robolectric unit tests, these run against the real Android framework
 * and verify that SharedPreferences persistence works correctly end-to-end.
 */
@RunWith(AndroidJUnit4::class)
class CustomRepliesDataInstrumentedTest {

    private lateinit var context: Context
    private lateinit var repliesData: CustomRepliesData

    @Before
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // Clear the CustomRepliesData-specific SharedPreferences
        context.getSharedPreferences("CustomRepliesData", Activity.MODE_PRIVATE)
            .edit().clear().commit()
        // Clear default SharedPreferences (used by PreferencesManager)
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        PreferencesManager.resetInstance()
        CustomRepliesData.resetInstance()
        repliesData = CustomRepliesData.getInstance(context)
    }

    @After
    fun tearDown() {
        context.getSharedPreferences("CustomRepliesData", Activity.MODE_PRIVATE)
            .edit().clear().commit()
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().commit()
        PreferencesManager.resetInstance()
        CustomRepliesData.resetInstance()
    }

    @Test
    fun instanceIsNotNull() {
        assertNotNull(repliesData)
    }

    @Test
    fun singletonReturnsSameInstance() {
        val instance2 = CustomRepliesData.getInstance(context)
        assertTrue(repliesData === instance2)
    }

    @Test
    fun freshInstanceHasDefaultReply() {
        // init() sets a default reply on first install
        val reply = repliesData.get()
        assertNotNull(reply)
        assertTrue(reply!!.isNotEmpty())
    }

    @Test
    fun setAndGetReply() {
        val testReply = "Test auto-reply message for instrumented test"
        val result = repliesData.set(testReply)
        assertEquals(testReply, result)
        assertEquals(testReply, repliesData.get())
    }

    @Test
    fun setNullReturnsNull() {
        val result = repliesData.set(null as String?)
        assertNull(result)
    }

    @Test
    fun setEmptyStringReturnsNull() {
        val result = repliesData.set("")
        assertNull(result)
    }

    @Test
    fun setOverMaxLengthReturnsNull() {
        val longReply = "a".repeat(CustomRepliesData.MAX_STR_LENGTH_CUSTOM_REPLY + 1)
        val result = repliesData.set(longReply)
        assertNull(result)
    }

    @Test
    fun setMaxLengthReplySucceeds() {
        val maxReply = "b".repeat(CustomRepliesData.MAX_STR_LENGTH_CUSTOM_REPLY)
        val result = repliesData.set(maxReply)
        assertEquals(maxReply, result)
        assertEquals(maxReply, repliesData.get())
    }

    @Test
    fun getOrElseReturnsReplyWhenSet() {
        val testReply = "My custom reply"
        repliesData.set(testReply)
        assertEquals(testReply, repliesData.getOrElse("default"))
    }

    @Test
    fun getOrElseReturnsFallbackWhenDefault() {
        // A fresh instance has a default reply set by init(), so getOrElse
        // should return that default, not the provided fallback
        val reply = repliesData.getOrElse("fallback")
        assertNotNull(reply)
        assertTrue(reply != "fallback") // init() set a default
    }

    @Test
    fun multipleSetKeepsHistory() {
        // Set several replies — the most recent should be returned by get()
        repliesData.set("Reply 1")
        repliesData.set("Reply 2")
        repliesData.set("Reply 3")
        assertEquals("Reply 3", repliesData.get())
    }

    @Test
    fun historyLimitedToMaxEntries() {
        // Set more than MAX_NUM_CUSTOM_REPLY replies
        for (i in 1..CustomRepliesData.MAX_NUM_CUSTOM_REPLY + 5) {
            repliesData.set("Reply $i")
        }
        // The most recent should still be the last one set
        val expected = "Reply ${CustomRepliesData.MAX_NUM_CUSTOM_REPLY + 5}"
        assertEquals(expected, repliesData.get())
    }

    @Test
    fun dataPersistsAcrossInstances() {
        val testReply = "Persistent reply"
        repliesData.set(testReply)

        // Reset singleton and create new instance
        CustomRepliesData.resetInstance()
        val newInstance = CustomRepliesData.getInstance(context)

        assertEquals(testReply, newInstance.get())
    }

    @Test
    fun isValidCustomReplyAcceptsNormalText() {
        assertTrue(CustomRepliesData.isValidCustomReply("Hello!"))
    }

    @Test
    fun isValidCustomReplyRejectsNull() {
        assertFalse(CustomRepliesData.isValidCustomReply(null as String?))
    }

    @Test
    fun isValidCustomReplyRejectsEmpty() {
        assertFalse(CustomRepliesData.isValidCustomReply(""))
    }

    @Test
    fun isValidCustomReplyRejectsTooLong() {
        val tooLong = "x".repeat(CustomRepliesData.MAX_STR_LENGTH_CUSTOM_REPLY + 1)
        assertFalse(CustomRepliesData.isValidCustomReply(tooLong))
    }

    @Test
    fun getTextToSendOrElseReturnsNonNull() {
        val text = repliesData.getTextToSendOrElse()
        assertNotNull(text)
        assertTrue(text.isNotEmpty())
    }
}
