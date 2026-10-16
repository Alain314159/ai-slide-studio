package com.aislide.studio.domain.usecase

import com.aislide.studio.data.local.ImageStorageManager
import com.aislide.studio.data.repository.LlmRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates an image from a text prompt using the configured LLM provider's
 * image-generation endpoint (GLM CogView, DALL-E, etc.).
 *
 * v0.3 — uses [ImageStorageManager] to save the generated image to the
 * PERSISTENT files directory (not cache), so AI-generated images don't
 * disappear after app cache clearing.
 *
 * The local path can be assigned to [com.aislide.studio.data.model.SlideObject.imageUrl]
 * for use by [HtmlSlideRenderer] and [PptxExportEngine].
 */
@Singleton
class GenerateImageUseCase @Inject constructor(
    private val llmRepository: LlmRepository,
    private val imageStorage: ImageStorageManager
) {
    suspend operator fun invoke(prompt: String, slideIdHint: String = ""): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = llmRepository.generateImage(prompt)
                imageStorage.saveGeneratedImage(url, slideIdHint)
            }
        }
}
