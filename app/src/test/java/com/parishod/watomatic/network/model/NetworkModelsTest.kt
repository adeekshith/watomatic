package com.parishod.watomatic.network.model

import com.parishod.watomatic.network.model.atomatic.AtomaticAIErrorResponse
import com.parishod.watomatic.network.model.atomatic.AtomaticAIRequest
import com.parishod.watomatic.network.model.atomatic.AtomaticAIResponse
import com.parishod.watomatic.network.model.openai.Choice
import com.parishod.watomatic.network.model.openai.Message
import com.parishod.watomatic.network.model.openai.ModelData
import com.parishod.watomatic.network.model.openai.OpenAIErrorDetail
import com.parishod.watomatic.network.model.openai.OpenAIErrorResponse
import com.parishod.watomatic.network.model.openai.OpenAIModelsResponse
import com.parishod.watomatic.network.model.openai.OpenAIRequest
import com.parishod.watomatic.network.model.openai.OpenAIResponse
import com.parishod.watomatic.network.model.openai.ResponseMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkModelsTest {

    // --- Message ---

    @Test
    fun `Message constructor stores role and content`() {
        val msg = Message("user", "Hello world")
        assertEquals("user", msg.role)
        assertEquals("Hello world", msg.content)
    }

    @Test
    fun `Message setters update role and content`() {
        val msg = Message("user", "original")
        msg.role = "assistant"
        msg.content = "updated content"
        assertEquals("assistant", msg.role)
        assertEquals("updated content", msg.content)
    }

    @Test
    fun `Message allows system role`() {
        val msg = Message("system", "You are a helpful assistant.")
        assertEquals("system", msg.role)
    }

    // --- ResponseMessage ---

    @Test
    fun `ResponseMessage getters and setters work`() {
        val rm = ResponseMessage()
        rm.role = "assistant"
        rm.content = "I can help with that"
        assertEquals("assistant", rm.role)
        assertEquals("I can help with that", rm.content)
    }

    @Test
    fun `ResponseMessage defaults to null fields`() {
        val rm = ResponseMessage()
        assertNull(rm.role)
        assertNull(rm.content)
    }

    // --- Choice ---

    @Test
    fun `Choice getters and setters work`() {
        val rm = ResponseMessage()
        rm.role = "assistant"
        rm.content = "response text"

        val choice = Choice()
        choice.message = rm

        assertNotNull(choice.message)
        assertEquals("response text", choice.message.content)
    }

    @Test
    fun `Choice defaults to null message`() {
        val choice = Choice()
        assertNull(choice.message)
    }

    // --- OpenAIRequest ---

    @Test
    fun `OpenAIRequest constructor stores model and messages`() {
        val messages = listOf(Message("user", "test"))
        val request = OpenAIRequest("gpt-4o", messages)
        assertEquals("gpt-4o", request.model)
        assertEquals(1, request.messages.size)
        assertEquals("test", request.messages[0].content)
    }

    @Test
    fun `OpenAIRequest setters update fields`() {
        val request = OpenAIRequest("gpt-3.5-turbo", emptyList())
        request.model = "gpt-4o"
        request.messages = listOf(Message("user", "new"))
        assertEquals("gpt-4o", request.model)
        assertEquals(1, request.messages.size)
    }

    @Test
    fun `OpenAIRequest allows empty messages list`() {
        val request = OpenAIRequest("gpt-4o", emptyList())
        assertTrue(request.messages.isEmpty())
    }

    // --- OpenAIResponse ---

    @Test
    fun `OpenAIResponse getters and setters work`() {
        val rm = ResponseMessage()
        rm.content = "reply"
        val choice = Choice()
        choice.message = rm

        val response = OpenAIResponse()
        response.choices = listOf(choice)

        assertEquals(1, response.choices.size)
        assertEquals("reply", response.choices[0].message.content)
    }

    @Test
    fun `OpenAIResponse defaults to null choices`() {
        val response = OpenAIResponse()
        assertNull(response.choices)
    }

    // --- OpenAIErrorDetail ---

    @Test
    fun `OpenAIErrorDetail getters return null by default`() {
        val detail = OpenAIErrorDetail()
        assertNull(detail.message)
        assertNull(detail.type)
        assertNull(detail.param)
        assertNull(detail.code)
    }

    // --- OpenAIErrorResponse ---

    @Test
    fun `OpenAIErrorResponse getError returns null by default`() {
        val errorResponse = OpenAIErrorResponse()
        assertNull(errorResponse.error)
    }

    // --- ModelData ---

    @Test
    fun `ModelData getters and setters work`() {
        val model = ModelData()
        model.id = "gpt-4o"
        model.objectType = "model"
        model.created = 1_700_000_000L
        model.ownedBy = "openai"

        assertEquals("gpt-4o", model.id)
        assertEquals("model", model.objectType)
        assertEquals(1_700_000_000L, model.created)
        assertEquals("openai", model.ownedBy)
    }

    @Test
    fun `ModelData defaults to null and zero`() {
        val model = ModelData()
        assertNull(model.id)
        assertNull(model.objectType)
        assertEquals(0L, model.created)
        assertNull(model.ownedBy)
    }

    // --- OpenAIModelsResponse ---

    @Test
    fun `OpenAIModelsResponse getters and setters work`() {
        val model = ModelData()
        model.id = "gpt-4o"

        val response = OpenAIModelsResponse()
        response.data = listOf(model)
        response.objectType = "list"

        assertEquals(1, response.data.size)
        assertEquals("gpt-4o", response.data[0].id)
        assertEquals("list", response.objectType)
    }

    @Test
    fun `OpenAIModelsResponse defaults to null`() {
        val response = OpenAIModelsResponse()
        assertNull(response.data)
        assertNull(response.objectType)
    }

    // --- AtomaticAIRequest ---

    @Test
    fun `AtomaticAIRequest constructor stores message and prompt`() {
        val request = AtomaticAIRequest("Hello there", "Reply briefly")
        assertEquals("Hello there", request.message)
        assertEquals("Reply briefly", request.prompt)
    }

    @Test
    fun `AtomaticAIRequest setters update fields`() {
        val request = AtomaticAIRequest("original", "original prompt")
        request.message = "updated message"
        request.prompt = "updated prompt"
        assertEquals("updated message", request.message)
        assertEquals("updated prompt", request.prompt)
    }

    @Test
    fun `AtomaticAIRequest allows null prompt`() {
        val request = AtomaticAIRequest("Hello", null)
        assertNull(request.prompt)
    }

    // --- AtomaticAIResponse ---

    @Test
    fun `AtomaticAIResponse constructor stores reply and remainingAtoms`() {
        val response = AtomaticAIResponse("Auto reply text", 42)
        assertEquals("Auto reply text", response.reply)
        assertEquals(42, response.remainingAtoms)
    }

    @Test
    fun `AtomaticAIResponse no-arg constructor defaults to null and zero`() {
        val response = AtomaticAIResponse()
        assertNull(response.reply)
        assertEquals(0, response.remainingAtoms)
    }

    @Test
    fun `AtomaticAIResponse setters update fields`() {
        val response = AtomaticAIResponse()
        response.reply = "new reply"
        response.remainingAtoms = 99
        assertEquals("new reply", response.reply)
        assertEquals(99, response.remainingAtoms)
    }

    // --- AtomaticAIErrorResponse ---

    @Test
    fun `AtomaticAIErrorResponse no-arg constructor defaults correctly`() {
        val err = AtomaticAIErrorResponse()
        assertNull(err.error)
        assertNull(err.message)
        assertEquals(0, err.statusCode)
    }

    @Test
    fun `AtomaticAIErrorResponse full constructor stores all fields`() {
        val err = AtomaticAIErrorResponse("Unauthorized", "Token expired", 401)
        assertEquals("Unauthorized", err.error)
        assertEquals("Token expired", err.message)
        assertEquals(401, err.statusCode)
    }

    @Test
    fun `AtomaticAIErrorResponse setters update fields`() {
        val err = AtomaticAIErrorResponse()
        err.error = "Forbidden"
        err.message = "Access denied"
        err.statusCode = 403
        assertEquals("Forbidden", err.error)
        assertEquals("Access denied", err.message)
        assertEquals(403, err.statusCode)
    }

    // --- AtomaticAIErrorResponse.isAuthError ---

    @Test
    fun `isAuthError returns true for status 401`() {
        val err = AtomaticAIErrorResponse("Unauthorized", "Invalid credentials", 401)
        assertTrue(err.isAuthError)
    }

    @Test
    fun `isAuthError returns true for status 403`() {
        val err = AtomaticAIErrorResponse("Forbidden", "Access denied", 403)
        assertTrue(err.isAuthError)
    }

    @Test
    fun `isAuthError returns false for status 200`() {
        val err = AtomaticAIErrorResponse(null, null, 200)
        assertFalse(err.isAuthError)
    }

    @Test
    fun `isAuthError returns false for status 500`() {
        val err = AtomaticAIErrorResponse("Server Error", "Internal error", 500)
        assertFalse(err.isAuthError)
    }

    @Test
    fun `isAuthError returns true when message contains expired token`() {
        val err = AtomaticAIErrorResponse("Error", "Your token has expired", 200)
        assertTrue(err.isAuthError)
    }

    @Test
    fun `isAuthError returns true when message contains invalid token`() {
        val err = AtomaticAIErrorResponse("Error", "Token is invalid", 200)
        assertTrue(err.isAuthError)
    }

    @Test
    fun `isAuthError returns false when message is about token but not expired or invalid`() {
        val err = AtomaticAIErrorResponse("Error", "Token created successfully", 200)
        assertFalse(err.isAuthError)
    }

    @Test
    fun `isAuthError returns false when message is null and status is not 401 or 403`() {
        val err = AtomaticAIErrorResponse("Not Found", null, 404)
        assertFalse(err.isAuthError)
    }
}
