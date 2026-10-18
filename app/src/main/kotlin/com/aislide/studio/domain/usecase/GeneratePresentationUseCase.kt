package com.aislide.studio.domain.usecase

import com.aislide.studio.data.local.database.AppJson
import com.aislide.studio.data.model.ObjectPosition
import com.aislide.studio.data.model.ObjectStyle
import com.aislide.studio.data.model.ObjectType
import com.aislide.studio.data.model.Palette
import com.aislide.studio.data.model.Presentation
import com.aislide.studio.data.model.PresentationTemplate
import com.aislide.studio.data.model.Slide
import com.aislide.studio.data.model.SlideLayout
import com.aislide.studio.data.model.SlideObject
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
 * v1.2: now accepts an optional [PresentationTemplate] to apply a specific
 * palette + typography + prompt hint to guide the LLM's output style.
 *
 * If template is null, the LLM chooses its own palette (default behavior).
 *
 * Steps:
 * 1. Calls the LLM with the user's prompt + template's promptHint (if any).
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
    suspend operator fun invoke(
        prompt: String,
        template: PresentationTemplate? = null
    ): Result<Long> = runCatching {
        val dto = llmRepository.generatePresentation(prompt, template)
        val presentation = mapDtoToPresentation(dto, template).let { p ->
            p.copy(slides = p.slides.map { slide ->
                slide.copy(htmlContent = htmlRenderer.render(slide, p.palette, p.typography))
            })
        }
        projectRepository.insertPresentation(presentation)
    }

    private fun mapDtoToPresentation(
        dto: PresentationDto,
        template: PresentationTemplate?
    ): Presentation {
        // If we have a template, use its palette and typography (overrides what the LLM returned)
        val palette = template?.palette ?: Palette(
            primary = dto.palette.primary,
            secondary = dto.palette.secondary,
            accent = dto.palette.accent,
            background = dto.palette.background,
            surface = dto.palette.surface,
            onPrimary = dto.palette.onPrimary,
            onBackground = dto.palette.onBackground
        )
        val typography = template?.typography ?: Typography(
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
                objects = buildObjectsForLayout(layout, slideDto, typography),
                notesForSpeaker = slideDto.notesForSpeaker ?: ""
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
        slideDto: com.aislide.studio.data.remote.dto.SlideDto,
        typography: Typography
    ): List<SlideObject> {
        val headingFont = typography.headingFont
        val bodyFont = typography.bodyFont
        val titleObj = SlideObject(
            id = UUID.randomUUID().toString(),
            type = ObjectType.TITLE,
            text = slideDto.title,
            position = ObjectPosition(0.08f, 0.12f, 0.84f, 0.16f),
            style = ObjectStyle.default().copy(
                fontFamily = headingFont,
                fontSizeSp = 48,
                fontWeight = typography.headingWeight,
                textAlign = TextAlign.LEFT
            )
        )
        val bulletsObj = slideDto.bullets.takeIf { it.isNotEmpty() }?.let {
            SlideObject(
                id = UUID.randomUUID().toString(),
                type = ObjectType.BULLET_LIST,
                bullets = it,
                position = ObjectPosition(0.08f, 0.35f, 0.84f, 0.55f),
                style = ObjectStyle.default().copy(
                    fontFamily = bodyFont,
                    fontSizeSp = 22,
                    fontWeight = typography.bodyWeight,
                    textAlign = TextAlign.LEFT
                )
            )
        }
        return listOfNotNull(titleObj, bulletsObj)
    }
}
