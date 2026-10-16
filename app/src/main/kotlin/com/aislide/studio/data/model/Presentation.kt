package com.aislide.studio.data.model

import kotlinx.serialization.Serializable

/**
 * A presentation project. Top-level container for slides.
 *
 * Stored as a Room row (entity) + DataStore prefs for the API key,
 * the slides themselves live in their own [SlideEntity] table.
 *
 * Immutable by design — Rule 6 of kotlin skill: emit new copies via copy()
 * rather than mutating fields.
 */
@Serializable
data class Presentation(
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val palette: Palette = Palette.default(),
    val typography: Typography = Typography.default(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val slides: List<Slide> = emptyList()
) {
    val slideCount: Int get() = slides.size
}

/**
 * Color palette for the presentation. AI generates this per-project so each
 * presentation looks unique — no shared templates.
 */
@Serializable
data class Palette(
    val primary: String,    // hex, e.g. "#2E7D32"
    val secondary: String,
    val accent: String,
    val background: String,
    val surface: String,
    val onPrimary: String,
    val onBackground: String
) {
    companion object {
        fun default() = Palette(
            primary = "#5B6CFF",
            secondary = "#FFC857",
            accent = "#7DD3C0",
            background = "#0F1117",
            surface = "#1A1D26",
            onPrimary = "#FFFFFF",
            onBackground = "#E8E9F0"
        )
    }
}

/**
 * Typography pairing. Each presentation has its own font pairing chosen by the AI.
 */
@Serializable
data class Typography(
    val headingFont: String, // e.g. "Poppins"
    val bodyFont: String,    // e.g. "Inter"
    val headingWeight: Int = 600,
    val bodyWeight: Int = 400
) {
    companion object {
        fun default() = Typography("Poppins", "Inter")
    }
}
