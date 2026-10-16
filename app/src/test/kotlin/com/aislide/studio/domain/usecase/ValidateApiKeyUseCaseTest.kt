package com.aislide.studio.domain.usecase

import com.aislide.studio.data.local.SettingsDataStore
import com.aislide.studio.data.remote.LlmException
import com.aislide.studio.data.remote.dto.ChatMessage
import com.aislide.studio.data.remote.dto.ChatRequest
import com.aislide.studio.data.remote.dto.ChatResponse
import com.aislide.studio.data.remote.dto.Choice
import com.aislide.studio.data.repository.LlmRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [ValidateApiKeyUseCase].
 *
 * Verifies the four result branches:
 * - Success: LLM responds with text
 * - InvalidKey: 401/403 in error
 * - WrongEndpoint: 404 in error
 * - Error: anything else
 *
 * And the early-out when API key is empty.
 */
class ValidateApiKeyUseCaseTest {

    private val llm: LlmRepository = mockk()
    private val settings: SettingsDataStore = mockk()

    private val useCase = ValidateApiKeyUseCase(llm, settings)

    @Test
    fun `empty API key returns InvalidKey`() = runTest {
        coEvery { settings.apiKey } returns flowOf("")
        coEvery { settings.providerId } returns flowOf("glm")
        coEvery { settings.model } returns flowOf("glm-4-plus")

        val result = useCase()

        assertTrue(result is ValidationResult.InvalidKey)
        assertTrue((result as ValidationResult.InvalidKey).message.contains("vacía"))
    }

    @Test
    fun `successful chat returns Success with reply`() = runTest {
        coEvery { settings.apiKey } returns flowOf("sk-valid-key")
        coEvery { settings.providerId } returns flowOf("glm")
        coEvery { settings.model } returns flowOf("glm-4-plus")
        coEvery { llm.chatText(any()) } returns "OK"

        val result = useCase()

        assertTrue(result is ValidationResult.Success)
        assertEquals("OK", (result as ValidationResult.Success).reply)
    }

    @Test
    fun `HTTP 401 in LlmException returns InvalidKey`() = runTest {
        coEvery { settings.apiKey } returns flowOf("sk-wrong")
        coEvery { settings.providerId } returns flowOf("glm")
        coEvery { settings.model } returns flowOf("glm-4-plus")
        coEvery { llm.chatText(any()) } throws LlmException("HTTP 401: Unauthorized")

        val result = useCase()

        assertTrue(result is ValidationResult.InvalidKey)
    }

    @Test
    fun `HTTP 403 in LlmException returns InvalidKey`() = runTest {
        coEvery { settings.apiKey } returns flowOf("sk-wrong")
        coEvery { settings.providerId } returns flowOf("glm")
        coEvery { settings.model } returns flowOf("glm-4-plus")
        coEvery { llm.chatText(any()) } throws LlmException("HTTP 403: Forbidden")

        val result = useCase()

        assertTrue(result is ValidationResult.InvalidKey)
    }

    @Test
    fun `HTTP 404 in LlmException returns WrongEndpoint`() = runTest {
        coEvery { settings.apiKey } returns flowOf("sk-x")
        coEvery { settings.providerId } returns flowOf("glm")
        coEvery { settings.model } returns flowOf("glm-4-plus")
        coEvery { llm.chatText(any()) } throws LlmException("HTTP 404: Not Found")

        val result = useCase()

        assertTrue(result is ValidationResult.WrongEndpoint)
    }

    @Test
    fun `network exception returns Error`() = runTest {
        coEvery { settings.apiKey } returns flowOf("sk-x")
        coEvery { settings.providerId } returns flowOf("glm")
        coEvery { settings.model } returns flowOf("glm-4-plus")
        coEvery { llm.chatText(any()) } throws java.net.SocketTimeoutException("timeout")

        val result = useCase()

        assertTrue(result is ValidationResult.Error)
    }

    @Test
    fun `LlmException with 500 returns Error not InvalidKey`() = runTest {
        coEvery { settings.apiKey } returns flowOf("sk-x")
        coEvery { settings.providerId } returns flowOf("glm")
        coEvery { settings.model } returns flowOf("glm-4-plus")
        coEvery { llm.chatText(any()) } throws LlmException("HTTP 500: Internal Server Error")

        val result = useCase()

        assertTrue(result is ValidationResult.Error)
        assertTrue(result !is ValidationResult.InvalidKey)
    }
}
