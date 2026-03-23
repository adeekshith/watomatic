package com.parishod.watomatic.model

import android.os.Parcel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class GithubReleaseNotesTest {

    @Test
    fun `no-arg constructor defaults all fields to null`() {
        val notes = GithubReleaseNotes()
        assertNull(notes.id)
        assertNull(notes.tagName)
        assertNull(notes.body)
    }

    @Test
    fun `setId and getId round-trip`() {
        val notes = GithubReleaseNotes()
        notes.id = 12345
        assertEquals(12345, notes.id)
    }

    @Test
    fun `setTagName and getTagName round-trip`() {
        val notes = GithubReleaseNotes()
        notes.tagName = "v1.35"
        assertEquals("v1.35", notes.tagName)
    }

    @Test
    fun `setBody and getBody round-trip`() {
        val notes = GithubReleaseNotes()
        notes.body = "Bug fixes and performance improvements."
        assertEquals("Bug fixes and performance improvements.", notes.body)
    }

    @Test
    fun `writeToParcel and readFromParcel preserves all fields`() {
        val original = GithubReleaseNotes()
        original.id = 99
        original.tagName = "v2.0"
        original.body = "New feature release"

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = GithubReleaseNotes()
        restored.readFromParcel(parcel)
        parcel.recycle()

        assertEquals(99, restored.id)
        assertEquals("v2.0", restored.tagName)
        assertEquals("New feature release", restored.body)
    }

    @Test
    fun `CREATOR createFromParcel restores object`() {
        val original = GithubReleaseNotes()
        original.id = 77
        original.tagName = "v1.0"
        original.body = "Initial release"

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = GithubReleaseNotes.CREATOR.createFromParcel(parcel)
        parcel.recycle()

        assertEquals(77, restored.id)
        assertEquals("v1.0", restored.tagName)
        assertEquals("Initial release", restored.body)
    }

    @Test
    fun `CREATOR newArray creates array of requested size`() {
        val array = GithubReleaseNotes.CREATOR.newArray(3)
        assertNotNull(array)
        assertEquals(3, array.size)
    }

    @Test
    fun `describeContents returns 0`() {
        val notes = GithubReleaseNotes()
        assertEquals(0, notes.describeContents())
    }

    @Test
    fun `writeToParcel handles null fields`() {
        val original = GithubReleaseNotes()
        // All fields remain null

        val parcel = Parcel.obtain()
        original.writeToParcel(parcel, 0)
        parcel.setDataPosition(0)

        val restored = GithubReleaseNotes.CREATOR.createFromParcel(parcel)
        parcel.recycle()

        assertNull(restored.id)
        assertNull(restored.tagName)
        assertNull(restored.body)
    }
}
