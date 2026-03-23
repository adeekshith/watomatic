package com.parishod.watomatic.model.logs

import org.junit.Assert.assertEquals
import org.junit.Test

class AppPackageTest {

    @Test
    fun `constructor stores packageName`() {
        val pkg = AppPackage("com.whatsapp")
        assertEquals("com.whatsapp", pkg.packageName)
    }

    @Test
    fun `index defaults to 0`() {
        val pkg = AppPackage("com.whatsapp")
        assertEquals(0, pkg.index)
    }

    @Test
    fun `setIndex updates value`() {
        val pkg = AppPackage("com.whatsapp")
        pkg.index = 42
        assertEquals(42, pkg.index)
    }

    @Test
    fun `setPackageName updates value`() {
        val pkg = AppPackage("com.whatsapp")
        pkg.packageName = "org.telegram"
        assertEquals("org.telegram", pkg.packageName)
    }
}
