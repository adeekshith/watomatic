package com.parishod.watomatic.model.utils

import android.app.Notification
import android.os.Bundle
import android.service.notification.StatusBarNotification
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class NotificationUtilsTest {

    private lateinit var mockSbn: StatusBarNotification

    @Before
    fun setUp() {
        mockSbn = mock()
    }

    // --- isNewNotification ---

    @Test
    fun `isNewNotification returns true when notification when is 0`() {
        val notification = Notification()
        notification.`when` = 0
        whenever(mockSbn.notification).thenReturn(notification)

        assertTrue(NotificationUtils.isNewNotification(mockSbn))
    }

    @Test
    fun `isNewNotification returns true for notification within 2 minutes`() {
        val notification = Notification()
        notification.`when` = System.currentTimeMillis() - 60_000L // 1 minute ago
        whenever(mockSbn.notification).thenReturn(notification)

        assertTrue(NotificationUtils.isNewNotification(mockSbn))
    }

    @Test
    fun `isNewNotification returns true for very recent notification`() {
        val notification = Notification()
        notification.`when` = System.currentTimeMillis() - 1_000L // 1 second ago
        whenever(mockSbn.notification).thenReturn(notification)

        assertTrue(NotificationUtils.isNewNotification(mockSbn))
    }

    @Test
    fun `isNewNotification returns false for notification older than 2 minutes`() {
        val notification = Notification()
        notification.`when` = System.currentTimeMillis() - (3 * 60 * 1_000L) // 3 minutes ago
        whenever(mockSbn.notification).thenReturn(notification)

        assertFalse(NotificationUtils.isNewNotification(mockSbn))
    }

    @Test
    fun `isNewNotification returns false for very old notification`() {
        val notification = Notification()
        notification.`when` = System.currentTimeMillis() - (60 * 60 * 1_000L) // 1 hour ago
        whenever(mockSbn.notification).thenReturn(notification)

        assertFalse(NotificationUtils.isNewNotification(mockSbn))
    }

    // --- getTitle for non-group conversations ---

    @Test
    fun `getTitle returns android title for non-group conversation`() {
        val extras = Bundle()
        extras.putBoolean("android.isGroupConversation", false)
        extras.putString("android.title", "John Doe")

        val notification = Notification()
        notification.extras = extras
        whenever(mockSbn.notification).thenReturn(notification)

        assertEquals("John Doe", NotificationUtils.getTitle(mockSbn))
    }

    @Test
    fun `getTitle returns null when android title is not set for non-group`() {
        val extras = Bundle()
        extras.putBoolean("android.isGroupConversation", false)
        // No android.title set

        val notification = Notification()
        notification.extras = extras
        whenever(mockSbn.notification).thenReturn(notification)

        assertNull(NotificationUtils.getTitle(mockSbn))
    }

    // --- getTitle for group conversations ---

    @Test
    fun `getTitle returns hiddenConversationTitle for group conversation`() {
        val extras = Bundle()
        extras.putBoolean("android.isGroupConversation", true)
        extras.putString("android.hiddenConversationTitle", "Family Group")

        val notification = Notification()
        notification.extras = extras
        whenever(mockSbn.notification).thenReturn(notification)

        assertEquals("Family Group", NotificationUtils.getTitle(mockSbn))
    }

    @Test
    fun `getTitle extracts group name before colon when hiddenConversationTitle is null`() {
        val extras = Bundle()
        extras.putBoolean("android.isGroupConversation", true)
        extras.putString("android.hiddenConversationTitle", null)
        extras.putString("android.title", "Family Group: John")

        val notification = Notification()
        notification.extras = extras
        whenever(mockSbn.notification).thenReturn(notification)

        assertEquals("Family Group", NotificationUtils.getTitle(mockSbn))
    }

    @Test
    fun `getTitle returns full title when no colon in group title`() {
        val extras = Bundle()
        extras.putBoolean("android.isGroupConversation", true)
        extras.putString("android.hiddenConversationTitle", null)
        extras.putString("android.title", "WorkTeam")

        val notification = Notification()
        notification.extras = extras
        whenever(mockSbn.notification).thenReturn(notification)

        assertEquals("WorkTeam", NotificationUtils.getTitle(mockSbn))
    }

    // --- getTitleRaw ---

    @Test
    fun `getTitleRaw returns raw android title string`() {
        val extras = Bundle()
        extras.putString("android.title", "Raw Title Value")

        val notification = Notification()
        notification.extras = extras
        whenever(mockSbn.notification).thenReturn(notification)

        assertEquals("Raw Title Value", NotificationUtils.getTitleRaw(mockSbn))
    }

    @Test
    fun `getTitleRaw returns null when android title not set`() {
        val extras = Bundle()
        // No android.title

        val notification = Notification()
        notification.extras = extras
        whenever(mockSbn.notification).thenReturn(notification)

        assertNull(NotificationUtils.getTitleRaw(mockSbn))
    }
}
