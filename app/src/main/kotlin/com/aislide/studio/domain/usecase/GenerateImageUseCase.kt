package com.aislide.studio.domain.usecase

import android.content.Context
import com.aislide.studio.data.repository.LlmRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates an image from a text prompt using the configured LLM provider's
 * image-generation endpoint (GLM CogView, DALL-E, etc.).
 *
 * Downloads the image bytes and saves them to the app's cache directory, then
 * returns the local file path so the UI can display it via Coil.
 *
 * The local path can be assigned to [com.aislide.studio.data.model.SlideObject.imageUrl]
 * for use by [HtmlSlideRenderer] and [PptxExportEngine].
 */
@Singleton
class GenerateImageUseCase @Inject constructor(
    private val llmRepository: LlmRepository,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(prompt: String, slideIdHint: String = ""): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = llmRepository.generateImage(prompt)
                val bytes = URL(url).openStream().use { it.readBytes() }
                val safeName = "img_${slideIdHint.take(8).ifBlank { "slide" }}_${System.currentTimeMillis()}.png"
                val file = File(context.cacheDir, "generated_images").apply { mkdirs() }
                    .resolve(safeName)
                file.writeBytes(bytes)
                file.absolutePath
            }
        }
}
