package com.parishod.watomatic.model

import com.parishod.watomatic.model.utils.Constants
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConstantsTest {

    @Test
    fun `SUPPORTED_APPS contains WhatsApp`() {
        assertTrue(Constants.SUPPORTED_APPS.any { it.packageName == "com.whatsapp" })
    }

    @Test
    fun `SUPPORTED_APPS contains Telegram`() {
        assertTrue(Constants.SUPPORTED_APPS.any { it.packageName == "org.telegram.messenger" })
    }

    @Test
    fun `SUPPORTED_APPS contains Facebook Messenger`() {
        assertTrue(Constants.SUPPORTED_APPS.any { it.packageName == "com.facebook.orca" })
    }

    @Test
    fun `SUPPORTED_APPS contains Facebook Messenger Lite`() {
        assertTrue(Constants.SUPPORTED_APPS.any { it.packageName == "com.facebook.mlite" })
    }

    @Test
    fun `SUPPORTED_APPS contains LinkedIn`() {
        assertTrue(Constants.SUPPORTED_APPS.any { it.packageName == "com.linkedin.android" })
    }

    @Test
    fun `SUPPORTED_APPS has five entries`() {
        assertEquals(5, Constants.SUPPORTED_APPS.size)
    }

    @Test
    fun `PROVIDER_URLS contains OpenAI`() {
        assertNotNull(Constants.PROVIDER_URLS["OpenAI"])
        assertEquals("https://api.openai.com/", Constants.PROVIDER_URLS["OpenAI"])
    }

    @Test
    fun `PROVIDER_URLS contains Claude`() {
        assertNotNull(Constants.PROVIDER_URLS["Claude"])
        assertEquals("https://api.anthropic.com/", Constants.PROVIDER_URLS["Claude"])
    }

    @Test
    fun `PROVIDER_URLS contains Gemini`() {
        assertNotNull(Constants.PROVIDER_URLS["Gemini"])
        assertTrue(Constants.PROVIDER_URLS["Gemini"]!!.contains("googleapis.com"))
    }

    @Test
    fun `PROVIDER_URLS has all expected providers`() {
        val expected = listOf("OpenAI", "Claude", "Grok", "Gemini", "DeepSeek", "Mistral")
        for (provider in expected) {
            assertTrue("$provider missing from PROVIDER_URLS", Constants.PROVIDER_URLS.containsKey(provider))
        }
    }

    @Test
    fun `all PROVIDER_URLS values end with slash`() {
        for ((provider, url) in Constants.PROVIDER_URLS) {
            assertTrue("URL for $provider should end with /", url.endsWith("/"))
        }
    }

    @Test
    fun `DEFAULT_LLM_PROMPT is not empty`() {
        assertTrue(Constants.DEFAULT_LLM_PROMPT.isNotEmpty())
    }

    @Test
    fun `DEFAULT_LLM_MODEL is not empty`() {
        assertTrue(Constants.DEFAULT_LLM_MODEL.isNotEmpty())
    }

    @Test
    fun `DEFAULT_LLM_PROMPT contains instruction not to reveal being AI`() {
        assertTrue(Constants.DEFAULT_LLM_PROMPT.contains("AI", ignoreCase = true))
    }

    @Test
    fun `LinkedIn is marked as experimental`() {
        val linkedin = Constants.SUPPORTED_APPS.first { it.packageName == "com.linkedin.android" }
        assertTrue(linkedin.isExperimental)
    }

    @Test
    fun `non-LinkedIn apps are not experimental`() {
        val nonExperimental = Constants.SUPPORTED_APPS.filter { it.packageName != "com.linkedin.android" }
        assertFalse(nonExperimental.any { it.isExperimental })
    }

    @Test
    fun `MIN_DAYS is 0`() {
        assertEquals(0, Constants.MIN_DAYS)
    }

    @Test
    fun `MAX_DAYS is 30`() {
        assertEquals(30, Constants.MAX_DAYS)
    }

    @Test
    fun `MIN_REPLIES_TO_ASK_APP_RATING is positive`() {
        assertTrue(Constants.MIN_REPLIES_TO_ASK_APP_RATING > 0)
    }

    @Test
    fun `EMAIL_ADDRESS is set`() {
        assertTrue(Constants.EMAIL_ADDRESS.isNotEmpty())
        assertTrue(Constants.EMAIL_ADDRESS.contains("@"))
    }
}
