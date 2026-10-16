package com.aislide.studio.domain.usecase

import com.aislide.studio.data.model.SlideObject
import com.aislide.studio.data.repository.ProjectRepository
import com.aislide.studio.domain.service.HtmlSlideRenderer
import com.aislide.studio.domain.service.SlideHistoryManager
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Adds an object to a slide. Snapshots state first for undo.
 */
@Singleton
class AddSlideObjectUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val htmlRenderer: HtmlSlideRenderer,
    private val history: SlideHistoryManager
) {
    suspend operator fun invoke(
        projectId: Long,
        slideIndex: Int,
        type: com.aislide.studio.data.model.ObjectType,
        text: String = "",
        bullets: List<String> = emptyList(),
        imageUrl: String? = null
    ): Result<Unit> = runCatching {
        val project = projectRepository.getProject(projectId)
            ?: error("Project not found: $projectId")
        val slide = project.slides.find { it.index == slideIndex }
            ?: error("Slide not found: $slideIndex")

        // Snapshot for undo
        history.get(slide.id).push(slide)

        val newObj = SlideObject(
            id = UUID.randomUUID().toString(),
            type = type,
            text = text,
            bullets = bullets,
            imageUrl = imageUrl,
            position = com.aislide.studio.data.model.ObjectPosition(
                x = 0.08f,
                y = 0.08f,
                width = 0.84f,
                height = 0.2f
            ),
            style = com.aislide.studio.data.model.ObjectStyle.default()
        )
        val updatedSlide = slide.copy(
            objects = slide.objects + newObj,
            htmlContent = htmlRenderer.render(
                slide.copy(objects = slide.objects + newObj),
                project.palette,
                project.typography,
                interactive = true
            )
        )
        projectRepository.updateSlide(projectId, slideIndex, updatedSlide)
    }
}
