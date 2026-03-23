package com.parishod.watomatic.network

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.scalars.ScalarsConverterFactory

/**
 * Unit tests for [RetrofitInstance].
 *
 * Verifies base URLs, converter factories, and singleton behavior
 * for all three Retrofit instances (GitHub, OpenAI, Atomatic AI).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class RetrofitInstanceTest {

    // --- Base URL constants ---

    @Test
    fun `OPENAI_BASE_URL ends with slash`() {
        assertTrue(
            "OpenAI base URL must end with /",
            RetrofitInstance.OPENAI_BASE_URL.endsWith("/")
        )
    }

    @Test
    fun `OPENAI_BASE_URL is https`() {
        assertTrue(RetrofitInstance.OPENAI_BASE_URL.startsWith("https://"))
    }

    @Test
    fun `ATOMATIC_AI_BASE_URL ends with slash`() {
        assertTrue(
            "Atomatic AI base URL must end with /",
            RetrofitInstance.ATOMATIC_AI_BASE_URL.endsWith("/")
        )
    }

    @Test
    fun `ATOMATIC_AI_BASE_URL is https`() {
        assertTrue(RetrofitInstance.ATOMATIC_AI_BASE_URL.startsWith("https://"))
    }

    // --- GitHub Retrofit instance ---

    @Test
    fun `getRetrofitInstance returns non-null`() {
        val instance = RetrofitInstance.getRetrofitInstance()
        assertNotNull(instance)
    }

    @Test
    fun `getRetrofitInstance uses GitHub base URL`() {
        val instance = RetrofitInstance.getRetrofitInstance()
        assertEquals("https://api.github.com/", instance.baseUrl().toString())
    }

    @Test
    fun `getRetrofitInstance has ScalarsConverterFactory`() {
        val instance = RetrofitInstance.getRetrofitInstance()
        val factories = instance.converterFactories()
        assertTrue(
            "Should contain ScalarsConverterFactory",
            factories.any { it is ScalarsConverterFactory }
        )
    }

    @Test
    fun `getRetrofitInstance has GsonConverterFactory`() {
        val instance = RetrofitInstance.getRetrofitInstance()
        val factories = instance.converterFactories()
        assertTrue(
            "Should contain GsonConverterFactory",
            factories.any { it is GsonConverterFactory }
        )
    }

    @Test
    fun `getRetrofitInstance returns same singleton`() {
        val first = RetrofitInstance.getRetrofitInstance()
        val second = RetrofitInstance.getRetrofitInstance()
        assertSame(first, second)
    }

    // --- OpenAI Retrofit instance ---

    @Test
    fun `getOpenAIRetrofitInstance returns non-null`() {
        assertNotNull(RetrofitInstance.getOpenAIRetrofitInstance())
    }

    @Test
    fun `getOpenAIRetrofitInstance uses OpenAI base URL`() {
        val instance = RetrofitInstance.getOpenAIRetrofitInstance()
        assertEquals("https://api.openai.com/", instance.baseUrl().toString())
    }

    @Test
    fun `getOpenAIRetrofitInstance has GsonConverterFactory`() {
        val instance = RetrofitInstance.getOpenAIRetrofitInstance()
        assertTrue(
            "Should contain GsonConverterFactory",
            instance.converterFactories().any { it is GsonConverterFactory }
        )
    }

    @Test
    fun `getOpenAIRetrofitInstance does not have ScalarsConverterFactory`() {
        val instance = RetrofitInstance.getOpenAIRetrofitInstance()
        assertFalse(
            "OpenAI instance should not have ScalarsConverterFactory",
            instance.converterFactories().any { it is ScalarsConverterFactory }
        )
    }

    @Test
    fun `getOpenAIRetrofitInstance with custom base URL`() {
        val customUrl = "https://custom.openai.proxy.com/"
        val instance = RetrofitInstance.getOpenAIRetrofitInstance(customUrl)
        assertEquals(customUrl, instance.baseUrl().toString())
    }

    @Test
    fun `getOpenAIRetrofitInstance with custom URL returns different instance each call`() {
        val first = RetrofitInstance.getOpenAIRetrofitInstance("https://a.com/")
        val second = RetrofitInstance.getOpenAIRetrofitInstance("https://b.com/")
        assertNotSame(first, second)
    }

    // --- Atomatic AI Retrofit instance ---

    @Test
    fun `getAtomaticAIRetrofitInstance returns non-null`() {
        assertNotNull(RetrofitInstance.getAtomaticAIRetrofitInstance())
    }

    @Test
    fun `getAtomaticAIRetrofitInstance uses Atomatic base URL`() {
        val instance = RetrofitInstance.getAtomaticAIRetrofitInstance()
        assertEquals(RetrofitInstance.ATOMATIC_AI_BASE_URL, instance.baseUrl().toString())
    }

    @Test
    fun `getAtomaticAIRetrofitInstance has GsonConverterFactory`() {
        val instance = RetrofitInstance.getAtomaticAIRetrofitInstance()
        assertTrue(
            "Should contain GsonConverterFactory",
            instance.converterFactories().any { it is GsonConverterFactory }
        )
    }

    // --- Error parsing ---

    @Test
    fun `parseOpenAIError returns null for null error body`() {
        val response = retrofit2.Response.success<String>("ok")
        assertNull(RetrofitInstance.parseOpenAIError(response))
    }

    @Test
    fun `parseAtomaticAIError returns null for null error body`() {
        val response = retrofit2.Response.success<String>("ok")
        assertNull(RetrofitInstance.parseAtomaticAIError(response))
    }
}
