package com.aislide.studio.domain.usecase

import com.aislide.studio.data.model.SlideObject
import com.aislide.studio.data.repository.ProjectRepository
import com.aislide.studio.domain.service.HtmlSlideRenderer
import com.aislide.studio.domain.service.SlideHistoryManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manually edits an object's text or bullets (no AI).
 * Used by the inline text editor in the slide editor.
 *
 * Snapshots before update for undo support.
 */
@Singleton
class UpdateObjectTextUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val htmlRenderer: HtmlSlideRenderer,
    private val history: SlideHistoryManager
) {
    suspend operator fun invoke(
        projectId: Long,
        slide: com.aislide.studio.data.model.Slide,
        target: SlideObject,
        newText: String? = null,
        newBullets: List<String>? = null
    ): Result<Unit> = runCatching {
        history.get(slide.id).push(slide)

        val project = projectRepository.getProject(projectId)
            ?: error("Project not found: $projectId")

        val updated = target.copy(
            text = newText ?: target.text,
            bullets = newBullets ?: target.bullets
        )
        val updatedObjects = slide.objects.map { if (it.id == target.id) updated else it }
        val updatedSlide = slide.copy(
            objects = updatedObjects,
            htmlContent = htmlRenderer.render(
                slide.copy(objects = updatedObjects),
                project.palette,
                project.typography,
                interactive = true
            )
        )
        projectRepository.updateSlide(projectId, slide.index, updatedSlide)
    }
}
