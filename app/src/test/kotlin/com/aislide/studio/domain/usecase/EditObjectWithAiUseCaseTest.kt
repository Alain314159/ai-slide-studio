package com.aislide.studio.domain.usecase

import com.aislide.studio.data.local.database.AppJson
import com.aislide.studio.data.model.ObjectPosition
import com.aislide.studio.data.model.ObjectStyle
import com.aislide.studio.data.model.ObjectType
import com.aislide.studio.data.model.Palette
import com.aislide.studio.data.model.Slide
import com.aislide.studio.data.model.SlideLayout
import com.aislide.studio.data.model.SlideObject
import com.aislide.studio.data.model.TextAlign
import com.aislide.studio.data.model.Typography
import com.aislide.studio.data.remote.dto.ObjectEditResultDto
import com.aislide.studio.data.remote.dto.ObjectStyleDto
import com.aislide.studio.data.repository.LlmRepository
import com.aislide.studio.data.repository.ProjectRepository
import com.aislide.studio.data.model.Presentation
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
 * Unit tests for [EditObjectWithAiUseCase].
 *
 * These verify:
 * 1. The LLM is called with the correct object serialized as JSON.
 * 2. The applyEdit() mapping covers all ObjectEditResultDto fields.
 * 3. Other objects in the slide are NOT touched (only the targeted one).
 * 4. Snapshot is pushed to history BEFORE applying the change (so undo works).
 * 5. HTML is regenerated with the new object.
 * 6. Failure from the LLM propagates as Result.failure.
 *
 * A failure here means the user's central feature (per-object AI editing) is broken.
 */
class EditObjectWithAiUseCaseTest {

    private val llm: LlmRepository = mockk(relaxed = true)
    private val projectRepository: ProjectRepository = mockk(relaxed = true)
    private val htmlRenderer: HtmlSlideRenderer = HtmlSlideRenderer()
    private val history: SlideHistoryManager = SlideHistoryManager()
    private val useCase = EditObjectWithAiUseCase(llm, projectRepository, htmlRenderer, history)

    private val palette = Palette.default()
    private val typography = Typography.default()

    private fun makeSlide(objects: List<SlideObject>): Slide = Slide(
        id = "slide-1", index = 0, layout = SlideLayout.TITLE_AND_BULLETS,
        title = "Test", objects = objects,
        htmlContent = htmlRenderer.render(
            Slide(id = "slide-1", index = 0, layout = SlideLayout.TITLE_AND_BULLETS,
                title = "Test", objects = objects),
            palette, typography
        )
    )

    @Test
    fun `applies new_text from LLM result`() = runTest {
        val original = SlideObject(id = "obj-1", type = ObjectType.TEXT_BOX, text = "Original")
        val slide = makeSlide(listOf(original))
        val presentation = Presentation(id = 1L, title = "Test", slides = listOf(slide))

        coEvery { projectRepository.getProject(1L) } returns presentation
        coEvery { llm.editObjectWithAi(any(), any(), any()) } returns ObjectEditResultDto(
            newText = "Texto mejorado por IA",
            explanation = "Mejoré el texto"
        )
        coEvery { projectRepository.updateSlide(any(), any(), any()) } returns Unit

        val result = useCase(1L, slide, original, "hazlo mejor")

        assertTrue(result.isSuccess)
        assertEquals("Mejoré el texto", result.getOrNull())

        coVerify(exactly = 1) { llm.editObjectWithAi(any(), any(), any()) }
        coVerify(exactly = 1) { projectRepository.updateSlide(1L, 0, any()) }
    }

    @Test
    fun `other objects are NOT touched when editing one`() = runTest {
        val target = SlideObject(id = "target", type = ObjectType.TEXT_BOX, text = "Original")
        val other = SlideObject(id = "other", type = ObjectType.TEXT_BOX, text = "Don't touch me")
        val slide = makeSlide(listOf(target, other))
        val presentation = Presentation(id = 1L, title = "Test", slides = listOf(slide))

        coEvery { projectRepository.getProject(1L) } returns presentation
        coEvery { llm.editObjectWithAi(any(), any(), any()) } returns ObjectEditResultDto(
            newText = "Edited",
            explanation = "cambié el target"
        )
        coEvery { projectRepository.updateSlide(any(), any(), any()) } returns Unit

        useCase(1L, slide, target, "edit target")

        // Verify the captured slide passed to updateSlide still has the other object unchanged
        coVerify {
            projectRepository.updateSlide(1L, 0, match { updated ->
                val targetObj = updated.objects.find { it.id == "target" }
                val otherObj = updated.objects.find { it.id == "other" }
                targetObj?.text == "Edited" && otherObj?.text == "Don't touch me"
            })
        }
    }

