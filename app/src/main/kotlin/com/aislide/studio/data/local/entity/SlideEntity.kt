package com.aislide.studio.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for a slide.
 *
 * Foreign key to projects with CASCADE delete: if a project is deleted,
 * all its slides go with it (no orphan rows).
 *
 * The slide's JSON structure (objects, layout, palette overrides) is stored
 * as a single JSON column — keeping schema stable across iterations while
 * the [Slide] data class evolves.
 */
@Entity(
    tableName = "slides",
    foreignKeys = [
        ForeignKey(
            entity = ProjectEntity::class,
            parentColumns = ["id"],
            childColumns = ["projectId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("projectId"),
        Index(value = ["projectId", "slideIndex"], unique = true)
    ]
)
data class SlideEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val projectId: Long,
    val slideIndex: Int,             // 0-based position; unique per project
    val slideJson: String,           // serialized Slide (objects, layout, etc.)
    val title: String = "",          // denormalized for fast list display
    val notesForSpeaker: String = "",
    val thumbnailPath: String? = null,  // local cache of rendered thumbnail
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
)
