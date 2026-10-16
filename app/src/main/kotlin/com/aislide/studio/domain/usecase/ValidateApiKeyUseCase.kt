package com.aislide.studio.domain.usecase

import com.aislide.studio.data.local.SettingsDataStore
import com.aislide.studio.data.remote.LlmException
import com.aislide.studio.data.remote.LlmProvider
import com.aislide.studio.data.remote.dto.ChatMessage
import com.aislide.studio.data.remote.dto.ChatRequest
import com.aislide.studio.data.remote.dto.ResponseFormat
import com.aislide.studio.data.repository.LlmRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Validates the user's API key + endpoint by sending a tiny "ping" request
 * to the LLM and reporting whether it succeeded.
 *
 * Used by the Settings screen "Probar conexión" button so users can verify
 * their API key works before generating a presentation.
 *
 * Returns:
 * - [ValidationResult.Success] on 200 + parseable response
 * - [ValidationResult.InvalidKey] on 401/403
 * - [ValidationResult.WrongEndpoint] on 404 / connection refused
 * - [ValidationResult.Error] on any other failure
 */
@Singleton
class ValidateApiKeyUseCase @Inject constructor(
    private val llmRepository: LlmRepository,
    private val settings: SettingsDataStore
) {
    suspend operator fun invoke(): ValidationResult {
        val apiKey = settings.apiKey.first()
        if (apiKey.isBlank()) return ValidationResult.InvalidKey("API key está vacía")

        val provider = LlmProvider.fromId(settings.providerId.first())
        val model = settings.model.first().ifBlank { provider.defaultModel }

        return try {
            val request = ChatRequest(
                model = model,
                maxTokens = 5,
                responseFormat = ResponseFormat(type = "text"),
                messages = listOf(
                    ChatMessage(role = "system", content = "Reply with: OK"),
                    ChatMessage(role = "user", content = "ping")
                )
            )
            val response = llmRepository.chatText(request)
            ValidationResult.Success(response.trim())
        } catch (e: LlmException) {
            val msg = e.message ?: ""
            when {
                msg.contains("401") || msg.contains("403") -> ValidationResult.InvalidKey(msg)
                msg.contains("404") -> ValidationResult.WrongEndpoint(msg)
                else -> ValidationResult.Error(msg)
            }
        } catch (e: Exception) {
            ValidationResult.Error(e.message ?: "Error desconocido")
        }
    }
}

sealed interface ValidationResult {
    data class Success(val reply: String) : ValidationResult
    data class InvalidKey(val message: String) : ValidationResult
    data class WrongEndpoint(val message: String) : ValidationResult
    data class Error(val message: String) : ValidationResult
}
