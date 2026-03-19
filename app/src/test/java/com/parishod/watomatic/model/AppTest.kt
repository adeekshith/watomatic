package com.parishod.watomatic.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppTest {

    @Test
    fun `constructor stores name and packageName`() {
        val app = App(name = "WhatsApp", packageName = "com.whatsapp")
        assertEquals("WhatsApp", app.name)
        assertEquals("com.whatsapp", app.packageName)
    }

    @Test
    fun `isExperimental defaults to false`() {
        val app = App(name = "WhatsApp", packageName = "com.whatsapp")
        assertFalse(app.isExperimental)
    }

    @Test
    fun `isExperimental can be set to true`() {
        val app = App(name = "Signal", packageName = "org.signal", isExperimental = true)
        assertTrue(app.isExperimental)
    }

    @Test
    fun `two Apps with same values are equal`() {
        val app1 = App("WhatsApp", "com.whatsapp", false)
        val app2 = App("WhatsApp", "com.whatsapp", false)
        assertEquals(app1, app2)
        assertEquals(app1.hashCode(), app2.hashCode())
    }

    @Test
    fun `two Apps with different names are not equal`() {
        val app1 = App("WhatsApp", "com.whatsapp")
        val app2 = App("Telegram", "com.whatsapp")
        assertNotEquals(app1, app2)
    }

    @Test
    fun `two Apps with different packageNames are not equal`() {
        val app1 = App("WhatsApp", "com.whatsapp")
        val app2 = App("WhatsApp", "org.telegram")
        assertNotEquals(app1, app2)
    }

    @Test
    fun `two Apps with different isExperimental are not equal`() {
        val app1 = App("Signal", "org.signal", false)
        val app2 = App("Signal", "org.signal", true)
        assertNotEquals(app1, app2)
    }

    @Test
    fun `copy creates modified instance`() {
        val original = App("WhatsApp", "com.whatsapp", false)
        val copy = original.copy(isExperimental = true)
        assertEquals("WhatsApp", copy.name)
        assertEquals("com.whatsapp", copy.packageName)
        assertTrue(copy.isExperimental)
    }

    @Test
    fun `toString includes all fields`() {
        val app = App("WhatsApp", "com.whatsapp", true)
        val str = app.toString()
        assertTrue(str.contains("WhatsApp"))
        assertTrue(str.contains("com.whatsapp"))
        assertTrue(str.contains("true"))
    }
}
