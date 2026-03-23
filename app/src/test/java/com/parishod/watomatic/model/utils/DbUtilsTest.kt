package com.parishod.watomatic.model.utils

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.parishod.watomatic.model.CustomRepliesData
import com.parishod.watomatic.model.logs.MessageLogsDB
import com.parishod.watomatic.model.preferences.PreferencesManager
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class DbUtilsTest {

    private lateinit var context: Context
    private lateinit var dbUtils: DbUtils

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PreferencesManager.resetInstance()
        CustomRepliesData.resetInstance()
        dbUtils = DbUtils(context)
    }

    @After
    fun tearDown() {
        PreferencesManager.resetInstance()
        CustomRepliesData.resetInstance()
        // Reset the database singleton via reflection
        try {
            val field = MessageLogsDB::class.java.getDeclaredField("_instance")
            field.isAccessible = true
            val db = field.get(null) as? MessageLogsDB
            db?.close()
            field.set(null, null)
        } catch (_: Exception) {
            // Ignore if reflection fails
        }
    }

    // --- getNumReplies ---

    @Test
    fun `getNumReplies returns 0 when no logs exist`() {
        val count = dbUtils.getNumReplies()
        assertEquals(0L, count)
    }

    // --- getLastRepliedTime ---

    @Test
    fun `getLastRepliedTime returns 0 for null title`() {
        val result = dbUtils.getLastRepliedTime("com.whatsapp", null)
        assertEquals(0L, result)
    }

    @Test
    fun `getLastRepliedTime returns 0 when no logs exist`() {
        val result = dbUtils.getLastRepliedTime("com.whatsapp", "John")
        assertEquals(0L, result)
    }

    // --- getFirstRepliedTime ---

    @Test
    fun `getFirstRepliedTime returns 0 when no logs exist`() {
        val result = dbUtils.getFirstRepliedTime()
        assertEquals(0L, result)
    }

    // --- purgeMessageLogs ---

    @Test
    fun `purgeMessageLogs does not throw when no logs exist`() {
        // Should not throw
        dbUtils.purgeMessageLogs()
        assertEquals(0L, dbUtils.getNumReplies())
    }

    // --- Constructor ---

    @Test
    fun `DbUtils can be constructed with context`() {
        val utils = DbUtils(context)
        // Basic smoke test — doesn't crash
        assertTrue(utils.getNumReplies() >= 0)
    }
}
