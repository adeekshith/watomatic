package com.parishod.watomatic.model.utils

import com.parishod.watomatic.network.model.openai.ModelData
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Field

/**
 * Tests for OpenAIHelper cache logic.
 * Uses Robolectric because OpenAIHelper.invalidateCache() calls Log.d().
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [28])
class OpenAIHelperTest {

    private fun createModelData(id: String): ModelData {
        val model = ModelData()
        model.id = id
        return model
    }

    @Before
    fun setUp() {
        OpenAIHelper.invalidateCache()
    }

    @After
    fun tearDown() {
        OpenAIHelper.invalidateCache()
    }

    // --- getCachedModels ---

    @Test
    fun `getCachedModels returns null after invalidation`() {
        assertNull(OpenAIHelper.getCachedModels())
    }

    @Test
    fun `getCachedModels returns models after setting cache via reflection`() {
        val models = listOf(createModelData("gpt-4"), createModelData("gpt-3.5-turbo"))
        setCachedModelsViaReflection(models)
        setLastFetchTimeViaReflection(System.currentTimeMillis())

        val result = OpenAIHelper.getCachedModels()
        assertEquals(2, result?.size)
        assertEquals("gpt-4", result?.get(0)?.id)
    }

    // --- isCacheValid ---

    @Test
    fun `isCacheValid returns false when cache is null`() {
        assertFalse(OpenAIHelper.isCacheValid())
    }

    @Test
    fun `isCacheValid returns false when cache is empty list`() {
        setCachedModelsViaReflection(emptyList())
        setLastFetchTimeViaReflection(System.currentTimeMillis())
        assertFalse(OpenAIHelper.isCacheValid())
    }

    @Test
    fun `isCacheValid returns false when cache is older than 1 hour`() {
        val models = listOf(createModelData("gpt-4"))
        setCachedModelsViaReflection(models)
        // Set fetch time to 2 hours ago
        setLastFetchTimeViaReflection(System.currentTimeMillis() - 2 * 60 * 60 * 1000)
        assertFalse(OpenAIHelper.isCacheValid())
    }

    @Test
    fun `isCacheValid returns true when cache is fresh`() {
        val models = listOf(createModelData("gpt-4"))
        setCachedModelsViaReflection(models)
        setLastFetchTimeViaReflection(System.currentTimeMillis())
        assertTrue(OpenAIHelper.isCacheValid())
    }

    @Test
    fun `isCacheValid returns true when cache is 59 minutes old`() {
        val models = listOf(createModelData("gpt-4"))
        setCachedModelsViaReflection(models)
        setLastFetchTimeViaReflection(System.currentTimeMillis() - 59 * 60 * 1000)
        assertTrue(OpenAIHelper.isCacheValid())
    }

    // --- invalidateCache ---

    @Test
    fun `invalidateCache clears models`() {
        val models = listOf(createModelData("gpt-4"))
        setCachedModelsViaReflection(models)
        setLastFetchTimeViaReflection(System.currentTimeMillis())

        assertTrue(OpenAIHelper.isCacheValid())
        OpenAIHelper.invalidateCache()
        assertNull(OpenAIHelper.getCachedModels())
        assertFalse(OpenAIHelper.isCacheValid())
    }

    @Test
    fun `invalidateCache resets fetch timestamp to 0`() {
        setLastFetchTimeViaReflection(System.currentTimeMillis())
        OpenAIHelper.invalidateCache()
        val field = OpenAIHelper::class.java.getDeclaredField("lastModelsFetchTimeMillis")
        field.isAccessible = true
        assertEquals(0L, field.getLong(null))
    }

    @Test
    fun `invalidateCache is safe to call when already empty`() {
        OpenAIHelper.invalidateCache()
        OpenAIHelper.invalidateCache() // Should not throw
        assertNull(OpenAIHelper.getCachedModels())
    }

    // --- Helpers ---

    private fun setCachedModelsViaReflection(models: List<ModelData>?) {
        val field: Field = OpenAIHelper::class.java.getDeclaredField("cachedModels")
        field.isAccessible = true
        field.set(null, models)
    }

    private fun setLastFetchTimeViaReflection(timeMillis: Long) {
        val field: Field = OpenAIHelper::class.java.getDeclaredField("lastModelsFetchTimeMillis")
        field.isAccessible = true
        field.setLong(null, timeMillis)
    }
}
