package com.aislide.studio.domain.usecase

import com.aislide.studio.data.remote.dto.PaletteDto
import com.aislide.studio.data.remote.dto.PresentationDto
import com.aislide.studio.data.remote.dto.SlideDto
import com.aislide.studio.data.remote.dto.TypographyDto
import com.aislide.studio.data.repository.LlmRepository
import com.aislide.studio.data.repository.ProjectRepository
import com.aislide.studio.data.model.SlideLayout
import com.aislide.studio.domain.service.HtmlSlideRenderer
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [GeneratePresentationUseCase].
 *
 * These verify:
 * 1. The LLM is called once with the user's prompt.
 * 2. The DTO is correctly mapped to a Presentation domain object.
 * 3. Each slide's HTML is generated (non-empty htmlContent).
 * 4. The TITLE_AND_BULLETS layout produces title + bullets objects.
 * 5. Unknown layout names fall back to a default.
 * 6. The project is persisted via the repository.
 *
 * A failure here means AI-generated presentations would be missing fields or
 * fail to render.
 */
class GeneratePresentationUseCaseTest {

    private val llm: LlmRepository = mockk(relaxed = true)
    private val projectRepository: ProjectRepository = mockk(relaxed = true)
    private val htmlRenderer: HtmlSlideRenderer = HtmlSlideRenderer()
    private val useCase = GeneratePresentationUseCase(llm, projectRepository, htmlRenderer)

    @Test
    fun `calls LLM once and persists the presentation`() = runTest {
        val dto = PresentationDto(
            title = "Test",
            description = "desc",
            palette = PaletteDto(
                primary = "#000000", secondary = "#FFFFFF", accent = "#FF0000",
                background = "#1A1A1A", surface = "#2A2A2A",
                onPrimary = "#FFFFFF", onBackground = "#FFFFFF"
            ),
            typography = TypographyDto(
                headingFont = "Poppins",
                bodyFont = "Inter",
                headingWeight = 600,
                bodyWeight = 400
            ),
            slides = listOf(
                SlideDto(
                    index = 0,
                    layout = "TITLE",
                    title = "Portada",
                    subtitle = "Subtítulo",
                    bullets = emptyList(),
                    notesForSpeaker = "Hola"
                )
            )
        )

        coEvery { llm.generatePresentation(any()) } returns dto
        coEvery { projectRepository.insertPresentation(any()) } returns 42L

        val result = useCase("un prompt")

        assertTrue(result.isSuccess)
        assertEquals(42L, result.getOrNull())

        coVerify(exactly = 1) { llm.generatePresentation("un prompt") }
        coVerify(exactly = 1) { projectRepository.insertPresentation(any()) }
    }

    @Test
    fun `TITLE_AND_BULLETS layout produces title and bullets objects`() = runTest {
        val dto = PresentationDto(
            title = "T",
            palette = PaletteDto("#000","#fff","#fff","#000","#111","#fff","#fff"),
            typography = TypographyDto("Inter", "Inter"),
            slides = listOf(
                SlideDto(
                    index = 0,
                    layout = "TITLE_AND_BULLETS",
                    title = "Title",
                    bullets = listOf("one", "two")
                )
            )
        )
        coEvery { llm.generatePresentation(any()) } returns dto
        coEvery { projectRepository.insertPresentation(any()) } returns 1L

        useCase("prompt")

        // Verify that insertPresentation received a Presentation with at least 2 objects on the slide
        coVerify {
            projectRepository.insertPresentation(match { pres ->
                val slide = pres.slides.firstOrNull()
                slide != null && slide.objects.size >= 2 &&
                    slide.objects.any { it.type == com.aislide.studio.data.model.ObjectType.TITLE } &&
                    slide.objects.any { it.type == com.aislide.studio.data.model.ObjectType.BULLET_LIST }
            })
        }
    }

    @Test
    fun `unknown layout falls back to TITLE_AND_BULLETS`() = runTest {
        val dto = PresentationDto(
            title = "T",
            palette = PaletteDto("#000","#fff","#fff","#000","#111","#fff","#fff"),
            typography = TypographyDto("Inter", "Inter"),
            slides = listOf(
                SlideDto(index = 0, layout = "UNKNOWN_LAYOUT_XYZ", title = "t")
            )
        )
        coEvery { llm.generatePresentation(any()) } returns dto
        coEvery { projectRepository.insertPresentation(any()) } returns 1L

        useCase("prompt")

        coVerify {
            projectRepository.insertPresentation(match { pres ->
                pres.slides.firstOrNull()?.layout == SlideLayout.TITLE_AND_BULLETS
            })
        }
    }

    @Test
    fun `each slide gets non-empty htmlContent`() = runTest {
        val dto = PresentationDto(
            title = "T",
            palette = PaletteDto("#000","#fff","#fff","#000","#111","#fff","#fff"),
            typography = TypographyDto("Inter", "Inter"),
            slides = listOf(
                SlideDto(index = 0, layout = "TITLE", title = "S1"),
                SlideDto(index = 1, layout = "TITLE_AND_BULLETS", title = "S2", bullets = listOf("a", "b")),
                SlideDto(index = 2, layout = "CONCLUSION", title = "S3")
            )
        )
        coEvery { llm.generatePresentation(any()) } returns dto
        coEvery { projectRepository.insertPresentation(any()) } returns 1L

        useCase("prompt")

        coVerify {
            projectRepository.insertPresentation(match { pres ->
                pres.slides.all { it.htmlContent.isNotBlank() && it.htmlContent.contains("<!DOCTYPE html>") }
            })
        }
    }

    @Test
    fun `LLM failure propagates`() = runTest {
        coEvery { llm.generatePresentation(any()) } throws RuntimeException("network")

        val result = useCase("prompt")

        assertTrue(result.isFailure)
        assertEquals("network", result.exceptionOrNull()?.message)
    }

    @Test
    fun `palette is mapped from dto`() = runTest {
        val dto = PresentationDto(
            title = "T",
            palette = PaletteDto(
                primary = "#FF0000",
                secondary = "#00FF00",
                accent = "#0000FF",
                background = "#111111",
                surface = "#222222",
                onPrimary = "#FFFFFF",
                onBackground = "#EEEEEE"
            ),
            typography = TypographyDto("Inter", "Inter"),
            slides = emptyList()
        )
        coEvery { llm.generatePresentation(any()) } returns dto
        coEvery { projectRepository.insertPresentation(any()) } returns 1L

        useCase("prompt")

        coVerify {
            projectRepository.insertPresentation(match { pres ->
                pres.palette.primary == "#FF0000" &&
                pres.palette.accent == "#0000FF" &&
                pres.palette.onBackground == "#EEEEEE"
            })
        }
    }
}
