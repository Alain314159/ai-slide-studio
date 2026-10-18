package com.aislide.studio.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests for [PresentationTemplates] — the Canva-style template gallery.
 *
 * Verifies:
 * 1. All 16 templates are present and well-formed.
 * 2. All palettes have 7 valid hex colors.
 * 3. Typography has non-empty fonts.
 * 4. byCategory() filters correctly.
 * 5. byId() returns null for unknown IDs.
 * 6. All template IDs are unique.
 * 7. All categories have at least one template.
 */
class PresentationTemplatesTest {

    @Test
    fun `all templates are present`() {
        assertEquals(16, PresentationTemplates.all.size)
    }

    @Test
    fun `all template IDs are unique`() {
        val ids = PresentationTemplates.all.map { it.id }
        assertEquals("IDs must be unique", ids.size, ids.toSet().size)
    }

    @Test
    fun `all templates have non-empty name and description`() {
        PresentationTemplates.all.forEach { template ->
            assertTrue("Template ${template.id} has empty name", template.name.isNotBlank())
            assertTrue("Template ${template.id} has empty description", template.description.isNotBlank())
            assertTrue("Template ${template.id} has empty promptHint", template.promptHint.isNotBlank())
        }
    }

    @Test
    fun `all templates have valid 7-color palette`() {
        PresentationTemplates.all.forEach { template ->
            val palette = template.palette
            val colors = listOf(
                palette.primary, palette.secondary, palette.accent,
                palette.background, palette.surface,
                palette.onPrimary, palette.onBackground
            )
            assertEquals("Template ${template.id} palette must have 7 colors", 7, colors.size)
            colors.forEach { hex ->
                assertTrue("Template ${template.id} has invalid hex: $hex", hex.matches(Regex("^#[0-9A-Fa-f]{6}$")))
            }
        }
    }

    @Test
    fun `all templates have non-empty typography`() {
        PresentationTemplates.all.forEach { template ->
            val t = template.typography
            assertTrue("Template ${template.id} has empty headingFont", t.headingFont.isNotBlank())
            assertTrue("Template ${template.id} has empty bodyFont", t.bodyFont.isNotBlank())
            assertTrue("Template ${template.id} headingWeight out of range", t.headingWeight in 100..900)
            assertTrue("Template ${template.id} bodyWeight out of range", t.bodyWeight in 100..900)
        }
    }

    @Test
    fun `byId returns matching template for known id`() {
        val template = PresentationTemplates.byId("edu-vibrant")
        assertNotNull(template)
        assertEquals("Educativo Vibrante", template?.name)
        assertEquals(TemplateCategory.EDUCATIVO, template?.category)
    }

    @Test
    fun `byId returns null for unknown id`() {
        assertEquals(null, PresentationTemplates.byId("nonexistent-template"))
        assertEquals(null, PresentationTemplates.byId(""))
    }

    @Test
    fun `byCategory filters correctly`() {
        TemplateCategory.entries.forEach { category ->
            val templates = PresentationTemplates.byCategory(category)
            assertTrue("Category $category should have at least 1 template", templates.isNotEmpty())
            templates.forEach { t ->
                assertEquals(category, t.category)
            }
        }
    }

    @Test
    fun `each category has at least 2 templates`() {
        // Spec: 2 templates per category, 8 categories = 16 total
        TemplateCategory.entries.forEach { category ->
            val count = PresentationTemplates.byCategory(category).size
            assertTrue("Category $category should have at least 2 templates, has $count", count >= 2)
        }
    }

    @Test
    fun `every template has a preview emoji`() {
        PresentationTemplates.all.forEach { template ->
            assertTrue("Template ${template.id} has empty emoji", template.previewEmoji.isNotBlank())
        }
    }

    @Test
    fun `categories enum has exactly 8 values`() {
        assertEquals(8, TemplateCategory.entries.size)
    }
}
