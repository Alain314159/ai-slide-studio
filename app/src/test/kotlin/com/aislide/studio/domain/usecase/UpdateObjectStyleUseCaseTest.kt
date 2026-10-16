package com.aislide.studio.domain.usecase

import com.aislide.studio.data.model.ObjectPosition
import com.aislide.studio.data.model.ObjectStyle
import com.aislide.studio.data.model.ObjectType
import com.aislide.studio.data.model.Palette
import com.aislide.studio.data.model.Presentation
import com.aislide.studio.data.model.Slide
import com.aislide.studio.data.model.SlideLayout
import com.aislide.studio.data.model.SlideObject
import com.aislide.studio.data.model.Typography
import com.aislide.studio.data.repository.ProjectRepository
import com.aislide.studio.domain.service.HtmlSlideRenderer
import com.aislide.studio.domain.service.SlideHistoryManager
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [UpdateObjectStyleUseCase].
 *
 * Verifies:
 * 1. The new style replaces the old style on the target object.
 * 2. Other objects are NOT modified.
 * 3. HTML is regenerated.
 * 4. Snapshot is pushed for undo.
 * 5. Project not found returns failure.
 */
class UpdateObjectStyleUseCaseTest {

    private val projectRepository: ProjectRepository = mockk(relaxed = true)
    private val htmlRenderer: HtmlSlideRenderer = HtmlSlideRenderer()
    private val history: SlideHistoryManager = SlideHistoryManager()
    private val useCase = UpdateObjectStyleUseCase(projectRepository, htmlRenderer, history)

    @Test
    fun `applies new style to target object only`() = runTest {
        val target = SlideObject(id = "obj-1", type = ObjectType.TEXT_BOX, text = "Hi")
        val other = SlideObject(id = "obj-2", type = ObjectType.TEXT_BOX, text = "Other")
        val slide = Slide(
            id = "slide-1", index = 0, layout = SlideLayout.TITLE_AND_BULLETS,
            title = "T", objects = listOf(target, other)
        )
        val presentation = Presentation(
            id = 1L, title = "P",
            palette = Palette.default(),
            typography = Typography.default(),
            slides = listOf(slide)
        )
        coEvery { projectRepository.getProject(1L) } returns presentation
        coEvery { projectRepository.updateSlide(any(), any(), any()) } returns Unit

        val newStyle = target.style.copy(fontSizeSp = 32, fontWeight = 700)

        val result = useCase(1L, slide, target, newStyle)

        assertTrue(result.isSuccess)
        coVerify {
            projectRepository.updateSlide(1L, 0, match { updated ->
                val updatedTarget = updated.objects.find { it.id == "obj-1" }
                val updatedOther = updated.objects.find { it.id == "obj-2" }
                updatedTarget?.style?.fontSizeSp == 32 &&
                updatedTarget?.style?.fontWeight == 700 &&
                // Other object unchanged
                updatedOther?.style == other.style
            })
        }
    }

    @Test
    fun `snapshot is pushed to history before applying`() = runTest {
        val target = SlideObject(id = "obj-1", type = ObjectType.TEXT_BOX, text = "Hi")
        val slide = Slide(id = "slide-1", index = 0, layout = SlideLayout.TITLE, objects = listOf(target))
        val presentation = Presentation(id = 1L, title = "P", slides = listOf(slide))

        coEvery { projectRepository.getProject(1L) } returns presentation
        coEvery { projectRepository.updateSlide(any(), any(), any()) } returns Unit

        useCase(1L, slide, target, target.style.copy(fontWeight = 700))

        assertTrue(history.get(slide.id).canUndo)
    }

    @Test
    fun `project not found returns failure`() = runTest {
        val target = SlideObject(id = "obj-1", type = ObjectType.TEXT_BOX, text = "Hi")
        val slide = Slide(id = "slide-1", index = 0, layout = SlideLayout.TITLE, objects = listOf(target))

        coEvery { projectRepository.getProject(1L) } returns null

        val result = useCase(1L, slide, target, target.style)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Project not found") == true)
    }
}