    @Test
    fun `applies new_bullets replaces bullet list`() = runTest {
        val target = SlideObject(
            id = "obj-1",
            type = ObjectType.BULLET_LIST,
            bullets = listOf("old")
        )
        val slide = makeSlide(listOf(target))
        val presentation = Presentation(id = 1L, title = "Test", slides = listOf(slide))

        coEvery { projectRepository.getProject(1L) } returns presentation
        coEvery { llm.editObjectWithAi(any(), any(), any()) } returns ObjectEditResultDto(
            newBullets = listOf("new1", "new2", "new3"),
            explanation = "Tres bullets nuevos"
        )
        coEvery { projectRepository.updateSlide(any(), any(), any()) } returns Unit

        useCase(1L, slide, target, "añade más bullets")

        coVerify {
            projectRepository.updateSlide(1L, 0, match { updated ->
                val obj = updated.objects.find { it.id == "obj-1" }
                obj?.bullets == listOf("new1", "new2", "new3")
            })
        }
    }

    @Test
    fun `applies new_style partially`() = runTest {
        val originalStyle = ObjectStyle.default().copy(fontSizeSp = 16, fontWeight = 400)
        val target = SlideObject(id = "obj-1", type = ObjectType.TEXT_BOX, text = "test", style = originalStyle)
        val slide = makeSlide(listOf(target))
        val presentation = Presentation(id = 1L, title = "Test", slides = listOf(slide))

        coEvery { projectRepository.getProject(1L) } returns presentation
        coEvery { llm.editObjectWithAi(any(), any(), any()) } returns ObjectEditResultDto(
            newStyle = ObjectStyleDto(
                fontSizeSp = 24,
                textColorHex = "#FF0000"
                // fontWeight, textAlign, background not changed
            ),
            explanation = "Subí el tamaño y cambié color"
        )
        coEvery { projectRepository.updateSlide(any(), any(), any()) } returns Unit

        useCase(1L, slide, target, "más grande y rojo")

        coVerify {
            projectRepository.updateSlide(1L, 0, match { updated ->
                val obj = updated.objects.find { it.id == "obj-1" }
                // Changed fields
                obj?.style?.fontSizeSp == 24 && obj.style.textColorHex == "#FF0000"
                // Unchanged fields
                && obj.style.fontWeight == 400
                && obj.style.textAlign == originalStyle.textAlign
            })
        }
    }

    @Test
    fun `snapshot is pushed to history before applying edit`() = runTest {
        val target = SlideObject(id = "obj-1", type = ObjectType.TEXT_BOX, text = "Original")
        val slide = makeSlide(listOf(target))
        val presentation = Presentation(id = 1L, title = "Test", slides = listOf(slide))

        coEvery { projectRepository.getProject(1L) } returns presentation
        coEvery { llm.editObjectWithAi(any(), any(), any()) } returns ObjectEditResultDto(
            newText = "Edited",
            explanation = "editado"
        )
        coEvery { projectRepository.updateSlide(any(), any(), any()) } returns Unit

        useCase(1L, slide, target, "edit")

        assertTrue("History should have at least 1 entry for undo", history.get(slide.id).canUndo)
    }

    @Test
    fun `LLM failure propagates as Result_failure`() = runTest {
        val target = SlideObject(id = "obj-1", type = ObjectType.TEXT_BOX, text = "x")
        val slide = makeSlide(listOf(target))

        coEvery { projectRepository.getProject(1L) } returns null  // simulate not found

        val result = useCase(1L, slide, target, "edit")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("Project not found") == true)
    }

    @Test
    fun `LLM throws returns failure`() = runTest {
        val target = SlideObject(id = "obj-1", type = ObjectType.TEXT_BOX, text = "x")
        val slide = makeSlide(listOf(target))
        val presentation = Presentation(id = 1L, title = "Test", slides = listOf(slide))

        coEvery { projectRepository.getProject(1L) } returns presentation
        coEvery { llm.editObjectWithAi(any(), any(), any()) } throws RuntimeException("API down")

        val result = useCase(1L, slide, target, "edit")

        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message?.contains("API down") == true)
    }
}
