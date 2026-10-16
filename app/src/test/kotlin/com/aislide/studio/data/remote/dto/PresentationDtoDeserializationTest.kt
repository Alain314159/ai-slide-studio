package com.aislide.studio.data.remote.dto

import com.aislide.studio.data.local.database.AppJson
import kotlinx.serialization.encodeToString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Serialization tests for [PresentationDto] — the structured JSON the LLM returns
 * when asked to generate a presentation.
 *
 * These verify that:
 * 1. snake_case aliases (heading_font, body_font, image_description, etc.) round-trip
 *    correctly — a single misnamed alias would silently drop a field.
 * 2. The schema accepts real GLM output with all fields present.
 * 3. The schema tolerates missing optional fields (image_description, notes_for_speaker).
 * 4. Invalid layout names don't crash deserialization (mapper falls back to default).
 *
 * A failure here means the AI-generated presentation JSON would be silently truncated.
 */
class PresentationDtoDeserializationTest {

    private val json = AppJson.default

    @Test
    fun `PresentationDto decodes from full LLM response`() {
        val llmJson = """
            {
              "title": "La Fotosíntesis",
              "description": "Presentación educativa sobre la fotosíntesis para 7mo grado",
              "palette": {
                "primary": "#2E7D32",
                "secondary": "#FFC107",
                "accent": "#7DD3C0",
                "background": "#0F1117",
                "surface": "#1A1D26",
                "on_primary": "#FFFFFF",
                "on_background": "#E8E9F0"
              },
              "typography": {
                "heading_font": "Poppins",
                "body_font": "Inter",
                "heading_weight": 600,
                "body_weight": 400
              },
              "slides": [
                {
                  "index": 0,
                  "layout": "TITLE",
                  "title": "La Fotosíntesis",
                  "subtitle": "Cómo las plantas fabrican su alimento",
                  "bullets": [],
                  "notes_for_speaker": "Bienvenidos a esta presentación..."
                },
                {
                  "index": 1,
                  "layout": "TITLE_AND_BULLETS",
                  "title": "¿Qué es la fotosíntesis?",
                  "bullets": ["Proceso químico", "Ocurre en las hojas", "Usa luz solar"],
                  "image_description": "Diagrama de una planta con flechas mostrando entrada de luz y salida de oxígeno",
                  "notes_for_speaker": "En esta diapositiva explicamos los fundamentos..."
                }
              ]
            }
        """.trimIndent()

        val dto = json.decodeFromString(PresentationDto.serializer(), llmJson)
        assertEquals("La Fotosíntesis", dto.title)
        assertEquals(2, dto.slides.size)
        assertEquals("TITLE", dto.slides[0].layout)
        assertEquals(3, dto.slides[1].bullets.size)
        assertEquals("Poppins", dto.typography.headingFont)
        assertEquals(600, dto.typography.headingWeight)
        assertEquals("#2E7D32", dto.palette.primary)
    }

    @Test
    fun `PresentationDto tolerates missing image_description`() {
        val llmJson = """
            {
              "title": "Test",
              "palette": {"primary":"#000000","secondary":"#fff","accent":"#fff","background":"#000","surface":"#111","on_primary":"#fff","on_background":"#fff"},
              "typography": {"heading_font":"Inter","body_font":"Inter"},
              "slides": [{"index":0,"layout":"TITLE","title":"Hello"}]
            }
        """.trimIndent()

        val dto = json.decodeFromString(PresentationDto.serializer(), llmJson)
        assertNull(dto.slides[0].imageDescription)
        assertNull(dto.slides[0].notesForSpeaker)
    }

    @Test
    fun `PresentationDto tolerates unknown layout name`() {
        val llmJson = """
            {
              "title": "Test",
              "palette": {"primary":"#000000","secondary":"#fff","accent":"#fff","background":"#000","surface":"#111","on_primary":"#fff","on_background":"#fff"},
              "typography": {"heading_font":"Inter","body_font":"Inter"},
              "slides": [{"index":0,"layout":"UNKNOWN_LAYOUT","title":"Hello"}]
            }
        """.trimIndent()

        val dto = json.decodeFromString(PresentationDto.serializer(), llmJson)
        // Field is just a string at the DTO level — the use case maps to enum and falls back
        assertEquals("UNKNOWN_LAYOUT", dto.slides[0].layout)
    }

    @Test
    fun `ObjectEditResultDto decodes with all fields null`() {
        val llmJson = """{"explanation":"No changes needed"}"""
        val dto = json.decodeFromString(ObjectEditResultDto.serializer(), llmJson)
        assertNull(dto.newText)
        assertNull(dto.newBullets)
        assertNull(dto.newImageDescription)
        assertNull(dto.newStyle)
        assertEquals("No changes needed", dto.explanation)
    }

    @Test
    fun `ObjectEditResultDto decodes with new_text and style`() {
        val llmJson = """
            {
              "new_text": "Texto mejorado",
              "new_style": {
                "font_size_sp": 24,
                "font_weight": 700,
                "text_color_hex": "#FF5722",
                "text_align": "CENTER"
              },
              "explanation": "Aumenté el tamaño y cambié el color"
            }
        """.trimIndent()
        val dto = json.decodeFromString(ObjectEditResultDto.serializer(), llmJson)
        assertEquals("Texto mejorado", dto.newText)
        assertEquals(24, dto.newStyle?.fontSizeSp)
        assertEquals(700, dto.newStyle?.fontWeight)
        assertEquals("CENTER", dto.newStyle?.textAlign)
    }

    @Test
    fun `ImageGenerationResponse decodes from OpenAI shape`() {
        val llmJson = """
            {
              "created": 1700000000,
              "data": [
                {"url": "https://example.com/img1.png"}
              ]
            }
        """.trimIndent()
        val dto = json.decodeFromString(ImageGenerationResponse.serializer(), llmJson)
        assertEquals(1, dto.data.size)
        assertEquals("https://example.com/img1.png", dto.data[0].url)
    }

    @Test
    fun `ImageGenerationResponse decodes b64_json variant`() {
        val llmJson = """
            {"created":0,"data":[{"b64_json":"aGVsbG8="}]}
        """.trimIndent()
        val dto = json.decodeFromString(ImageGenerationResponse.serializer(), llmJson)
        assertEquals("aGVsbG8=", dto.data[0].b64Json)
        assertNull(dto.data[0].url)
    }

    @Test
    fun `PaletteDto round-trips with snake_case fields`() {
        val original = PaletteDto(
            primary = "#000000",
            secondary = "#FFFFFF",
            accent = "#FF0000",
            background = "#1A1A1A",
            surface = "#2A2A2A",
            onPrimary = "#FFFFFF",
            onBackground = "#FFFFFF"
        )
        val encoded = json.encodeToString(original)
        assertTrue(encoded.contains("\"on_primary\""))
        assertTrue(encoded.contains("\"on_background\""))
        val decoded = json.decodeFromString(PaletteDto.serializer(), encoded)
        assertEquals(original, decoded)
    }
}
