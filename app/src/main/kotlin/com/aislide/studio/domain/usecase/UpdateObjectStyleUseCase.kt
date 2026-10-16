package com.aislide.studio.domain.usecase

import com.aislide.studio.data.model.SlideObject
import com.aislide.studio.data.model.ObjectStyle
import com.aislide.studio.data.model.TextAlign
import com.aislide.studio.data.repository.ProjectRepository
import com.aislide.studio.domain.service.HtmlSlideRenderer
import com.aislide.studio.domain.service.SlideHistoryManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manually updates an object's style (font size, weight, color, alignment).
 * Used by the style editor bottom sheet in the slide editor.
 *
 * Snapshots before update for undo support.
 */
@Singleton
class UpdateObjectStyleUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val htmlRenderer: HtmlSlideRenderer,
    private val history: SlideHistoryManager
) {
    suspend operator fun invoke(
        projectId: Long,
        slide: com.aislide.studio.data.model.Slide,
        target: SlideObject,
        newStyle: ObjectStyle
    ): Result<Unit> = runCatching {
        // Snapshot for undo
        history.get(slide.id).push(slide)

        val project = projectRepository.getProject(projectId)
            ?: error("Project not found: $projectId")

        val updated = target.copy(style = newStyle)
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
