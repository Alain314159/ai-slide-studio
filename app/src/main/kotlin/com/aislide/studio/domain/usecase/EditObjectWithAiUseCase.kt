package com.aislide.studio.domain.usecase

import com.aislide.studio.data.local.database.AppJson
import com.aislide.studio.data.model.Slide
import com.aislide.studio.data.model.SlideObject
import com.aislide.studio.data.model.TextAlign
import com.aislide.studio.data.remote.dto.ObjectEditResultDto
import com.aislide.studio.data.repository.LlmRepository
import com.aislide.studio.data.repository.ProjectRepository
import com.aislide.studio.domain.service.HtmlSlideRenderer
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Edits ONE slide object with AI — the user's central feature request.
 *
 * The user taps an object on the slide and writes what they want changed.
 * This use case calls the LLM constrained to ONLY modify that object,
 * applies the result, re-renders the slide's HTML, and persists.
 */
@Singleton
class EditObjectWithAiUseCase @Inject constructor(
    private val llmRepository: LlmRepository,
    private val projectRepository: ProjectRepository,
    private val htmlRenderer: HtmlSlideRenderer
) {
    /**
     * @param projectId   The project the slide belongs to
     * @param slide        The current slide (full context for the LLM)
     * @param target       The specific object the user selected
     * @param instruction  Free-text instruction in natural language
     * @return Result with the explanation string from the LLM
     */
    suspend operator fun invoke(
        projectId: Long,
        slide: Slide,
        target: SlideObject,
        instruction: String
    ): Result<String> = runCatching {
        // Serialize the target object for the LLM
        val objectJson = AppJson.default.encodeToString(SlideObject.serializer(), target)

        // Provide context: which slide, what layout, what title
        val slideContext = "Slide ${slide.index + 1}, layout=${slide.layout}, title=\"${slide.title}\""

        // Call the LLM — constrained to ONLY modify the targeted object
        val result = llmRepository.editObjectWithAi(slideContext, objectJson, instruction)

        // Apply the edit locally to the object
        val updated = applyEdit(target, result)

        // Get the project to fetch palette + typography for HTML regen
        val project = projectRepository.getProject(projectId)
            ?: throw IllegalStateException("Project not found: $projectId")

        // Build the updated slide (other objects untouched)
        val updatedObjects = slide.objects.map { if (it.id == target.id) updated else it }
        val updatedSlide = slide.copy(
            objects = updatedObjects,
            htmlContent = htmlRenderer.render(slide.copy(objects = updatedObjects), project.palette, project.typography)
        )

        // Persist
        projectRepository.updateSlide(projectId, slide.index, updatedSlide)

        result.explanation
    }

    private fun applyEdit(target: SlideObject, result: ObjectEditResultDto): SlideObject {
        val newText = result.newText ?: target.text
        val newBullets = result.newBullets ?: target.bullets
        val newImageDescription = result.newImageDescription ?: target.imageDescription
        val style = result.newStyle?.let { newStyle ->
            target.style.copy(
                fontSizeSp = newStyle.fontSizeSp ?: target.style.fontSizeSp,
                fontWeight = newStyle.fontWeight ?: target.style.fontWeight,
                textColorHex = newStyle.textColorHex ?: target.style.textColorHex,
                textAlign = newStyle.textAlign?.let {
                    runCatching { TextAlign.valueOf(it) }.getOrDefault(target.style.textAlign)
                } ?: target.style.textAlign,
                backgroundColorHex = newStyle.backgroundColorHex ?: target.style.backgroundColorHex
            )
        } ?: target.style

        return target.copy(
            text = newText,
            bullets = newBullets,
            imageDescription = newImageDescription,
            style = style
        )
    }
}
