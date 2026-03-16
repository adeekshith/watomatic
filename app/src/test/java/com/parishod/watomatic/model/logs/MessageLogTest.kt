package com.parishod.watomatic.model.logs

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MessageLogTest {

    private fun buildLog(
        index: Int = 1,
        title: String = "John Doe",
        arrivedTime: Long = 1_000_000L,
        repliedMsg: String = "I'm busy",
        replyTime: Long = 1_001_000L
    ) = MessageLog(index, title, arrivedTime, repliedMsg, replyTime)

    @Test
    fun `constructor stores index`() {
        val log = buildLog(index = 7)
        assertEquals(7, log.index)
    }

    @Test
    fun `constructor stores notifTitle`() {
        val log = buildLog(title = "Alice")
        assertEquals("Alice", log.notifTitle)
    }

    @Test
    fun `constructor stores notifArrivedTime`() {
        val log = buildLog(arrivedTime = 9_999_000L)
        assertEquals(9_999_000L, log.notifArrivedTime)
    }

    @Test
    fun `constructor stores notifRepliedMsg`() {
        val log = buildLog(repliedMsg = "Will call you back")
        assertEquals("Will call you back", log.notifRepliedMsg)
    }

    @Test
    fun `constructor stores notifReplyTime`() {
        val log = buildLog(replyTime = 5_000_000L)
        assertEquals(5_000_000L, log.notifReplyTime)
    }

    @Test
    fun `constructor sets notifIsReplied to true`() {
        val log = buildLog()
        assertTrue(log.isNotifIsReplied)
    }

    @Test
    fun `constructor generates non-null notifId`() {
        val log = buildLog()
        assertNotNull(log.notifId)
        assertTrue(log.notifId.isNotEmpty())
    }

    @Test
    fun `each MessageLog gets a unique notifId`() {
        val log1 = buildLog()
        val log2 = buildLog()
        assertTrue(log1.notifId != log2.notifId)
    }

    @Test
    fun `setId persists value`() {
        val log = buildLog()
        log.id = 42
        assertEquals(42, log.id)
    }

    @Test
    fun `setIndex persists value`() {
        val log = buildLog(index = 1)
        log.index = 99
        assertEquals(99, log.index)
    }

    @Test
    fun `setNotifId persists value`() {
        val log = buildLog()
        log.notifId = "custom-uuid"
        assertEquals("custom-uuid", log.notifId)
    }

    @Test
    fun `setNotifTitle persists value`() {
        val log = buildLog(title = "original")
        log.notifTitle = "updated"
        assertEquals("updated", log.notifTitle)
    }

    @Test
    fun `setNotifArrivedTime persists value`() {
        val log = buildLog(arrivedTime = 0L)
        log.notifArrivedTime = 12_345_678L
        assertEquals(12_345_678L, log.notifArrivedTime)
    }

    @Test
    fun `setNotifIsReplied persists false`() {
        val log = buildLog()
        log.isNotifIsReplied = false
        assertEquals(false, log.isNotifIsReplied)
    }

    @Test
    fun `setNotifRepliedMsg persists value`() {
        val log = buildLog(repliedMsg = "original reply")
        log.notifRepliedMsg = "updated reply"
        assertEquals("updated reply", log.notifRepliedMsg)
    }

    @Test
    fun `setNotifReplyTime persists value`() {
        val log = buildLog(replyTime = 0L)
        log.notifReplyTime = 9_876_543L
        assertEquals(9_876_543L, log.notifReplyTime)
    }

    @Test
    fun `notifId is a valid UUID format`() {
        val log = buildLog()
        // UUID format: xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
        val uuidRegex = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$")
        assertTrue(log.notifId.matches(uuidRegex))
    }
}
