package com.aislide.studio.domain.service

import com.aislide.studio.data.model.ObjectPosition
import com.aislide.studio.data.model.ObjectStyle
import com.aislide.studio.data.model.ObjectType
import com.aislide.studio.data.model.Palette
import com.aislide.studio.data.model.Slide
import com.aislide.studio.data.model.SlideLayout
import com.aislide.studio.data.model.SlideObject
import com.aislide.studio.data.model.TextAlign
import com.aislide.studio.data.model.Typography
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [HtmlSlideRenderer].
 *
 * These verify:
 * 1. The rendered HTML contains the expected structural markers
 *    (DOCTYPE, slide div, object divs with data-obj-id).
 * 2. Position percentages are correctly formatted from normalized 0..1 coords.
 * 3. Bullet lists render as <ul><li>…</li></ul> with escaped text.
 * 4. Interactive mode injects the JS bridge script.
 * 5. Non-interactive mode omits the JS.
 * 6. Text is HTML-escaped (& < > and newlines → <br>).
 * 7. Background override (slide.backgroundHex) takes precedence over palette.
 *
 * A failure here means either:
 * - The WebView would render broken markup
 * - The PptxExportEngine parser would fail to extract objects
 * - The JS tap-to-select wouldn't fire on objects
 */
class HtmlSlideRendererTest {

    private val renderer = HtmlSlideRenderer()
    private val palette = Palette.default()
    private val typography = Typography.default()

    private fun makeSlide(
        title: String = "Test",
        objects: List<SlideObject> = emptyList(),
        backgroundHex: String? = null
    ) = Slide(
        id = "slide-1",
        index = 0,
        layout = SlideLayout.TITLE_AND_BULLETS,
        title = title,
        objects = objects,
        backgroundHex = backgroundHex
    )

    @Test
    fun `render returns full HTML document with DOCTYPE`() {
        val html = renderer.render(makeSlide(), palette, typography)
        assertTrue("HTML must start with DOCTYPE", html.startsWith("<!DOCTYPE html>"))
        assertTrue(html.contains("<html"))
        assertTrue(html.contains("</html>"))
    }

    @Test
    fun `render includes slide div with data attributes`() {
        val html = renderer.render(makeSlide(), palette, typography)
        assertTrue(html.contains("class=\"slide\""))
        assertTrue(html.contains("data-slide-id=\"slide-1\""))
        assertTrue(html.contains("data-slide-index=\"0\""))
    }

    @Test
    fun `render includes object divs with data-obj-id`() {
        val obj = SlideObject(
            id = "obj-1",
            type = ObjectType.TITLE,
            text = "Mi Título",
            position = ObjectPosition(0.1f, 0.1f, 0.8f, 0.2f)
        )
        val html = renderer.render(makeSlide(objects = listOf(obj)), palette, typography)
        assertTrue(html.contains("data-obj-id=\"obj-1\""))
        assertTrue(html.contains("Mi Título"))
    }

    @Test
    fun `render formats position as percentages`() {
        val obj = SlideObject(
            id = "obj-1",
            type = ObjectType.TEXT_BOX,
            text = "test",
            position = ObjectPosition(0.1f, 0.2f, 0.5f, 0.6f)
        )
        val html = renderer.render(makeSlide(objects = listOf(obj)), palette, typography)
        // 0.1f → "10.00%", etc.
        assertTrue(html.contains("left: 10.00%"))
        assertTrue(html.contains("top: 20.00%"))
        assertTrue(html.contains("width: 50.00%"))
        assertTrue(html.contains("height: 60.00%"))
    }

    @Test
    fun `render bullet list as ul with li elements`() {
        val obj = SlideObject(
            id = "obj-1",
            type = ObjectType.BULLET_LIST,
            bullets = listOf("Uno", "Dos", "Tres")
        )
        val html = renderer.render(makeSlide(objects = listOf(obj)), palette, typography)
        assertTrue(html.contains("<ul>"))
        assertTrue(html.contains("<li>Uno</li>"))
        assertTrue(html.contains("<li>Dos</li>"))
        assertTrue(html.contains("<li>Tres</li>"))
        assertTrue(html.contains("</ul>"))
    }

    @Test
    fun `render escapes HTML special characters in text`() {
        val obj = SlideObject(
            id = "obj-1",
            type = ObjectType.TEXT_BOX,
            text = "Tom & Jerry <story> by \"me\""
        )
        val html = renderer.render(makeSlide(objects = listOf(obj)), palette, typography)
        // & must be escaped to &amp; to avoid being parsed as an entity
        assertTrue("Ampersand must be escaped", html.contains("Tom &amp; Jerry"))
        assertTrue("Angle brackets must be escaped", html.contains("&lt;story&gt;"))
    }

    @Test
    fun `render escapes ampersand only once`() {
        val obj = SlideObject(id = "x", type = ObjectType.TEXT_BOX, text = "a & b")
        val html = renderer.render(makeSlide(objects = listOf(obj)), palette, typography)
        // Must NOT be double-escaped to &amp;amp;
        assertFalse(html.contains("&amp;amp;"))
    }

