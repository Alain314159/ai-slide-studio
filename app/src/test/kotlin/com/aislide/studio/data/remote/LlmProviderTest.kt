package com.aislide.studio.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [LlmProvider] enum.
 *
 * These verify:
 * 1. Default values are populated (no empty defaults that would crash the LLM call).
 * 2. fromId resolves correctly.
 * 3. Unknown IDs fall back to GLM (not throw) — important for forward-compat.
 * 4. Every provider has consistent data (no null defaultEndpoint for non-custom providers).
 *
 * A failure here means a user picking a provider in Settings would get an empty
 * endpoint / model, and the LLM call would fail with "URL malformed" or similar.
 */
class LlmProviderTest {

    @Test
    fun `fromId returns matching provider`() {
        assertEquals(LlmProvider.GLM, LlmProvider.fromId("glm"))
        assertEquals(LlmProvider.OPENAI, LlmProvider.fromId("openai"))
        assertEquals(LlmProvider.ANTHROPIC, LlmProvider.fromId("anthropic"))
        assertEquals(LlmProvider.CUSTOM, LlmProvider.fromId("custom"))
    }

    @Test
    fun `fromId falls back to GLM for unknown id`() {
        // Forward-compat: if a future version adds a new provider id and the
        // stored settings still has the old one, we should fall back gracefully.
        assertEquals(LlmProvider.GLM, LlmProvider.fromId("nonexistent-provider"))
        assertEquals(LlmProvider.GLM, LlmProvider.fromId(""))
    }

    @Test
    fun `GLM provider has required defaults`() {
        val p = LlmProvider.GLM
        assertEquals("glm", p.id)
        assertTrue(p.defaultEndpoint.startsWith("https://"))
        assertTrue(p.defaultModel.isNotBlank())
        assertTrue(p.defaultImageModel.isNotBlank())
    }

    @Test
    fun `OPENAI provider has required defaults`() {
        val p = LlmProvider.OPENAI
        assertTrue(p.defaultEndpoint.startsWith("https://"))
        assertTrue(p.defaultModel.startsWith("gpt-"))
        assertTrue(p.defaultImageModel.isNotBlank())
    }

    @Test
    fun `ANTHROPIC provider has required defaults`() {
        val p = LlmProvider.ANTHROPIC
        assertTrue(p.defaultEndpoint.startsWith("https://"))
        assertTrue(p.defaultModel.isNotBlank())
    }

    @Test
    fun `CUSTOM provider has empty defaults`() {
        // CUSTOM is the user-configured escape hatch — its defaults should be empty
        // so the user is forced to fill them in (no silent wrong call).
        val p = LlmProvider.CUSTOM
        assertEquals("", p.defaultEndpoint)
        assertEquals("", p.defaultModel)
    }

    @Test
    fun `all providers have unique ids`() {
        val ids = LlmProvider.entries.map { it.id }
        assertEquals("Ids must be unique", ids.size, ids.toSet().size)
    }
}
