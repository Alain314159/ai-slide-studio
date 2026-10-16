package com.aislide.studio.domain.service

import com.aislide.studio.data.model.Palette
import com.aislide.studio.data.model.Presentation
import com.aislide.studio.data.model.SlideObject
import com.aislide.studio.data.model.Typography
import org.apache.poi.xslf.usermodel.SlideLayout
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFSlide
import org.apache.poi.xslf.usermodel.XSLFTextBox
import org.apache.poi.xslf.usermodel.XSLFTextParagraph
import org.apache.poi.xslf.usermodel.XSLFTextRun
import org.apache.poi.xslf.usermodel.XSLFPictureShape
import org.apache.poi.xslf.usermodel.XSLFAutoShape
import org.apache.poi.sl.usermodel.PictureData
import org.apache.poi.sl.usermodel.Vertical
import org.apache.poi.sl.usermodel.TextAlign
import org.apache.poi.sl.usermodel.PlaceholderType
import org.jsoup.Jsoup
import java.awt.Color
import java.awt.Rectangle
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exports a [Presentation] to .pptx using Apache POI with absolute EMU positioning.
 *
 * This is the EDITABLE mode — text remains selectable/editable in PowerPoint.
 * The mapeo is HTML/CSS → PPTX objects positioned at exact EMU coordinates:
 *
 *   slide.html (positioned absolute divs in % units)
 *        ↓
 *   XSLFTextBox / XSLFPictureShape / XSLFAutoShape
 *   positioned with new Rectangle(x, y, width, height) in EMU
 *
 * PowerPoint slide is 12,192,000 x 6,858,000 EMU (16:9, 13.333in × 7.5in).
 * Each [SlideObject] has normalized 0..1 coordinates, so:
 *   x_emu = (object.position.x * SLIDE_WIDTH_EMU).toInt()
 *
 * Fuentes embebidas: TODO in v0.2 (needs Apache POI embedFont + font file).
 */
@Singleton
class PptxExportEngine @Inject constructor() {

    fun export(presentation: Presentation, output: OutputStream) {
        XMLSlideShow().use { ppt ->
            // Set 16:9 widescreen dimensions
            val pgSize = ppt.slideSize
            pgSize.setSize(SLIDE_WIDTH_EMU, SLIDE_HEIGHT_EMU)

            presentation.slides.forEach { slide ->
                val blankLayout = ppt.createSlideMaster().getLayout(SlideLayout.BLANK)
                val xslfSlide = ppt.createSlide(blankLayout)
                xslfSlide.background.fillForegroundColor = parseColor(
                    slide.backgroundHex ?: presentation.palette.background
                )

                slide.objects.forEach { obj ->
                    placeObject(xslfSlide, obj, presentation.palette, presentation.typography)
                }
            }

            ppt.write(output)
        }
    }

    /**
     * Convenience: returns .pptx bytes.
     */
    fun exportToBytes(presentation: Presentation): ByteArray {
        return ByteArrayOutputStream().use { out ->
            export(presentation, out)
            out.toByteArray()
        }
    }

    // === Object placement ===

    private fun placeObject(
        slide: XSLFSlide,
        obj: SlideObject,
        palette: Palette,
        typography: Typography
    ) {
        val rect = emuRect(obj)

        when (obj.type) {
            com.aislide.studio.data.model.ObjectType.TITLE,
            com.aislide.studio.data.model.ObjectType.SUBTITLE,
            com.aislide.studio.data.model.ObjectType.TEXT_BOX -> {
                val tb = slide.createTextBox()
                tb.anchor = rect
                val para = tb.addNewTextParagraph()
                val run = para.addNewTextRun()
                run.text = obj.text
                applyTextStyle(run, obj.style, typography.headingFont)
                applyAlignment(para, obj.style.textAlign)
            }

            com.aislide.studio.data.model.ObjectType.BULLET_LIST -> {
                val tb = slide.createTextBox()
                tb.anchor = rect
                obj.bullets.forEach { bullet ->
                    val para = tb.addNewTextParagraph()
                    para.bulletFont = "Arial"
                    para.fontAlign = org.apache.poi.sl.usermodel.FontAlign.AUTO
                    val run = para.addNewTextRun()
                    run.text = bullet
                    applyTextStyle(run, obj.style, typography.bodyFont)
                    applyAlignment(para, obj.style.textAlign)
                }
            }

            com.aislide.studio.data.model.ObjectType.IMAGE,
            com.aislide.studio.data.model.ObjectType.BACKGROUND_IMAGE -> {
                obj.imageUrl?.let { urlStr ->
                    runCatching {
                        val bytes = fetchImageBytes(urlStr)
                        val picType = inferPictureType(urlStr)
                        val picData = slide.slideShow.addPicture(bytes, picType)
                        val pic = slide.createPicture(picData)
                        pic.anchor = rect
                    }
                }
            }

            com.aislide.studio.data.model.ObjectType.SHAPE_RECTANGLE,
            com.aislide.studio.data.model.ObjectType.SHAPE_CIRCLE,
            com.aislide.studio.data.model.ObjectType.SHAPE_ARROW,
            com.aislide.studio.data.model.ObjectType.SHAPE_LINE -> {
                val shapeType = when (obj.type) {
                    com.aislide.studio.data.model.ObjectType.SHAPE_CIRCLE ->
                        org.apache.poi.sl.usermodel.ShapeType.ELLIPSE
                    com.aislide.studio.data.model.ObjectType.SHAPE_ARROW ->
                        org.apache.poi.sl.usermodel.ShapeType.LEFT_ARROW
                    com.aislide.studio.data.model.ObjectType.SHAPE_LINE ->
                        org.apache.poi.sl.usermodel.ShapeType.LINE
                    else -> org.apache.poi.sl.usermodel.ShapeType.RECT
                }
                val shape = slide.createAutoShape(shapeType)
                shape.anchor = rect
                obj.style.backgroundColorHex?.let { shape.fillColor = parseColor(it) }
            }

            else -> { /* table/chart/icon — TODO in later phase */ }
        }
    }

