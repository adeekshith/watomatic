package com.parishod.watomatic.model.utils

import java.net.URI

/**
 * Utility for validating URLs used in API base URL configuration.
 * Ensures URLs have a valid http/https scheme and a parseable host.
 */
object UrlValidator {

    /**
     * Validates that the given URL is well-formed and uses http or https scheme.
     *
     * @param url The URL string to validate.
     * @return true if the URL is valid and usable as a Retrofit base URL.
     */
    fun isValidUrl(url: String): Boolean {
        if (url.isBlank()) return false
        // Must start with http:// or https://
        if (!url.startsWith("http://") && !url.startsWith("https://")) return false
        return try {
            val uri = URI(url)
            // Must have a valid host
            !uri.host.isNullOrBlank()
        } catch (e: Exception) {
            false
        }
    }
}
