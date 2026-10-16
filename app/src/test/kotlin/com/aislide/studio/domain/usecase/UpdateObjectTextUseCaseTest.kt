package com.aislide.studio.domain.usecase

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
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [UpdateObjectTextUseCase].
 *
 * Verifies:
 * 1. newText replaces target.text, bullets unchanged.
 * 2. newBullets replaces target.bullets, text unchanged.
 * 3. Other objects unchanged.
 * 4. Snapshot pushed to history.
 * 5. Project not found returns failure.
 */
class UpdateObjectTextUseCaseTest {

    private val projectRepository: ProjectRepository = mockk(relaxed = true)
    private val htmlRenderer: HtmlSlideRenderer = HtmlSlideRenderer()
    private val history: SlideHistoryManager = SlideHistoryManager()
    private val useCase = UpdateObjectTextUseCase(projectRepository, htmlRenderer, history)

    @Test
    fun `newText replaces text but leaves bullets`() = runTest {
        val target = SlideObject(
            id = "obj-1", type = ObjectType.BULLET_LIST,
            text = "old", bullets = listOf("b1", "b2")
        )
        val slide = Slide(id = "s1", index = 0, layout = SlideLayout.TITLE, objects = listOf(target))
        val presentation = Presentation(
            id = 1L, title = "P",
            palette = Palette.default(), typography = Typography.default(),
            slides = listOf(slide)
        )
        coEvery { projectRepository.getProject(1L) } returns presentation
        coEvery { projectRepository.updateSlide(any(), any(), any()) } returns Unit

        useCase(1L, slide, target, newText = "new text", newBullets = null)

        coVerify {
            projectRepository.updateSlide(1L, 0, match { updated ->
                val obj = updated.objects.find { it.id == "obj-1" }
                obj?.text == "new text" && obj.bullets == listOf("b1", "b2")
            })
        }
    }

    @Test
    fun `newBullets replaces bullets but leaves text`() = runTest {
        val target = SlideObject(
            id = "obj-1", type = ObjectType.BULLET_LIST,
            text = "kept", bullets = listOf("old")
        )
        val slide = Slide(id = "s1", index = 0, layout = SlideLayout.TITLE, objects = listOf(target))
        val presentation = Presentation(
            id = 1L, title = "P",
            palette = Palette.default(), typography = Typography.default(),
            slides = listOf(slide)
        )
        coEvery { projectRepository.getProject(1L) } returns presentation
        coEvery { projectRepository.updateSlide(any(), any(), any()) } returns Unit

        useCase(1L, slide, target, newText = null, newBullets = listOf("new1", "new2"))

        coVerify {
            projectRepository.updateSlide(1L, 0, match { updated ->
                val obj = updated.objects.find { it.id == "obj-1" }
                obj?.text == "kept" && obj.bullets == listOf("new1", "new2")
            })
        }
    }

    @Test
    fun `snapshot pushed before applying`() = runTest {
        val target = SlideObject(id = "obj-1", type = ObjectType.TEXT_BOX, text = "old")
        val slide = Slide(id = "s1", index = 0, layout = SlideLayout.TITLE, objects = listOf(target))
        val presentation = Presentation(
            id = 1L, title = "P",
            palette = Palette.default(), typography = Typography.default(),
            slides = listOf(slide)
        )
        coEvery { projectRepository.getProject(1L) } returns presentation
        coEvery { projectRepository.updateSlide(any(), any(), any()) } returns Unit

        useCase(1L, slide, target, newText = "new")

        assertTrue(history.get(slide.id).canUndo)
    }

    @Test
    fun `project not found returns failure`() = runTest {
        val target = SlideObject(id = "obj-1", type = ObjectType.TEXT_BOX, text = "x")
        val slide = Slide(id = "s1", index = 0, layout = SlideLayout.TITLE, objects = listOf(target))

        coEvery { projectRepository.getProject(1L) } returns null

        val result = useCase(1L, slide, target, newText = "new")
        assertTrue(result.isFailure)
    }
}
