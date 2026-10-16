package com.aislide.studio.domain.usecase

import com.aislide.studio.data.model.Palette
import com.aislide.studio.data.model.Presentation
import com.aislide.studio.data.model.Slide
import com.aislide.studio.data.model.SlideLayout
import com.aislide.studio.data.model.SlideObject
import com.aislide.studio.data.model.ObjectType
import com.aislide.studio.data.model.Typography
import com.aislide.studio.data.repository.ProjectRepository
import com.aislide.studio.domain.service.HtmlSlideRenderer
import com.aislide.studio.domain.service.SlideHistoryManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [UndoRedoUseCase].
 *
 * Verifies:
 * 1. snapshot() pushes to history.
 * 2. undo() restores the previous slide and persists with HTML regeneration.
 * 3. redo() restores the next slide and persists.
 * 4. undo with empty history returns null (no crash).
 * 5. Project not found returns failure.
 */
class UndoRedoUseCaseTest {

    private val projectRepository: ProjectRepository = mockk(relaxed = true)
    private val htmlRenderer: HtmlSlideRenderer = HtmlSlideRenderer()
    private val history: SlideHistoryManager = SlideHistoryManager()
    private val useCase = UndoRedoUseCase(projectRepository, htmlRenderer, history)

    @Test
    fun `snapshot pushes to history`() = runTest {
        val slide = Slide(id = "s1", index = 0, layout = SlideLayout.TITLE, objects = listOf(
            SlideObject(id = "o1", type = ObjectType.TEXT_BOX, text = "v1")
        ))
        useCase.snapshot(1L, slide)
        assertTrue(useCase.canUndo(slide.id))
    }

    @Test
    fun `undo restores previous state and persists`() = runTest {
        val s1 = Slide(id = "s1", index = 0, layout = SlideLayout.TITLE, title = "v1", objects = listOf(
            SlideObject(id = "o1", type = ObjectType.TEXT_BOX, text = "v1")
        ))
        val s2 = Slide(id = "s1", index = 0, layout = SlideLayout.TITLE, title = "v2", objects = listOf(
            SlideObject(id = "o1", type = ObjectType.TEXT_BOX, text = "v2")
        ))
        val presentation = Presentation(
            id = 1L, title = "P",
            palette = Palette.default(), typography = Typography.default(),
            slides = listOf(s2)
        )

        coEvery { projectRepository.getProject(1L) } returns presentation
        coEvery { projectRepository.updateSlide(any(), any(), any()) } returns Unit

        // Push s1, current is s2
        useCase.snapshot(1L, s1)
        // Now undo (current=s2) → should restore s1
        val result = useCase.undo(1L, s2)

        assertTrue(result.isSuccess)
        val restored = result.getOrNull()
        assertTrue(restored != null)
        assertTrue(restored!!.title == "v1")

        coVerify(exactly = 1) { projectRepository.updateSlide(1L, 0, any()) }
    }

    @Test
    fun `redo restores next state and persists`() = runTest {
        val s1 = Slide(id = "s1", index = 0, layout = SlideLayout.TITLE, title = "v1",
            objects = listOf(SlideObject(id = "o1", type = ObjectType.TEXT_BOX, text = "v1")))
        val s2 = Slide(id = "s1", index = 0, layout = SlideLayout.TITLE, title = "v2",
            objects = listOf(SlideObject(id = "o1", type = ObjectType.TEXT_BOX, text = "v2")))
        val presentation = Presentation(
            id = 1L, title = "P",
            palette = Palette.default(), typography = Typography.default(),
            slides = listOf(s1)
        )

        coEvery { projectRepository.getProject(1L) } returns presentation
        coEvery { projectRepository.updateSlide(any(), any(), any()) } returns Unit

        useCase.snapshot(1L, s1)
        useCase.undo(1L, s2)  // undo (current=s2) → s1, push s2 to redo

        val result = useCase.redo(1L, s1)  // redo (current=s1) → s2

        assertTrue(result.isSuccess)
        val restored = result.getOrNull()
        assertTrue(restored != null)
        assertTrue(restored!!.title == "v2")
    }

    @Test
    fun `undo with empty history returns null success`() = runTest {
        val slide = Slide(id = "s1", index = 0, layout = SlideLayout.TITLE)

        val result = useCase.undo(1L, slide)

        assertTrue(result.isSuccess)
        assertTrue(result.getOrNull() == null)
    }

    @Test
    fun `project not found returns failure`() = runTest {
        val s1 = Slide(id = "s1", index = 0, layout = SlideLayout.TITLE)
        val s2 = Slide(id = "s1", index = 0, layout = SlideLayout.TITLE, title = "v2")

        coEvery { projectRepository.getProject(1L) } returns null

        useCase.snapshot(1L, s1)
        val result = useCase.undo(1L, s2)

        assertTrue(result.isFailure)
    }
}
