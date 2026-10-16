package com.aislide.studio.data.repository

import com.aislide.studio.data.local.SettingsDataStore
import com.aislide.studio.data.remote.LlmApiService
import com.aislide.studio.data.remote.LlmException
import com.aislide.studio.data.remote.LlmProvider
import com.aislide.studio.data.remote.dto.ChatMessage
import com.aislide.studio.data.remote.dto.ChatRequest
import com.aislide.studio.data.remote.dto.ImageGenerationRequest
import com.aislide.studio.data.remote.dto.PresentationDto
import com.aislide.studio.data.remote.dto.ObjectEditResultDto
import com.aislide.studio.data.local.database.AppJson
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository that wraps all LLM API calls.
 *
 * The repository reads the user's settings (provider, endpoint, model, key)
 * on every call — that way switching providers in Settings takes effect
 * immediately without app restart.
 *
 * All prompts are centralized here, so tweaking them is one-file work.
 */
@Singleton
class LlmRepository @Inject constructor(
    private val apiService: LlmApiService,
    private val settings: SettingsDataStore
) {
    private data class ResolvedConfig(val endpoint: String, val apiKey: String, val model: String, val imageModel: String)

    private suspend fun resolve(): ResolvedConfig {
        val apiKey = settings.apiKey.first()
        if (apiKey.isBlank()) throw LlmException("API key missing. Open Settings → API Key.")
        val provider = LlmProvider.fromId(settings.providerId.first())
        val endpoint = settings.endpoint.first().ifBlank { provider.defaultEndpoint }
        val model = settings.model.first().ifBlank { provider.defaultModel }
        val imageModel = provider.defaultImageModel
        return ResolvedConfig(endpoint, apiKey, model, imageModel)
    }

    /**
     * Generates a complete presentation from a free-text description.
     * Returns the structured JSON the LLM produced.
     */
    suspend fun generatePresentation(prompt: String): PresentationDto {
        val cfg = resolve()
        val request = ChatRequest(
            model = cfg.model,
            responseFormat = com.aislide.studio.data.remote.dto.ResponseFormat(type = "json_object"),
            temperature = 0.8,
            messages = listOf(
                ChatMessage(
                    role = "system",
                    content = GENERATION_SYSTEM_PROMPT
                ),
                ChatMessage(role = "user", content = prompt)
            )
        )
        val raw = apiService.chatText(cfg.endpoint, cfg.apiKey, request)
        return AppJson.default.decodeFromString(PresentationDto.serializer(), extractJsonObject(raw))
    }

    /**
     * Edits a single slide object with AI.
     * The LLM is constrained to ONLY modify the targeted object.
     */
    suspend fun editObjectWithAi(
        slideContext: String,
        objectJson: String,
        userInstruction: String
    ): ObjectEditResultDto {
        val cfg = resolve()
        val request = ChatRequest(
            model = cfg.model,
            responseFormat = com.aislide.studio.data.remote.dto.ResponseFormat(type = "json_object"),
            temperature = 0.7,
            messages = listOf(
                ChatMessage(role = "system", content = OBJECT_EDIT_SYSTEM_PROMPT),
                ChatMessage(
                    role = "user",
                    content = buildString {
                        appendLine("=== Slide context ===")
                        appendLine(slideContext)
                        appendLine()
                        appendLine("=== Object being edited (JSON) ===")
                        appendLine(objectJson)
                        appendLine()
                        appendLine("=== User instruction ===")
                        appendLine(userInstruction)
                    }
                )
            )
        )
        val raw = apiService.chatText(cfg.endpoint, cfg.apiKey, request)
        return AppJson.default.decodeFromString(ObjectEditResultDto.serializer(), extractJsonObject(raw))
    }

    /**
     * Generates an image with GLM image-gen (CogView) or DALL-E.
     * Returns the image URL (or throws if the call fails).
     */
    suspend fun generateImage(prompt: String): String {
        val cfg = resolve()
        val request = ImageGenerationRequest(
            prompt = prompt,
            model = cfg.imageModel.takeIf { it.isNotBlank() },
            n = 1,
            size = "1024x1024",
            responseFormat = "url"
        )
        val response = apiService.generateImage(cfg.endpoint, cfg.apiKey, request)
        val url = response.data.firstOrNull()?.url
            ?: throw LlmException("No image URL in response")
        return url
    }

    /**
     * Generates speaker notes for a slide.
     */
    suspend fun generateSpeakerNotes(slideContext: String): String {
        val cfg = resolve()
        val request = ChatRequest(
            model = cfg.model,
            temperature = 0.6,
            messages = listOf(
                ChatMessage(
                    role = "system",
                    content = "You are an expert speaker. Write natural, engaging speaker notes for the slide described by the user. 100-200 words. Plain prose, no markdown."
                ),
                ChatMessage(role = "user", content = slideContext)
            )
        )
        return apiService.chatText(cfg.endpoint, cfg.apiKey, request)
    }

    /**
     * Public chat call for arbitrary prompts (e.g. for API key validation).
     * Returns the assistant message text.
     */
    suspend fun chatText(request: ChatRequest): String {
        val cfg = resolve()
        return apiService.chatText(cfg.endpoint, cfg.apiKey, request)
    }

    /**
     * Some models wrap JSON inside ```json ... ``` blocks. Strip those.
     */
    private fun extractJsonObject(raw: String): String {
        val trimmed = raw.trim()
        if (trimmed.startsWith("{")) return trimmed
        val start = trimmed.indexOf("{")
        val end = trimmed.lastIndexOf("}")
        if (start >= 0 && end > start) return trimmed.substring(start, end + 1)
        return trimmed
    }

    companion object {
        const val GENERATION_SYSTEM_PROMPT = """You are an expert presentation designer. The user describes a presentation in natural language; you return a complete, ready-to-render presentation as JSON.

Required JSON shape:
{
  "title": string,
  "description": string,
  "palette": {
    "primary": hex string,
    "secondary": hex string,
    "accent": hex string,
    "background": hex string,
    "surface": hex string,
    "on_primary": hex string,
    "on_background": hex string
  },
  "typography": {
    "heading_font": string (e.g. "Poppins"),
    "body_font": string (e.g. "Inter"),
    "heading_weight": int (400-800),
    "body_weight": int (400-700)
  },
  "slides": [
    {
      "index": int,
      "layout": "TITLE" | "TITLE_AND_BULLETS" | "TITLE_AND_IMAGE" | "IMAGE_FULL" | "TWO_COLUMNS" | "QUOTE" | "SECTION_DIVIDER" | "DATA_TABLE" | "DATA_CHART" | "CONCLUSION",
      "title": string,
      "subtitle": string,
      "bullets": string[],
      "image_description": string (used by image-gen later; null if no image needed),
      "notes_for_speaker": string (100-200 words of what to SAY when presenting)
    }
  ]
}

Rules:
1. Every presentation MUST start with a TITLE slide and end with a CONCLUSION slide.
2. Each bullet should be a single short sentence — no nested bullets.
3. Image descriptions should be specific enough for an image generation model (subject, style, mood).
4. The palette must look unique and modern; never use defaults.
5. The typography pairing must complement the topic (e.g. playful fonts for kids, serif for academic, modern sans for tech).
6. Return ONLY the JSON object — no markdown, no prose."""

        const val OBJECT_EDIT_SYSTEM_PROMPT = """You are an expert presentation editor. The user has selected ONE object on a slide and wants you to edit ONLY that object.

You receive:
- The full slide context (for awareness only — do not modify other objects)
- The exact object being edited (JSON)
- The user's instruction in natural language

You return JSON with this shape:
{
  "new_text": string (only if text changed; null otherwise),
  "new_bullets": string[] (only if bullet list changed; null otherwise),
  "new_image_description": string (only if image should change; null otherwise),
  "new_style": {
    "font_size_sp": int (only if changed),
    "font_weight": int (only if changed),
    "text_color_hex": string (only if changed),
    "text_align": "LEFT" | "CENTER" | "RIGHT" | "JUSTIFIED" (only if changed),
    "background_color_hex": string (only if changed)
  },
  "explanation": string (1 sentence, in Spanish, describing what you changed)
}

Rules:
1. ONLY the targeted object changes — never modify other objects or the slide layout.
2. If the user asks to split the object into multiple slides, return new_text with the new content; the app will let the user move it elsewhere.
3. If the user asks to change the tone, rewrite text only — do not add new objects.
4. Return ONLY JSON — no markdown, no prose."""
    }
}