    @Test
    fun `render converts newlines to br in text`() {
        val obj = SlideObject(
            id = "obj-1",
            type = ObjectType.TEXT_BOX,
            text = "Line 1\nLine 2"
        )
        val html = renderer.render(makeSlide(objects = listOf(obj)), palette, typography)
        assertTrue("Newlines must become <br>", html.contains("Line 1<br>Line 2"))
    }

    @Test
    fun `non-interactive mode omits selection JS script`() {
        val html = renderer.render(makeSlide(), palette, typography, interactive = false)
        assertFalse(html.contains("AndroidBridge"))
        assertFalse(html.contains("onObjectTapped"))
    }

    @Test
    fun `interactive mode injects AndroidBridge JS`() {
        val html = renderer.render(makeSlide(), palette, typography, interactive = true)
        assertTrue("Interactive HTML must include AndroidBridge reference", html.contains("AndroidBridge"))
        assertTrue("Interactive HTML must include onObjectTapped function", html.contains("onObjectTapped"))
        assertTrue(html.contains("__setSelection"))
    }

    @Test
    fun `interactive mode adds selected class outline CSS`() {
        val html = renderer.render(makeSlide(), palette, typography, interactive = true)
        assertTrue(html.contains(".obj.selected"))
    }

    @Test
    fun `background override takes precedence over palette`() {
        val customBg = "#FF0000"
        val html = renderer.render(makeSlide(backgroundHex = customBg), palette, typography)
        assertTrue("Custom background must be applied", html.contains("background: $customBg"))
        assertFalse(html.contains("background: ${palette.background}"))
    }

    @Test
    fun `text align css values are correct`() {
        val alignments = mapOf(
            TextAlign.LEFT to "left",
            TextAlign.CENTER to "center",
            TextAlign.RIGHT to "right",
            TextAlign.JUSTIFIED to "justify"
        )
        alignments.forEach { (align, cssValue) ->
            val obj = SlideObject(
                id = "obj-${align.name}",
                type = ObjectType.TEXT_BOX,
                text = "test",
                position = ObjectPosition(0f, 0f, 1f, 0.1f),
                style = ObjectStyle.default().copy(textAlign = align)
            )
            val html = renderer.render(makeSlide(objects = listOf(obj)), palette, typography)
            assertTrue("textAlign $align must produce $cssValue", html.contains("text-align: $cssValue"))
        }
    }

    @Test
    fun `image object renders img tag with src and alt`() {
        val obj = SlideObject(
            id = "img-1",
            type = ObjectType.IMAGE,
            imageUrl = "file:///path/to/img.png",
            imageDescription = "Una planta"
        )
        val html = renderer.render(makeSlide(objects = listOf(obj)), palette, typography)
        assertTrue(html.contains("<img"))
        assertTrue(html.contains("src=\"file:///path/to/img.png\""))
        assertTrue(html.contains("Una planta"))
    }

    @Test
    fun `image object with null imageUrl renders empty`() {
        val obj = SlideObject(
            id = "img-1",
            type = ObjectType.IMAGE,
            imageUrl = null
        )
        val html = renderer.render(makeSlide(objects = listOf(obj)), palette, typography)
        // Should not crash, should still render the obj div
        assertTrue(html.contains("data-obj-id=\"img-1\""))
        assertFalse(html.contains("<img"))
    }

    @Test
    fun `rotation produces CSS transform`() {
        val obj = SlideObject(
            id = "obj-1",
            type = ObjectType.TEXT_BOX,
            text = "rotated",
            position = ObjectPosition(0f, 0f, 1f, 1f, rotation = 45f)
        )
        val html = renderer.render(makeSlide(objects = listOf(obj)), palette, typography)
        assertTrue(html.contains("transform: rotate(45deg)"))
    }

    @Test
    fun `zero rotation does not add transform`() {
        val obj = SlideObject(
            id = "obj-1",
            type = ObjectType.TEXT_BOX,
            text = "not rotated",
            position = ObjectPosition(0f, 0f, 1f, 1f, rotation = 0f)
        )
        val html = renderer.render(makeSlide(objects = listOf(obj)), palette, typography)
        assertFalse(html.contains("transform: rotate"))
    }

    @Test
    fun `multiple objects all rendered`() {
        val objs = (1..5).map {
            SlideObject(
                id = "obj-$it",
                type = ObjectType.TEXT_BOX,
                text = "Object $it",
                position = ObjectPosition(0f, it * 0.1f, 1f, 0.1f)
            )
        }
        val html = renderer.render(makeSlide(objects = objs), palette, typography)
        objs.forEach { obj ->
            assertTrue("Object ${obj.id} must be in HTML", html.contains(obj.id))
            assertTrue(html.contains(obj.text))
        }
    }
}
