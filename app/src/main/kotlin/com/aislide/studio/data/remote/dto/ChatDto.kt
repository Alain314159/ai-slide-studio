package com.aislide.studio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OpenAI-compatible Chat Completions request shape.
 *
 * Used by GLM (z.ai), OpenAI, and any other provider that implements the
 * OpenAI wire format — that's why it's the universal API contract.
 *
 * Docs: https://platform.openai.com/docs/api-reference/chat
 */
@Serializable
data class ChatRequest(
    val model: String,
    val messages: List<ChatMessage>,
    val temperature: Double? = null,
    @SerialName("max_tokens") val maxTokens: Int? = null,
    @SerialName("response_format") val responseFormat: ResponseFormat? = null,
    val stream: Boolean = false
)

@Serializable
data class ChatMessage(
    val role: String,        // "system" | "user" | "assistant"
    val content: String
)

@Serializable
data class ResponseFormat(
    val type: String        // "text" | "json_object"
)

@Serializable
data class ChatResponse(
    val id: String? = null,
    val choices: List<Choice> = emptyList(),
    val model: String? = null,
    val usage: Usage? = null
)

@Serializable
data class Choice(
    val index: Int = 0,
    val message: ChatMessage,
    @SerialName("finish_reason") val finishReason: String? = null
)

@Serializable
data class Usage(
    @SerialName("prompt_tokens") val promptTokens: Int = 0,
    @SerialName("completion_tokens") val completionTokens: Int = 0,
    @SerialName("total_tokens") val totalTokens: Int = 0
)