    // === EMU math ===

    private fun emuRect(obj: SlideObject): Rectangle {
        val x = (obj.position.x * SLIDE_WIDTH_EMU).toInt()
        val y = (obj.position.y * SLIDE_HEIGHT_EMU).toInt()
        val w = (obj.position.width * SLIDE_WIDTH_EMU).toInt()
        val h = (obj.position.height * SLIDE_HEIGHT_EMU).toInt()
        return Rectangle(x, y, w, h)
    }

    // === Style helpers ===

    private fun applyTextStyle(
        run: XSLFTextRun,
        style: com.aislide.studio.data.model.ObjectStyle,
        defaultFont: String
    ) {
        run.fontFamily = style.fontFamily.ifBlank { defaultFont }
        run.fontSize = style.fontSizeSp.toDouble()
        run.bold = style.fontWeight >= 700
        run.fontColor = parseColor(style.textColorHex)
    }

    private fun applyAlignment(para: XSLFTextParagraph, align: com.aislide.studio.data.model.TextAlign) {
        para.textAlign = when (align) {
            com.aislide.studio.data.model.TextAlign.LEFT -> TextAlign.LEFT
            com.aislide.studio.data.model.TextAlign.CENTER -> TextAlign.CENTER
            com.aislide.studio.data.model.TextAlign.RIGHT -> TextAlign.RIGHT
            com.aislide.studio.data.model.TextAlign.JUSTIFIED -> TextAlign.JUSTIFY
        }
    }

    private fun parseColor(hex: String): Color {
        val clean = hex.removePrefix("#")
        return runCatching {
            val rgb = clean.toLong(16)
            Color(rgb shr 16 and 0xFF, rgb shr 8 and 0xFF, rgb and 0xFF)
        }.getOrDefault(Color.BLACK)
    }

    private fun fetchImageBytes(urlStr: String): ByteArray {
        return if (urlStr.startsWith("http")) {
            URL(urlStr).openStream().use { it.readBytes() }
        } else {
            java.io.File(urlStr).readBytes()
        }
    }

    private fun inferPictureType(urlStr: String): PictureData.PictureType {
        val lower = urlStr.lowercase()
        return when {
            lower.endsWith(".png") -> PictureData.PictureType.PNG
            lower.endsWith(".jpg") || lower.endsWith(".jpeg") -> PictureData.PictureType.JPEG
            lower.endsWith(".gif") -> PictureData.PictureType.GIF
            lower.endsWith(".bmp") -> PictureData.PictureType.BMP
            lower.endsWith(".svg") -> PictureData.PictureType.SVG
            else -> PictureData.PictureType.JPEG
        }
    }

    companion object {
        // EMU constants for 16:9 slide (13.333in × 7.5in)
        // 1 inch = 914400 EMU
        const val SLIDE_WIDTH_EMU = 12192000    // 13.333"
        const val SLIDE_HEIGHT_EMU = 6858000    // 7.5"
        const val SLIDE_WIDTH_PX = 1920         // for reference
        const val SLIDE_HEIGHT_PX = 1080        // for reference
    }
}
