package com.aislide.studio.data.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * An individual object placed on a slide — title, bullet, image, shape, etc.
 *
 * Position is in NORMALIZED units (0.0..1.0 of slide width/height), so the
 * same object can be rendered at any resolution: WebView (HTML), SVG, or
 * PPTX EMU coordinates (1 inch = 914400 EMU).
 *
 * Each object is independently editable with the AI — tap the object, write
 * a prompt, and only that object is regenerated.
 */
@Serializable
data class SlideObject(
    val id: String,                  // UUID for stable identity across regenerations
    val type: ObjectType,
    val text: String = "",           // for text-based types
    val bullets: List<String> = emptyList(),  // for bullet lists
    val imageUrl: String? = null,    // local path or remote URL
    val imageDescription: String? = null,
    val position: ObjectPosition = ObjectPosition(0f, 0f, 1f, 0.2f),  // default top-left, full-width, 20% height
    val style: ObjectStyle = ObjectStyle.default(),
    val metadata: Map<String, String> = emptyMap()
)

@Serializable
enum class ObjectType {
    TITLE,
    SUBTITLE,
    TEXT_BOX,
    BULLET_LIST,
    IMAGE,
    SHAPE_RECTANGLE,
    SHAPE_CIRCLE,
    SHAPE_ARROW,
    SHAPE_LINE,
    TABLE,
    CHART_BAR,
    CHART_LINE,
    CHART_PIE,
    ICON,
    BACKGROUND_IMAGE
}

@Serializable
data class ObjectPosition(
    val x: Float,            // 0..1 of slide width
    val y: Float,            // 0..1 of slide height
    val width: Float,        // 0..1 of slide width
    val height: Float,       // 0..1 of slide height
    val rotation: Float = 0f // degrees, 0..360
)

@Serializable
data class ObjectStyle(
    val fontFamily: String = "Inter",
    val fontSizeSp: Int = 16,
    val fontWeight: Int = 400,
    val textColorHex: String = "#FFFFFF",
    val backgroundColorHex: String? = null,
    val textAlign: TextAlign = TextAlign.LEFT,
    val borderRadiusDp: Int = 0,
    val opacity: Float = 1.0f,
    val hasShadow: Boolean = false,
    val zIndex: Int = 0
) {
    companion object {
        fun default() = ObjectStyle()
    }
}

@Serializable
enum class TextAlign { LEFT, CENTER, RIGHT, JUSTIFIED }
