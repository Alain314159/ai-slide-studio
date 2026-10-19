package com.aislide.studio.llm

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.bearerAuth
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * LLM service — bridges Kotlin and any OpenAI-compatible chat API.
 *
 * v2 architecture: this service is the SOLE entry point for AI calls.
 * PPTist's AIRequest events are routed here. Results are returned as JSON
 * strings that the WebView injects back into PPTist.
 *
 * BYOK pattern: API key, endpoint, and model come from the user's settings.
 * For now (MVP), they're hardcoded constants — to be wired to DataStore later.
 *
 * Supported actions (called from PPTist via bridge):
 * - generate(prompt): full presentation from text
 * - edit_object(payload): edit a single slide object
 * - edit_slide(payload): edit an entire slide
 * - improve_design(payload): redesign a slide's visual style
 */
@Singleton
class LlmService @Inject constructor(
    private val httpClient: HttpClient
) {
    /**
     * Generate a complete presentation from a free-text prompt.
     * Returns a JSON string with the PPTist-compatible slide schema.
     */
    suspend fun generatePresentation(prompt: String): String {
        val request = ChatRequest(
            messages = listOf(
                ChatMessage("system", GENERATION_SYSTEM_PROMPT),
                ChatMessage("user", prompt)
            ),
            responseFormat = ResponseFormat("json_object")
        )
        return callLlm(request)
    }

    /**
     * Edit a single object on a slide.
     * payload: JSON with { slideContext, targetObject, instruction }
     */
    suspend fun editObject(payload: String): String {
        val request = ChatRequest(
            messages = listOf(
                ChatMessage("system", OBJECT_EDIT_SYSTEM_PROMPT),
                ChatMessage("user", payload)
            ),
            responseFormat = ResponseFormat("json_object")
        )
        return callLlm(request)
    }

    /**
     * Edit an entire slide.
     * payload: JSON with { slide, instruction }
     */
    suspend fun editSlide(payload: String): String {
        val request = ChatRequest(
            messages = listOf(
                ChatMessage("system", SLIDE_EDIT_SYSTEM_PROMPT),
                ChatMessage("user", payload)
            ),
            responseFormat = ResponseFormat("json_object")
        )
        return callLlm(request)
    }

    /**
     * Improve a slide's visual design (palette, typography, layout).
     * payload: JSON with { slide, style preference }
     */
    suspend fun improveDesign(payload: String): String {
        val request = ChatRequest(
            messages = listOf(
                ChatMessage("system", DESIGN_IMPROVE_SYSTEM_PROMPT),
                ChatMessage("user", payload)
            ),
            responseFormat = ResponseFormat("json_object")
        )
        return callLlm(request)
    }

    /**
     * Validates the API key by sending a tiny ping request.
     * Returns success/failure for use in Settings UI.
     */
    suspend fun validateApiKey(): Boolean {
        return try {
            val request = ChatRequest(
                messages = listOf(
                    ChatMessage("system", "Reply with the word OK."),
                    ChatMessage("user", "ping")
                ),
                maxTokens = 5
            )
            callLlm(request)
            true
        } catch (e: Exception) {
            Timber.w(e, "API key validation failed")
            false
        }
    }

    /**
     * Core call to the LLM API. Returns the assistant message text.
     */
    private suspend fun callLlm(request: ChatRequest): String {
        val cfg = resolveConfig()
        val url = "${cfg.endpoint.trimEnd('/')}/chat/completions"

        val response = httpClient.post(url) {
            bearerAuth(cfg.apiKey)
            contentType(ContentType.Application.Json)
            setBody(json.encodeToString(ChatRequest.serializer(), request))
        }

        if (!response.status.isSuccess()) {
            val body = response.bodyAsText()
            throw LlmException("HTTP ${response.status.value}: $body")
        }

        val responseBody = response.bodyAsText()
        val chatResponse = json.decodeFromString(ChatResponse.serializer(), responseBody)
        return chatResponse.choices.firstOrNull()?.message?.content
            ?: throw LlmException("Empty LLM response")
    }

    /**
     * Resolve LLM config (endpoint, api key, model).
     * TODO: replace hardcoded with DataStore-backed settings.
     */
    private fun resolveConfig(): LlmConfig {
        val apiKey = "REPLACE_WITH_USER_API_KEY"
        if (apiKey.isBlank() || apiKey == "REPLACE_WITH_USER_API_KEY") {
            throw LlmException("API key not configured. Open Settings to set it.")
        }
        return LlmConfig(
            endpoint = "https://api.z.ai/api/paas/v4",
            apiKey = apiKey,
            model = "glm-4-plus"
        )
    }

    private data class LlmConfig(
        val endpoint: String,
        val apiKey: String,
        val model: String
    )

    companion object {
        val json = Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
            explicitNulls = false
            prettyPrint = false
        }

        // === System prompts ===
        // These are intentionally permissive and instruct the LLM to return
        // PPTist-compatible JSON shapes. The actual schema is documented at
        // https://github.com/pipipi-pikachu/PPTist — slides is an array of
        // elements (text, image, shape, chart, table, etc.).

        const val GENERATION_SYSTEM_PROMPT = """You are an expert presentation designer. The user describes a presentation; you return a JSON object that PPTist (an open-source slide editor) can load.

Required JSON shape:
{
  "slides": [
    {
      "id": "slide-1",
      "elements": [
        {
          "type": "text",
          "id": "el-1",
          "content": "Slide title here",
          "style": {
            "fontFamily": "Inter",
            "fontSize": 48,
            "color": "#ffffff",
            "textAlign": "center",
            "bold": true
          },
          "left": 100,
          "top": 80,
          "width": 800,
          "height": 100
        }
      ],
      "background": {
        "fill": "#1a1d26"
      }
    }
  ],
  "theme": "default"
}

Element types: text, image, shape, chart, table, line.
- For text elements, use "content" with the actual text.
- For shapes, use "shape" with one of: rect, round, ellipse, triangle, arrow.
- For images, use "src" with a description; the app will generate or fetch later.
- For charts, use "chartType" (bar, line, pie) and "data".

Rules:
1. Every presentation starts with a title slide and ends with a conclusion slide.
2. Each slide should have at most 5-7 bullets or 2-3 visual elements.
3. Use colors that contrast well with the background.
4. Title slides: large text centered, subtitle smaller.
5. Content slides: title at top, bullets/elements below.
6. Image slides: image takes 60% of slide, text at the side.
7. Return ONLY JSON — no markdown, no prose, no code fences."""

        const val OBJECT_EDIT_SYSTEM_PROMPT = """You are an expert slide editor. The user has selected ONE element on a slide and wants you to modify ONLY that element.

The user message contains:
- slideContext: the slide as JSON (for context, do not modify other elements)
- targetObject: the element being edited
- instruction: what the user wants

Return JSON with the new element:
{
  "type": "...",
  "id": "...",  // same id as input
  "content": "...",
  "style": { ... },
  "left": ...,
  "top": ...,
  "width": ...,
  "height": ...
}

Rules:
1. Modify ONLY the targeted element. Do not change other elements.
2. Preserve the element's id.
3. Return ONLY JSON, no markdown."""

        const val SLIDE_EDIT_SYSTEM_PROMPT = """You are an expert slide editor. The user wants to modify an entire slide.

The user message contains:
- slide: the slide as JSON
- instruction: what the user wants

Return the modified slide as JSON (same schema as input, but updated).

Rules:
1. Keep the slide's id.
2. Modify elements as instructed.
3. Return ONLY JSON, no markdown."""

        const val DESIGN_IMPROVE_SYSTEM_PROMPT = """You are a presentation design expert. The user wants you to improve the visual design of a slide.

The user message contains:
- slide: the slide as JSON
- preferences: optional (e.g. "more minimalist", "warmer colors")

Return the slide with updated styles, positions, and layout. Improve:
- Color harmony
- Typography hierarchy
- Whitespace and alignment
- Element positioning

Return ONLY JSON, no markdown."""
    }
}

class LlmException(message: String, cause: Throwable? = null) : Exception(message, cause)

// === DTOs for OpenAI-compatible API ===

@Serializable
data class ChatRequest(
    val model: String = "glm-4-plus",
    val messages: List<ChatMessage>,
    val temperature: Double = 0.8,
    @kotlinx.serialization.SerialName("max_tokens")
    val maxTokens: Int? = null,
    @kotlinx.serialization.SerialName("response_format")
    val responseFormat: ResponseFormat? = null,
    val stream: Boolean = false
)

@Serializable
data class ChatMessage(
    val role: String,
    val content: String
)

@Serializable
data class ResponseFormat(
    val type: String
)

@Serializable
data class ChatResponse(
    val id: String? = null,
    val choices: List<Choice> = emptyList(),
    val model: String? = null
)

@Serializable
data class Choice(
    val index: Int = 0,
    val message: ChatMessage,
    @kotlinx.serialization.SerialName("finish_reason")
    val finishReason: String? = null
)
