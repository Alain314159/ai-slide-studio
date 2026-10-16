package com.aislide.studio.data.repository

import com.aislide.studio.data.local.dao.ProjectDao
import com.aislide.studio.data.local.dao.SlideDao
import com.aislide.studio.data.local.database.AppJson
import com.aislide.studio.data.local.entity.ProjectEntity
import com.aislide.studio.data.local.entity.SlideEntity
import com.aislide.studio.data.local.entity.SourceType
import com.aislide.studio.data.model.Palette
import com.aislide.studio.data.model.Presentation
import com.aislide.studio.data.model.Slide
import com.aislide.studio.data.model.Typography
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for [Presentation] projects.
 *
 * Single source of truth: every read/write goes through here. ViewModels
 * never touch DAOs directly — Rule of android skill (architecture.md):
 * the second consumer of the same data is the trigger to add this layer.
 *
 * Maps between Room entities (storage) and domain models (Presentation/Slide).
 */
@Singleton
class ProjectRepository @Inject constructor(
    private val projectDao: ProjectDao,
    private val slideDao: SlideDao
) {
    private val json = AppJson.default

    fun observeAll(): Flow<List<Presentation>> =
        projectDao.observeAll().map { entities ->
            entities.map { it.toPresentationLight() }
        }

    fun observeProject(id: Long): Flow<Presentation?> =
        projectDao.observeById(id).map { entity ->
            entity?.let { e ->
                val slides = slideDao.getByProject(e.id).map { it.toSlide() }
                e.toPresentation(slides)
            }
        }

    suspend fun getProject(id: Long): Presentation? {
        val entity = projectDao.getById(id) ?: return null
        val slides = slideDao.getByProject(id).map { it.toSlide() }
        return entity.toPresentation(slides)
    }

    /**
     * Inserts a fully-formed presentation (with slides). Used after AI generation
     * or .pptx import. Returns the new project ID.
     */
    suspend fun insertPresentation(presentation: Presentation): Long {
        val now = System.currentTimeMillis()
        val projectId = projectDao.insert(
            ProjectEntity(
                title = presentation.title,
                description = presentation.description,
                paletteJson = json.encodeToString(Palette.serializer(), presentation.palette),
                typographyJson = json.encodeToString(Typography.serializer(), presentation.typography),
                createdAt = now,
                updatedAt = now,
                slideCount = presentation.slides.size,
                sourceType = SourceType.CREATED
            )
        )
        slideDao.insertAll(presentation.slides.mapIndexed { idx, slide ->
            SlideEntity(
                projectId = projectId,
                slideIndex = idx,
                slideJson = json.encodeToString(Slide.serializer(), slide),
                title = slide.title,
                notesForSpeaker = slide.notesForSpeaker
            )
        })
        return projectId
    }

    suspend fun rename(id: Long, title: String) =
        projectDao.rename(id, title)

    suspend fun delete(id: Long) = projectDao.deleteById(id)

    suspend fun updateSlide(projectId: Long, slideIndex: Int, slide: Slide) {
        val existing = slideDao.getByIndex(projectId, slideIndex)
        val now = System.currentTimeMillis()
        if (existing != null) {
            slideDao.update(
                existing.copy(
                    slideJson = json.encodeToString(Slide.serializer(), slide),
                    title = slide.title,
                    notesForSpeaker = slide.notesForSpeaker,
                    updatedAt = now
                )
            )
        }
    }

    suspend fun addSlide(projectId: Long, slide: Slide) {
        val nextIndex = (slideDao.maxIndex(projectId) ?: -1) + 1
        slideDao.insert(
            SlideEntity(
                projectId = projectId,
                slideIndex = nextIndex,
                slideJson = json.encodeToString(Slide.serializer(), slide.copy(index = nextIndex)),
                title = slide.title
            )
        )
        projectDao.updateSlideCount(projectId, nextIndex + 1)
    }

    suspend fun deleteSlide(projectId: Long, slideIndex: Int) {
        val slides = slideDao.getByProject(projectId).toMutableList()
        val toDelete = slides.firstOrNull { it.slideIndex == slideIndex } ?: return
        slideDao.deleteById(toDelete.id)
        // Re-index remaining slides
        val remaining = slides.filter { it.slideIndex != slideIndex }.sortedBy { it.slideIndex }
        remaining.forEachIndexed { i, s -> slideDao.updateIndex(s.id, i) }
        projectDao.updateSlideCount(projectId, remaining.size)
    }

    // === Mappers ===

    private fun ProjectEntity.toPresentationLight(): Presentation =
        Presentation(
            id = id,
            title = title,
            description = description,
            palette = runCatching { json.decodeFromString(Palette.serializer(), paletteJson) }.getOrDefault(Palette.default()),
            typography = runCatching { json.decodeFromString(Typography.serializer(), typographyJson) }.getOrDefault(Typography.default()),
            createdAt = createdAt,
            updatedAt = updatedAt,
            slides = emptyList()  // light: don't load slides for the list
        )

    private fun ProjectEntity.toPresentation(slides: List<Slide>): Presentation =
        Presentation(
            id = id,
            title = title,
            description = description,
            palette = runCatching { json.decodeFromString(Palette.serializer(), paletteJson) }.getOrDefault(Palette.default()),
            typography = runCatching { json.decodeFromString(Typography.serializer(), typographyJson) }.getOrDefault(Typography.default()),
            createdAt = createdAt,
            updatedAt = updatedAt,
            slides = slides
        )

    private fun SlideEntity.toSlide(): Slide =
        runCatching { json.decodeFromString(Slide.serializer(), slideJson) }
            .getOrElse {
                Slide(id = UUID.randomUUID().toString(), index = slideIndex, layout = com.aislide.studio.data.model.SlideLayout.TITLE_AND_BULLETS)
            }
}
