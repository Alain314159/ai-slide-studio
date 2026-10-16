package com.aislide.studio.domain.usecase

import com.aislide.studio.data.model.Palette
import com.aislide.studio.data.model.Presentation
import com.aislide.studio.data.model.Slide
import com.aislide.studio.data.model.SlideLayout
import com.aislide.studio.data.model.SlideObject
import com.aislide.studio.data.model.ObjectPosition
import com.aislide.studio.data.model.ObjectStyle
import com.aislide.studio.data.model.ObjectType
import com.aislide.studio.data.model.TextAlign
import com.aislide.studio.data.model.Typography
import com.aislide.studio.data.remote.dto.PresentationDto
import com.aislide.studio.data.repository.LlmRepository
import com.aislide.studio.data.repository.ProjectRepository
import com.aislide.studio.domain.service.HtmlSlideRenderer
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates a complete presentation from a free-text prompt.
 *
 * Steps:
 * 1. Calls the LLM with the user's prompt (returns PresentationDto).
 * 2. Maps the DTO to a [Presentation] domain model with [SlideObject]s positioned
 *    according to each slide's layout.
 * 3. Renders each slide to HTML via [HtmlSlideRenderer].
 * 4. Persists the project via [ProjectRepository].
 *
 * Returns the new project ID.
 */
@Singleton
class GeneratePresentationUseCase @Inject constructor(
    private val llmRepository: LlmRepository,
    private val projectRepository: ProjectRepository,
    private val htmlRenderer: HtmlSlideRenderer
) {
    suspend operator fun invoke(prompt: String): Result<Long> = runCatching {
        val dto = llmRepository.generatePresentation(prompt)
        val presentation = mapDtoToPresentation(dto).let { p ->
            p.copy(slides = p.slides.map { slide ->
                slide.copy(htmlContent = htmlRenderer.render(slide, p.palette, p.typography))
            })
        }
        projectRepository.insertPresentation(presentation)
    }

    private fun mapDtoToPresentation(dto: PresentationDto): Presentation {
        val palette = Palette(
            primary = dto.palette.primary,
            secondary = dto.palette.secondary,
            accent = dto.palette.accent,
            background = dto.palette.background,
            surface = dto.palette.surface,
            onPrimary = dto.palette.onPrimary,
            onBackground = dto.palette.onBackground
        )
        val typography = Typography(
            headingFont = dto.typography.headingFont,
            bodyFont = dto.typography.bodyFont,
            headingWeight = dto.typography.headingWeight,
            bodyWeight = dto.typography.bodyWeight
        )
        val slides = dto.slides.map { slideDto ->
            val layout = runCatching { SlideLayout.valueOf(slideDto.layout) }
                .getOrDefault(SlideLayout.TITLE_AND_BULLETS)
            Slide(
                id = UUID.randomUUID().toString(),
                index = slideDto.index,
                layout = layout,
                title = slideDto.title,
                subtitle = slideDto.subtitle,
                objects = buildObjectsForLayout(layout, slideDto),
                notesForSpeaker = slideDto.notesForSpeaker
            )
        }
        return Presentation(
            title = dto.title,
            description = dto.description,
            palette = palette,
            typography = typography,
            slides = slides
        )
    }

    private fun buildObjectsForLayout(
        layout: SlideLayout,
        slideDto: com.aislide.studio.data.remote.dto.SlideDto
    ): List<SlideObject> {
        val baseStyle = ObjectStyle.default()
        val titleObj = SlideObject(
            id = UUID.randomUUID().toString(),
            type = ObjectType.TITLE,
            text = slideDto.title,
            position = ObjectPosition(0.08f, 0.12f, 0.84f, 0.16f),
            style = baseStyle.copy(fontFamily = "Poppins", fontSizeSp = 48, fontWeight = 700)
        )
        val bulletsObj = slideDto.bullets.takeIf { it.isNotEmpty() }?.let {
            SlideObject(
                id = UUID.randomUUID().toString(),
                type = ObjectType.BULLET_LIST,
                bullets = it,
                position = ObjectPosition(0.08f, 0.35f, 0.84f, 0.55f),
                style = baseStyle.copy(fontFamily = "Inter", fontSizeSp = 22, fontWeight = 400)
            )
        }
        return listOfNotNull(titleObj, bulletsObj)
    }
}
