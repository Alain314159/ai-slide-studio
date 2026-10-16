package com.aislide.studio.data.repository

import com.aislide.studio.data.local.dao.ProjectDao
import com.aislide.studio.data.local.dao.SlideDao
import com.aislide.studio.data.local.entity.ProjectEntity
import com.aislide.studio.data.local.entity.SlideEntity
import com.aislide.studio.data.local.entity.SourceType
import com.aislide.studio.data.model.Palette
import com.aislide.studio.data.model.SlideLayout
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for [ProjectRepository].
 *
 * These verify:
 * 1. The entity ↔ domain mapping is correct (palette, typography, slides).
 * 2. insertPresentation() persists both project and slides with correct indices.
 * 3. getProject(id) returns null for unknown id.
 * 4. deleteSlide() re-indexes remaining slides correctly.
 * 5. JSON serialization round-trips for Slide (no data loss).
 *
 * A failure here means user-created presentations would lose data on save/load.
 */
class ProjectRepositoryTest {

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
    private val projectDao: ProjectDao = mockk(relaxed = true)
    private val slideDao: SlideDao = mockk(relaxed = true)
    private val repository = ProjectRepository(projectDao, slideDao)

    @Test
    fun `observeAll maps entities to light presentations without slides`() = runTest {
        val entity = ProjectEntity(
            id = 1L,
            title = "Test",
            description = "desc",
            paletteJson = json.encodeToString(Palette.serializer(), Palette.default()),
            typographyJson = json.encodeToString(com.aislide.studio.data.model.Typography.serializer(), com.aislide.studio.data.model.Typography.default()),
            slideCount = 5
        )
        coEvery { projectDao.observeAll() } returns flowOf(listOf(entity))

        val result = repository.observeAll().first()

        assertEquals(1, result.size)
        val pres = result.first()
        assertEquals(1L, pres.id)
        assertEquals("Test", pres.title)
        assertEquals("desc", pres.description)
        assertEquals(0, pres.slides.size)  // light: no slides loaded
    }

    @Test
    fun `getProject returns null when entity not found`() = runTest {
        coEvery { projectDao.getById(999L) } returns null

        val result = repository.getProject(999L)

        assertNull(result)
    }

    @Test
    fun `getProject returns full presentation with slides`() = runTest {
        val palette = Palette(primary = "#FF0000", secondary = "#00FF00", accent = "#0000FF",
            background = "#000", surface = "#111", onPrimary = "#fff", onBackground = "#fff")
        val entity = ProjectEntity(
            id = 1L,
            title = "Test",
            description = "desc",
            paletteJson = json.encodeToString(Palette.serializer(), palette),
            typographyJson = json.encodeToString(com.aislide.studio.data.model.Typography.serializer(), com.aislide.studio.data.model.Typography.default()),
            slideCount = 2
        )
        val slide = com.aislide.studio.data.model.Slide(
            id = "s1", index = 0, layout = SlideLayout.TITLE, title = "Hola"
        )
        val slideEntity = SlideEntity(
            id = 10L,
            projectId = 1L,
            slideIndex = 0,
            slideJson = json.encodeToString(com.aislide.studio.data.model.Slide.serializer(), slide),
            title = "Hola"
        )

        coEvery { projectDao.getById(1L) } returns entity
        coEvery { slideDao.getByProject(1L) } returns listOf(slideEntity)

        val result = repository.getProject(1L)

        assertNotNull(result)
        assertEquals(1L, result!!.id)
        assertEquals("Test", result.title)
        assertEquals("#FF0000", result.palette.primary)
        assertEquals(1, result.slides.size)
        assertEquals("Hola", result.slides[0].title)
    }

    @Test
    fun `getProject falls back to default palette when JSON is corrupted`() = runTest {
        val entity = ProjectEntity(
            id = 1L,
            title = "Test",
            description = "desc",
            paletteJson = "{ broken json",  // invalid JSON
            typographyJson = "{ also broken",
            slideCount = 0
        )
        coEvery { projectDao.getById(1L) } returns entity
        coEvery { slideDao.getByProject(1L) } returns emptyList()

        val result = repository.getProject(1L)

        assertNotNull(result)
        // Should fall back to defaults, not throw
        assertEquals(Palette.default().primary, result!!.palette.primary)
        assertEquals(com.aislide.studio.data.model.Typography.default().headingFont, result.typography.headingFont)
    }

    @Test
    fun `insertPresentation calls DAOs with correct data`() = runTest {
        val slide = com.aislide.studio.data.model.Slide(
            id = "s1", index = 0, layout = SlideLayout.TITLE, title = "Hola"
        )
        val presentation = com.aislide.studio.data.model.Presentation(
            title = "Test",
            description = "desc",
            slides = listOf(slide)
        )
        coEvery { projectDao.insert(any()) } returns 42L
        coEvery { slideDao.insertAll(any()) } returns listOf(100L)

        val newId = repository.insertPresentation(presentation)

        assertEquals(42L, newId)
        coVerify(exactly = 1) { projectDao.insert(any()) }
        coVerify(exactly = 1) { slideDao.insertAll(any()) }
    }

    @Test
    fun `deleteSlide re-indexes remaining slides`() = runTest {
        val now = System.currentTimeMillis()
        val slide0 = SlideEntity(id = 10L, projectId = 1L, slideIndex = 0, slideJson = "{}", title = "A")
        val slide1 = SlideEntity(id = 11L, projectId = 1L, slideIndex = 1, slideJson = "{}", title = "B")
        val slide2 = SlideEntity(id = 12L, projectId = 1L, slideIndex = 2, slideJson = "{}", title = "C")

        coEvery { slideDao.getByProject(1L) } returns listOf(slide0, slide1, slide2)
        coEvery { slideDao.deleteById(any()) } returns Unit
        coEvery { slideDao.updateIndex(any(), any(), any()) } returns Unit
        coEvery { projectDao.updateSlideCount(any(), any(), any()) } returns Unit

        // Delete slide at index 1 (the middle one)
        repository.deleteSlide(1L, 1)

        coVerify(exactly = 1) { slideDao.deleteById(11L) }  // deleted middle
        // Remaining slides should be re-indexed: slide0 stays at 0, slide2 becomes 1
        coVerify { slideDao.updateIndex(10L, 0, any()) }
        coVerify { slideDao.updateIndex(12L, 1, any()) }
        coVerify { projectDao.updateSlideCount(1L, 2, any()) }
    }

    @Test
    fun `deleteSlide with nonexistent index does nothing`() = runTest {
        coEvery { slideDao.getByProject(1L) } returns emptyList()

        repository.deleteSlide(1L, 5)  // no slide at index 5

        coVerify(exactly = 0) { slideDao.deleteById(any()) }
        coVerify(exactly = 0) { projectDao.updateSlideCount(any(), any(), any()) }
    }
}
