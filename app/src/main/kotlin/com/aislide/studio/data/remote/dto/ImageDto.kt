package com.aislide.studio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * OpenAI-compatible Image Generation request shape.
 *
 * Used by GLM (CogView), DALL-E, and any other OpenAI-compat image endpoint.
 *
 * Docs: https://platform.openai.com/docs/api-reference/images/create
 */
@Serializable
data class ImageGenerationRequest(
    val prompt: String,
    val model: String? = null,
    val n: Int = 1,
    val size: String = "1024x1024",       // "256x256" | "512x512" | "1024x1024"
    @SerialName("response_format") val responseFormat: String = "url"  // "url" | "b64_json"
)

@Serializable
data class ImageGenerationResponse(
    val created: Long = 0,
    val data: List<ImageData> = emptyList()
)

@Serializable
data class ImageData(
    val url: String? = null,
    @SerialName("b64_json") val b64Json: String? = null,
    @SerialName("revised_prompt") val revisedPrompt: String? = null
)
