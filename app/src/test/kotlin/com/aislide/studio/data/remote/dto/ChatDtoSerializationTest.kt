package com.aislide.studio.data.remote.dto

import com.aislide.studio.data.local.database.AppJson
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Serialization tests for [ChatRequest] / [ChatResponse] / [ChatMessage].
 *
 * These verify that:
 * 1. The OpenAI-compatible wire format round-trips correctly.
 * 2. snake_case aliases (max_tokens, response_format, finish_reason) serialize
 *    under the names OpenAI expects — NOT Kotlin camelCase.
 * 3. Nullable fields are omitted when null (encodeDefaults behavior).
 *
 * A failure here means the LLM call would silently send wrong field names,
 * and the API would return 400 / ignore the parameter.
 */
class ChatDtoSerializationTest {

    private val json = AppJson.default

    @Test
    fun `ChatMessage serializes with role and content only`() {
        val msg = ChatMessage(role = "user", content = "hello")
        val encoded = json.encodeToString(msg)
        assertTrue(encoded.contains("\"role\":\"user\""))
        assertTrue(encoded.contains("\"content\":\"hello\""))
    }

    @Test
    fun `ChatRequest encodes max_tokens as snake_case`() {
        val req = ChatRequest(
            model = "glm-4-plus",
            messages = listOf(ChatMessage("user", "test")),
            maxTokens = 100
        )
        val encoded = json.encodeToString(req)
        assertTrue("max_tokens must be snake_case for OpenAI compatibility", encoded.contains("\"max_tokens\":100"))
    }

    @Test
    fun `ChatRequest encodes response_format as snake_case`() {
        val req = ChatRequest(
            model = "glm-4-plus",
            messages = listOf(ChatMessage("user", "test")),
            responseFormat = ResponseFormat(type = "json_object")
        )
        val encoded = json.encodeToString(req)
        assertTrue(encoded.contains("\"response_format\":{\"type\":\"json_object\"}"))
    }

    @Test
    fun `ChatRequest with null fields omits them`() {
        val req = ChatRequest(
            model = "glm-4-plus",
            messages = listOf(ChatMessage("user", "test"))
        )
        val encoded = json.encodeToString(req)
        assertTrue(!encoded.contains("max_tokens"))
        assertTrue(!encoded.contains("response_format"))
    }

    @Test
    fun `ChatResponse decodes from OpenAI shape`() {
        val responseJson = """
            {
              "id": "chatcmpl-abc",
              "choices": [
                {
                  "index": 0,
                  "message": {"role": "assistant", "content": "Hello!"},
                  "finish_reason": "stop"
                }
              ],
              "model": "glm-4-plus",
              "usage": {"prompt_tokens": 10, "completion_tokens": 5, "total_tokens": 15}
            }
        """.trimIndent()

        val decoded = json.decodeFromString(ChatResponse.serializer(), responseJson)
        assertEquals("chatcmpl-abc", decoded.id)
        assertEquals(1, decoded.choices.size)
        assertEquals("assistant", decoded.choices[0].message.role)
        assertEquals("Hello!", decoded.choices[0].message.content)
        assertEquals("stop", decoded.choices[0].finishReason)
        assertEquals(15, decoded.usage?.totalTokens)
    }

    @Test
    fun `ChatResponse handles missing usage field`() {
        val responseJson = """{"choices":[{"index":0,"message":{"role":"assistant","content":"hi"}}]}"""
        val decoded = json.decodeFromString(ChatResponse.serializer(), responseJson)
        assertNotNull(decoded)
        assertEquals(1, decoded.choices.size)
        assertEquals(null, decoded.usage)
    }

    @Test
    fun `ChatResponse with empty choices does not throw`() {
        val responseJson = """{"choices":[]}"""
        val decoded = json.decodeFromString(ChatResponse.serializer(), responseJson)
        assertEquals(0, decoded.choices.size)
    }
}
