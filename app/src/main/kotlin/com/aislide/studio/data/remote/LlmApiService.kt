package com.aislide.studio.data.remote

import com.aislide.studio.data.remote.dto.ChatRequest
import com.aislide.studio.data.remote.dto.ChatResponse
import com.aislide.studio.data.remote.dto.ImageGenerationRequest
import com.aislide.studio.data.remote.dto.ImageGenerationResponse
import com.aislide.studio.data.local.database.AppJson
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

/**
 * HTTP client wrapper for any OpenAI-compatible LLM provider.
 *
 * Why Ktor instead of Retrofit: in 2026 Ktor is the lighter Kotlin-first choice,
 * and it shares the same kotlinx.serialization ecosystem we already use. The
 * call site stays identical if we ever swap engines.
 *
 * Rule 5 of android skill: every call is a suspend function called off the main
 * thread — the dispatcher is owned by the caller (the repository or use case).
 *
 * Rule 7 of android skill: the API key is NOT shipped — it comes from the user's
 * DataStore entry and is sent only on the wire, never persisted in code.
 *
 * Rule of kotlin skill: errors propagate via sealed types; this layer throws
 * so callers can wrap in NetworkResponse-style results.
 */
@Singleton
class LlmApiService @Inject constructor(
    private val httpClient: HttpClient
) {
    /**
     * Calls POST {endpoint}/chat/completions. Returns the assistant message text
     * (the LLM's full reply). Throws on any HTTP/network/parse failure.
     */
    suspend fun chat(
        endpoint: String,
        apiKey: String,
        request: ChatRequest
    ): ChatResponse {
        val url = "${endpoint.trimEnd('/')}/chat/completions"
        Timber.d("LLM chat → %s (model=%s, msgs=%d)", url, request.model, request.messages.size)

        val response = httpClient.post(url) {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            setBody(AppJson.default.encodeToString(ChatRequest.serializer(), request))
        }

        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            throw LlmException("HTTP ${response.status.value}: $body")
        }

        return try {
            AppJson.default.decodeFromString(ChatResponse.serializer(), response.bodyAsText())
        } catch (e: Exception) {
            throw LlmException("Failed to parse LLM response: ${e.message}", e)
        }
    }

    /**
     * Convenience: chat + extract the assistant message text.
     */
    suspend fun chatText(
        endpoint: String,
        apiKey: String,
        request: ChatRequest
    ): String {
        val response = chat(endpoint, apiKey, request)
        return response.choices.firstOrNull()?.message?.content
            ?: throw LlmException("Empty LLM response")
    }

    /**
     * Calls POST {endpoint}/images/generations. Returns image URL or base64.
     */
    suspend fun generateImage(
        endpoint: String,
        apiKey: String,
        request: ImageGenerationRequest
    ): ImageGenerationResponse {
        val url = "${endpoint.trimEnd('/')}/images/generations"
        Timber.d("LLM image → %s (prompt len=%d)", url, request.prompt.length)

        val response = httpClient.post(url) {
            bearerAuth(apiKey)
            contentType(ContentType.Application.Json)
            setBody(AppJson.default.encodeToString(ImageGenerationRequest.serializer(), request))
        }

        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            throw LlmException("HTTP ${response.status.value}: $body")
        }

        return AppJson.default.decodeFromString(ImageGenerationResponse.serializer(), response.bodyAsText())
    }
}

class LlmException(message: String, cause: Throwable? = null) : Exception(message, cause)
