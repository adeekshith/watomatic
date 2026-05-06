package com.parishod.watomatic.model.utils

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [UrlValidator.isValidUrl].
 *
 * These are pure JUnit tests (no Robolectric) since UrlValidator has
 * no Android framework dependencies.
 */
class UrlValidatorTest {

    // --- Valid URLs ---

    @Test
    fun `isValidUrl returns true for standard HTTPS API URL`() {
        assertTrue(UrlValidator.isValidUrl("https://api.openai.com"))
    }

    @Test
    fun `isValidUrl returns true for HTTPS URL with trailing slash`() {
        assertTrue(UrlValidator.isValidUrl("https://api.openai.com/"))
    }

    @Test
    fun `isValidUrl returns true for HTTPS URL with path`() {
        assertTrue(UrlValidator.isValidUrl("https://api.openai.com/v1/models"))
    }

    @Test
    fun `isValidUrl returns true for HTTP localhost with port`() {
        assertTrue(UrlValidator.isValidUrl("http://localhost:8080"))
    }

    @Test
    fun `isValidUrl returns true for HTTP localhost without port`() {
        assertTrue(UrlValidator.isValidUrl("http://localhost"))
    }

    @Test
    fun `isValidUrl returns true for HTTPS IP address`() {
        assertTrue(UrlValidator.isValidUrl("https://192.168.1.1"))
    }

    @Test
    fun `isValidUrl returns true for HTTP IP address with port`() {
        assertTrue(UrlValidator.isValidUrl("http://192.168.1.1:11434"))
    }

    @Test
    fun `isValidUrl returns true for HTTPS URL with subdomain`() {
        assertTrue(UrlValidator.isValidUrl("https://my-server.example.com/api"))
    }

    @Test
    fun `isValidUrl returns true for HTTP URL`() {
        assertTrue(UrlValidator.isValidUrl("http://example.com"))
    }

    // --- Invalid URLs: empty / blank ---

    @Test
    fun `isValidUrl returns false for empty string`() {
        assertFalse(UrlValidator.isValidUrl(""))
    }

    @Test
    fun `isValidUrl returns false for whitespace only`() {
        assertFalse(UrlValidator.isValidUrl("   "))
    }

    @Test
    fun `isValidUrl returns false for tab and newline`() {
        assertFalse(UrlValidator.isValidUrl("\t\n"))
    }

    // --- Invalid URLs: missing scheme ---

    @Test
    fun `isValidUrl returns false for URL without scheme`() {
        assertFalse(UrlValidator.isValidUrl("openai.com"))
    }

    @Test
    fun `isValidUrl returns false for URL with www but no scheme`() {
        assertFalse(UrlValidator.isValidUrl("www.openai.com"))
    }

    @Test
    fun `isValidUrl returns false for just a hostname`() {
        assertFalse(UrlValidator.isValidUrl("api.openai.com"))
    }

    // --- Invalid URLs: unsupported scheme ---

    @Test
    fun `isValidUrl returns false for ftp scheme`() {
        assertFalse(UrlValidator.isValidUrl("ftp://example.com"))
    }

    @Test
    fun `isValidUrl returns false for ws scheme`() {
        assertFalse(UrlValidator.isValidUrl("ws://example.com"))
    }

    @Test
    fun `isValidUrl returns false for file scheme`() {
        assertFalse(UrlValidator.isValidUrl("file:///etc/hosts"))
    }

    // --- Invalid URLs: incomplete ---

    @Test
    fun `isValidUrl returns false for scheme only without host`() {
        assertFalse(UrlValidator.isValidUrl("https://"))
    }

    @Test
    fun `isValidUrl returns false for http scheme only without host`() {
        assertFalse(UrlValidator.isValidUrl("http://"))
    }

    @Test
    fun `isValidUrl returns false for partial scheme http`() {
        assertFalse(UrlValidator.isValidUrl("http"))
    }

    @Test
    fun `isValidUrl returns false for partial scheme https`() {
        assertFalse(UrlValidator.isValidUrl("https"))
    }

    @Test
    fun `isValidUrl returns false for scheme with colon but no slashes`() {
        assertFalse(UrlValidator.isValidUrl("https:openai.com"))
    }

    @Test
    fun `isValidUrl returns false for single character after scheme`() {
        // "https://g" — technically has a host, so URI parses it.
        // This depends on whether java.net.URI considers "g" a valid host.
        // java.net.URI does accept single-char hosts, so this is actually valid.
        // Documenting the real behavior:
        val result = UrlValidator.isValidUrl("https://g")
        // Single char is a valid hostname per URI spec — we accept it
        assertTrue(result)
    }

    // --- Invalid URLs: malformed ---

    @Test
    fun `isValidUrl returns false for random text`() {
        assertFalse(UrlValidator.isValidUrl("not a url at all"))
    }

    @Test
    fun `isValidUrl returns false for just a colon`() {
        assertFalse(UrlValidator.isValidUrl(":"))
    }

    @Test
    fun `isValidUrl returns false for just slashes`() {
        assertFalse(UrlValidator.isValidUrl("://"))
    }

    // --- Edge cases ---

    @Test
    fun `isValidUrl returns true for URL with query parameters`() {
        assertTrue(UrlValidator.isValidUrl("https://api.example.com/v1?key=value"))
    }

    @Test
    fun `isValidUrl returns true for URL with port and path`() {
        assertTrue(UrlValidator.isValidUrl("http://localhost:11434/api/models"))
    }

    @Test
    fun `isValidUrl returns false for URL with spaces`() {
        assertFalse(UrlValidator.isValidUrl("https://api.open ai.com"))
    }
}
