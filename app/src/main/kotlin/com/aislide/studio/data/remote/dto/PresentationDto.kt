package com.aislide.studio.data.remote.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Structured presentation JSON returned by the LLM.
 *
 * The LLM is prompted to return this shape (with response_format=json_object)
 * so the app can deserialize it deterministically into domain models.
 *
 * This is the wire format between the LLM and the app — it does NOT touch Room
 * or UI; the [com.aislide.studio.data.model.Presentation] domain class does that.
 */
@Serializable
data class PresentationDto(
    val title: String,
    val description: String = "",
    val palette: PaletteDto,
    val typography: TypographyDto,
    val slides: List<SlideDto>
)

@Serializable
data class PaletteDto(
    val primary: String,
    val secondary: String,
    val accent: String,
    val background: String,
    val surface: String,
    @SerialName("on_primary") val onPrimary: String,
    @SerialName("on_background") val onBackground: String
)

@Serializable
data class TypographyDto(
    @SerialName("heading_font") val headingFont: String,
    @SerialName("body_font") val bodyFont: String,
    @SerialName("heading_weight") val headingWeight: Int = 600,
    @SerialName("body_weight") val bodyWeight: Int = 400
)

@Serializable
data class SlideDto(
    val index: Int,
    val layout: String,         // matches SlideLayout enum name
    val title: String = "",
    val subtitle: String = "",
    val bullets: List<String> = emptyList(),
    @SerialName("image_description") val imageDescription: String? = null,
    @SerialName("notes_for_speaker") val notesForSpeaker: String = ""
)

/**
 * What the LLM returns when asked to edit ONE object (the slide-by-slide AI
 * editor the user requested). The prompt constrains the LLM to only modify
 * the targeted object, never the rest of the slide.
 */
@Serializable
data class ObjectEditResultDto(
    val newText: String? = null,
    val newBullets: List<String>? = null,
    val newImageDescription: String? = null,
    val newStyle: ObjectStyleDto? = null,
    val explanation: String = ""
)

@Serializable
data class ObjectStyleDto(
    @SerialName("font_size_sp") val fontSizeSp: Int? = null,
    @SerialName("font_weight") val fontWeight: Int? = null,
    @SerialName("text_color_hex") val textColorHex: String? = null,
    @SerialName("text_align") val textAlign: String? = null,
    @SerialName("background_color_hex") val backgroundColorHex: String? = null
)
