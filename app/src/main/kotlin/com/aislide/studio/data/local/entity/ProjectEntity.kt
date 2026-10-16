package com.aislide.studio.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for a presentation project.
 *
 * Stores palette and typography as JSON strings (TypeConverter handles this).
 * The slides themselves live in [SlideEntity] — one row per slide, ordered by
 * `slideIndex` so reordering is cheap and stable.
 */
@Entity(
    tableName = "projects",
    indices = [Index("createdAt"), Index("updatedAt")]
)
data class ProjectEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String,
    val description: String = "",
    val paletteJson: String,         // serialized Palette
    val typographyJson: String,      // serialized Typography
    val coverColorHex: String = "#1A1D26",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val slideCount: Int = 0,
    val sourceType: SourceType = SourceType.CREATED    // CREATED vs IMPORTED from .pptx
)

enum class SourceType { CREATED, IMPORTED }
