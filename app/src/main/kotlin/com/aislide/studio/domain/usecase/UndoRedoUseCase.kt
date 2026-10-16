package com.aislide.studio.domain.usecase

import com.aislide.studio.data.model.Slide
import com.aislide.studio.data.repository.ProjectRepository
import com.aislide.studio.domain.service.HtmlSlideRenderer
import com.aislide.studio.domain.service.SlideHistoryManager
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies undo/redo to slide edits.
 *
 * Pattern:
 * - Before any change, the use case pushes the current slide state to the
 *   per-slide [SlideHistoryManager].
 * - On undo, the previous state is restored and the current is pushed to redo.
 * - On redo, the opposite happens.
 *
 * The slide's HTML is regenerated from the restored state so the WebView
 * reflects the change immediately.
 */
@Singleton
class UndoRedoUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val htmlRenderer: HtmlSlideRenderer,
    private val history: SlideHistoryManager
) {
    /**
     * Call this BEFORE applying any edit, to snapshot the current state.
     */
    suspend fun snapshot(projectId: Long, slide: Slide) {
        history.get(slide.id).push(slide)
    }

    suspend fun undo(projectId: Long, current: Slide): Result<Slide?> = runCatching {
        val previous = history.get(current.id).undo(current) ?: return@runCatching null
        val pres = projectRepository.getProject(projectId) ?: error("Project not found")
        val withHtml = previous.copy(
            htmlContent = htmlRenderer.render(previous, pres.palette, pres.typography, interactive = true)
        )
        projectRepository.updateSlide(projectId, current.index, withHtml)
        withHtml
    }

    suspend fun redo(projectId: Long, current: Slide): Result<Slide?> = runCatching {
        val next = history.get(current.id).redo(current) ?: return@runCatching null
        val pres = projectRepository.getProject(projectId) ?: error("Project not found")
        val withHtml = next.copy(
            htmlContent = htmlRenderer.render(next, pres.palette, pres.typography, interactive = true)
        )
        projectRepository.updateSlide(projectId, current.index, withHtml)
        withHtml
    }

    val canUndo: (slideId: String) -> Boolean = { history.get(it).canUndo }
    val canRedo: (slideId: String) -> Boolean = { history.get(it).canRedo }
}
