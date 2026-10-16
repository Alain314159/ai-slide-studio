package com.aislide.studio.data.remote

/**
 * Supported LLM providers. All must be OpenAI-compatible (same /chat/completions
 * and /images/generations endpoints, same JSON shape).
 *
 * The user picks one in Settings and provides their own API key (BYOK pattern).
 * GLM (z.ai) is the default since the user already has a key.
 *
 * Adding a new provider is as simple as adding a row here — no code changes
 * elsewhere, because all providers speak the same OpenAI wire format.
 */
enum class LlmProvider(
    val id: String,
    val displayName: String,
    val defaultEndpoint: String,
    val defaultModel: String,
    val defaultImageModel: String
) {
    GLM(
        id = "glm",
        displayName = "GLM (Z.ai)",
        defaultEndpoint = "https://api.z.ai/api/paas/v4",
        defaultModel = "glm-4-plus",
        defaultImageModel = "cogview-3-plus"
    ),
    OPENAI(
        id = "openai",
        displayName = "OpenAI",
        defaultEndpoint = "https://api.openai.com/v1",
        defaultModel = "gpt-4o",
        defaultImageModel = "dall-e-3"
    ),
    ANTHROPIC(
        id = "anthropic",
        displayName = "Anthropic (via OpenAI-compat shim)",
        defaultEndpoint = "https://api.anthropic.com/v1",
        defaultModel = "claude-3-5-sonnet-latest",
        defaultImageModel = "claude-3-5-sonnet-latest"
    ),
    CUSTOM(
        id = "custom",
        displayName = "Custom endpoint",
        defaultEndpoint = "",
        defaultModel = "",
        defaultImageModel = ""
    );

    companion object {
        fun fromId(id: String): LlmProvider = entries.find { it.id == id } ?: GLM
    }
}
