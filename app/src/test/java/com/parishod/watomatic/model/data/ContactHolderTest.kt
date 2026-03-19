package com.parishod.watomatic.model.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ContactHolderTest {

    @Test
    fun `three-arg constructor stores name phone and checked`() {
        val holder = ContactHolder("Alice", "+1234567890", true)
        assertEquals("Alice", holder.contactName)
        assertEquals("+1234567890", holder.phoneNumber)
        assertTrue(holder.isChecked)
    }

    @Test
    fun `three-arg constructor defaults isCustom to false`() {
        val holder = ContactHolder("Alice", "+1234567890", true)
        assertFalse(holder.isCustom)
    }

    @Test
    fun `custom constructor stores name checked and custom`() {
        val holder = ContactHolder("Bob", true, true)
        assertEquals("Bob", holder.contactName)
        assertTrue(holder.isChecked)
        assertTrue(holder.isCustom)
    }

    @Test
    fun `custom constructor sets phoneNumber to null`() {
        val holder = ContactHolder("Bob", false, true)
        assertNull(holder.phoneNumber)
    }

    @Test
    fun `setChecked updates value`() {
        val holder = ContactHolder("Alice", "+1234567890", false)
        assertFalse(holder.isChecked)
        holder.isChecked = true
        assertTrue(holder.isChecked)
    }

    @Test
    fun `setCustom updates value`() {
        val holder = ContactHolder("Alice", "+1234567890", false)
        assertFalse(holder.isCustom)
        holder.isCustom = true
        assertTrue(holder.isCustom)
    }

    @Test
    fun `contactName is immutable after construction`() {
        val holder = ContactHolder("Alice", "+1234567890", false)
        assertEquals("Alice", holder.contactName)
    }

    @Test
    fun `phoneNumber is immutable after construction`() {
        val holder = ContactHolder("Alice", "+1234567890", false)
        assertEquals("+1234567890", holder.phoneNumber)
    }
}
