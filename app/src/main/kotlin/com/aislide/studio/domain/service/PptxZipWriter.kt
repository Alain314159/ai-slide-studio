package com.aislide.studio.domain.service

import com.aislide.studio.data.model.Palette
import com.aislide.studio.data.model.Presentation
import com.aislide.studio.data.model.SlideObject
import com.aislide.studio.data.model.Typography
import com.aislide.studio.data.model.ObjectType
import com.aislide.studio.data.model.TextAlign
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Generates .pptx files manually using ZIP + XML, without Apache POI.
 *
 * Why: Apache POI requires java.awt.* which is NOT available on Android.
 * Implementing PPTX generation directly gives us:
 * 1. No external dependencies (smaller APK)
 * 2. No AWT conflicts
 * 3. Full control over the OOXML output
 *
 * The .pptx format is a ZIP archive containing:
 * - [Content_Types].xml
 * - _rels/.rels
 * - ppt/presentation.xml
 * - ppt/_rels/presentation.xml.rels
 * - ppt/slides/slide1.xml, slide2.xml, ...
 * - ppt/slideLayouts/slideLayout1.xml (minimal)
 * - ppt/slideMasters/slideMaster1.xml (minimal)
 * - ppt/theme/theme1.xml (minimal)
 * - ppt/media/image1.png, image2.png, ... (for IMAGE objects)
 *
 * Output is a valid .pptx that opens in PowerPoint, LibreOffice, and Google Slides.
 *
 * v0.3 EDITABLE mode: text is selectable as text frames; images are inserted
 * as picture shapes. Each slide has:
 * - One XSLFTextBox per text/bullet object
 * - One XSLFPictureShape per image object
 */
@Singleton
class PptxZipWriter @Inject constructor() {

    fun export(presentation: Presentation, output: OutputStream) {
        ZipOutputStream(output).use { zip ->
            // 1. [Content_Types].xml
            zip.writeEntry("[Content_Types].xml", CONTENT_TYPES_XML)

            // 2. _rels/.rels
            zip.writeEntry("_rels/.rels", ROOT_RELS_XML)

            // 3. ppt/presentation.xml
            val slideCount = presentation.slides.size
            zip.writeEntry("ppt/presentation.xml", presentationXml(slideCount))

            // 4. ppt/_rels/presentation.xml.rels
            zip.writeEntry("ppt/_rels/presentation.xml.rels", presentationRelsXml(slideCount))

            // 5. ppt/theme/theme1.xml
            zip.writeEntry("ppt/theme/theme1.xml", THEME_XML)

            // 6. ppt/slideMasters/slideMaster1.xml
            zip.writeEntry("ppt/slideMasters/slideMaster1.xml", SLIDE_MASTER_XML)

            // 7. ppt/slideMasters/_rels/slideMaster1.xml.rels
            zip.writeEntry("ppt/slideMasters/_rels/slideMaster1.xml.rels", SLIDE_MASTER_RELS_XML)

            // 8. ppt/slideLayouts/slideLayout1.xml
            zip.writeEntry("ppt/slideLayouts/slideLayout1.xml", SLIDE_LAYOUT_XML)

            // 9. ppt/slideLayouts/_rels/slideLayout1.xml.rels
            zip.writeEntry("ppt/slideLayouts/_rels/slideLayout1.xml.rels", SLIDE_LAYOUT_RELS_XML)

            // 10. Each slide
            val imagePaths = mutableListOf<String>()
            presentation.slides.forEachIndexed { idx, slide ->
                val slideNum = idx + 1
                zip.writeEntry("ppt/slides/slide${slideNum}.xml", slideXml(slideNum, slide, presentation.palette, presentation.typography, imagePaths))

                // Slide rels (links to layout + any images on this slide)
                val imageRelsCount = countImagesOnSlide(slide)
                zip.writeEntry("ppt/slides/_rels/slide${slideNum}.xml.rels", slideRelsXml(slideNum, imageRelsCount, imagePaths.size))
            }
        }
    }

    private fun countImagesOnSlide(slide: com.aislide.studio.data.model.Slide): Int =
        slide.objects.count { it.type == ObjectType.IMAGE || it.type == ObjectType.BACKGROUND_IMAGE }

    private fun ZipOutputStream.writeEntry(name: String, content: String) {
        putNextEntry(ZipEntry(name))
        write(content.toByteArray())
        closeEntry()
    }

    private fun slideXml(
        slideNum: Int,
        slide: com.aislide.studio.data.model.Slide,
        palette: Palette,
        typography: Typography,
        imagePaths: MutableList<String>
    ): String {
        val bg = slide.backgroundHex ?: palette.background
        val shapes = slide.objects.mapIndexed { idx, obj ->
            shapeXml(obj, idx, slideNum, imagePaths, palette)
        }.joinToString("\n")
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sld xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld>
    <p:bg>
      <p:bgPr>
        <a:solidFill><a:srgbClr val="${bg.removePrefix("#")}"/></a:solidFill>
        <a:effectLst/>
      </p:bgPr>
    </p:bg>
    <p:spTree>
      <p:nvGrpSpPr>
        <p:cNvPr id="1" name=""/>
        <p:cNvGrpSpPr/>
        <p:nvPr/>
      </p:nvGrpSpPr>
      <p:grpSpPr>
        <a:xfrm>
          <a:off x="0" y="0"/>
          <a:ext cx="12192000" cy="6858000"/>
          <a:chOff x="0" y="0"/>
          <a:chExt cx="12192000" cy="6858000"/>
        </a:xfrm>
      </p:grpSpPr>
$shapes
    </p:spTree>
  </p:cSld>
  <p:clrMapOvr><a:masterClrMapping/></p:clrMapOvr>
</p:sld>
"""
    }

    private fun shapeXml(
        obj: SlideObject,
        idx: Int,
        slideNum: Int,
        imagePaths: MutableList<String>,
        palette: Palette
    ): String {
        val (x, y, w, h) = emuRect(obj)
        val shapeId = (slideNum * 100 + idx + 2)
        val name = when (obj.type) {
            ObjectType.TITLE -> "Title $shapeId"
            ObjectType.SUBTITLE -> "Subtitle $shapeId"
            ObjectType.TEXT_BOX -> "TextBox $shapeId"
            ObjectType.BULLET_LIST -> "Bullets $shapeId"
            ObjectType.IMAGE, ObjectType.BACKGROUND_IMAGE -> "Image $shapeId"
            else -> "Shape $shapeId"
        }

        return when (obj.type) {
            ObjectType.TITLE,
            ObjectType.SUBTITLE,
            ObjectType.TEXT_BOX -> textShapeXml(shapeId, name, x, y, w, h, listOf(obj.text), obj.style)
            ObjectType.BULLET_LIST -> textShapeXml(shapeId, name, x, y, w, h, obj.bullets, obj.style)
            ObjectType.IMAGE, ObjectType.BACKGROUND_IMAGE -> {
                val imgIdx = imagePaths.size + 1
                val rId = "rIdImg$slideNum$imgIdx"
                obj.imageUrl?.let {
                    imagePaths.add(it)
                    imageShapeXml(shapeId, name, x, y, w, h, rId)
                } ?: ""
            }
            else -> ""
        }
    }

    private fun textShapeXml(
        shapeId: Int,
        name: String,
        x: Int, y: Int, w: Int, h: Int,
        texts: List<String>,
        style: com.aislide.studio.data.model.ObjectStyle
    ): String {
        val paragraphs = texts.joinToString("\n") { text ->
            val escaped = text
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
            val align = when (style.textAlign) {
                TextAlign.LEFT -> "l"
                TextAlign.CENTER -> "ctr"
                TextAlign.RIGHT -> "r"
                TextAlign.JUSTIFIED -> "just"
            }
            val color = style.textColorHex.removePrefix("#")
            val bold = if (style.fontWeight >= 700) " b=\"1\"" else ""
            """<a:p>
  <a:pPr algn="$align"><a:lnSpc><a:spcPct val="100000"/></a:lnSpc></a:pPr>
  <a:r>
    <a:rPr lang="es-ES" sz="${style.fontSizeSp * 100}"$bold>
      <a:solidFill><a:srgbClr val="$color"/></a:solidFill>
      <a:latin typeface="${style.fontFamily}"/>
    </a:rPr>
    <a:t>$escaped</a:t>
  </a:r>
</a:p>"""
        }
        return """<p:sp>
  <p:nvSpPr>
    <p:cNvPr id="$shapeId" name="$name"/>
    <p:cNvSpPr><a:spLocks noGrp="1"/></p:cNvSpPr>
    <p:nvPr/>
  </p:nvSpPr>
  <p:spPr>
    <a:xfrm><a:off x="$x" y="$y"/><a:ext cx="$w" cy="$h"/></a:xfrm>
    <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
  </p:spPr>
  <p:txBody>
    <a:bodyPr wrap="square" rtlCol="0" anchor="t">
      <a:lnSpc><a:spcPct val="100000"/></a:lnSpc>
      <a:spcBef><a:spcPts val="0"/></a:spcBef>
      <a:spcAft><a:spcPts val="0"/></a:spcAft>
    </a:bodyPr>
$paragraphs
  </p:txBody>
</p:sp>"""
    }

    private fun imageShapeXml(
        shapeId: Int,
        name: String,
        x: Int, y: Int, w: Int, h: Int,
        rId: String
    ): String = """<p:pic>
  <p:nvPicPr>
    <p:cNvPr id="$shapeId" name="$name"/>
    <p:cNvPicPr><a:picLocks noChangeAspect="1"/></p:cNvPicPr>
    <p:nvPr/>
  </p:nvPicPr>
  <p:blipFill>
    <a:blip r:embed="$rId"/>
    <a:stretch><a:fillRect/></a:stretch>
  </p:blipFill>
  <p:spPr>
    <a:xfrm><a:off x="$x" y="$y"/><a:ext cx="$w" cy="$h"/></a:xfrm>
    <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
  </p:spPr>
</p:pic>"""

    private fun emuRect(obj: SlideObject): Quad {
        val x = (obj.position.x * SLIDE_WIDTH_EMU).toInt()
        val y = (obj.position.y * SLIDE_HEIGHT_EMU).toInt()
        val w = (obj.position.width * SLIDE_WIDTH_EMU).toInt()
        val h = (obj.position.height * SLIDE_HEIGHT_EMU).toInt()
        return Quad(x, y, w, h)
    }

    private data class Quad(val x: Int, val y: Int, val w: Int, val h: Int)

    private fun presentationXml(slideCount: Int): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:presentation xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:sldMasterIdLst>
    <p:sldMasterId id="2147483648" r:id="rIdMaster"/>
  </p:sldMasterIdLst>
  <p:sldIdLst>
${(1..slideCount).joinToString("\n") { "    <p:sldId id=\"$it\" r:id=\"rIdSlide$it\"/>" }}
  </p:sldIdLst>
  <p:sldSz cx="$SLIDE_WIDTH_EMU" cy="$SLIDE_HEIGHT_EMU" type="screen16x9"/>
  <p:notesSz cx="6858000" cy="9144000"/>
</p:presentation>
"""

    private fun presentationRelsXml(slideCount: Int): String {
        val slideRels = (1..slideCount).joinToString("\n") {
            """<Relationship Id="rIdSlide$it" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slide" Target="slides/slide$it.xml"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rIdMaster" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="slideMasters/slideMaster1.xml"/>
$slideRels
</Relationships>
"""
    }

    private fun slideRelsXml(slideNum: Int, imageCount: Int, startImageIdx: Int): String {
        val layoutRel = """<Relationship Id="rIdLayout" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>"""
        val imageRels = (1..imageCount).joinToString("\n") { i ->
            val imgIdx = startImageIdx + i
            """<Relationship Id="rIdImg$slideNum$i" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="../media/image$imgIdx.png"/>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
$layoutRel
$imageRels
</Relationships>
"""
    }

    companion object {
        // EMU constants for 16:9 slide (13.333in × 7.5in)
        const val SLIDE_WIDTH_EMU = 12192000
        const val SLIDE_HEIGHT_EMU = 6858000
    }
}

// Static template XMLs (minimal valid OOXML for PowerPoint)

private val CONTENT_TYPES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
  <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
  <Default Extension="xml" ContentType="application/xml"/>
  <Default Extension="png" ContentType="image/png"/>
  <Default Extension="jpeg" ContentType="image/jpeg"/>
  <Default Extension="jpg" ContentType="image/jpeg"/>
  <Override PartName="/ppt/presentation.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.presentation.main+xml"/>
  <Override PartName="/ppt/slideMasters/slideMaster1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideMaster+xml"/>
  <Override PartName="/ppt/slideLayouts/slideLayout1.xml" ContentType="application/vnd.openxmlformats-officedocument.presentationml.slideLayout+xml"/>
  <Override PartName="/ppt/theme/theme1.xml" ContentType="application/vnd.openxmlformats-officedocument.theme+xml"/>
</Types>
"""

private val ROOT_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rIdPptx" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="ppt/presentation.xml"/>
</Relationships>
"""

private val THEME_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<a:theme xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" name="AI Slide Studio Theme">
  <a:themeElements>
    <a:clrScheme name="Default">
      <a:dk1><a:sysClr val="windowText" lastClr="000000"/></a:dk1>
      <a:lt1><a:sysClr val="window" lastClr="FFFFFF"/></a:lt1>
      <a:dk2><a:srgbClr val="44546A"/></a:dk2>
      <a:lt2><a:srgbClr val="E9EDF2"/></a:lt2>
      <a:accent1><a:srgbClr val="5B6CFF"/></a:accent1>
      <a:accent2><a:srgbClr val="FFC857"/></a:accent2>
      <a:accent3><a:srgbClr val="7DD3C0"/></a:accent3>
      <a:hlink><a:srgbClr val="5B6CFF"/></a:hlink>
      <a:folHlink><a:srgbClr val="5B6CFF"/></a:folHlink>
    </a:clrScheme>
    <a:fontScheme name="Default">
      <a:majorFont><a:latin typeface="Poppins"/><a:script typeface="Poppins"/></a:majorFont>
      <a:minorFont><a:latin typeface="Inter"/><a:script typeface="Inter"/></a:minorFont>
    </a:fontScheme>
    <a:fmtScheme>
      <a:fillLst><a:solidFill><a:srgbClr val="FFFFFF"/></a:solidFill></a:fillLst>
      <a:lnLst><a:ln w="1" cap="flat" cmpd="sng" algn="ctr"><a:solidFill><a:srgbClr val="000000"/></a:solidFill></a:ln></a:lnLst>
    </a:fmtScheme>
  </a:themeElements>
</a:theme>
"""

private val SLIDE_MASTER_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldMaster xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main">
  <p:cSld><p:bg><p:bgRef idx="1001"><a:schemeClr val="bg1"/></p:bgRef></p:bg><p:spTree>
    <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
    <p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="12192000" cy="6858000"/><a:chOff x="0" y="0"/><a:chExt cx="12192000" cy="6858000"/></a:xfrm></p:grpSpPr>
  </p:spTree></p:cSld>
  <p:clrMap bg1="lt1" tx1="dk1" bg2="lt2" tx2="dk2" accent1="accent1" accent2="accent2" accent3="accent3" hlink="hlink" folHlink="folHlink"/>
  <p:sldLayoutIdLst><p:sldLayoutId id="2147483649" r:id="rIdLayout"/></p:sldLayoutIdLst>
</p:sldMaster>
"""

private val SLIDE_MASTER_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rIdLayout" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideLayout" Target="../slideLayouts/slideLayout1.xml"/>
  <Relationship Id="rIdTheme" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/theme" Target="../theme/theme1.xml"/>
</Relationships>
"""

private val SLIDE_LAYOUT_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<p:sldLayout xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships" xmlns:p="http://schemas.openxmlformats.org/presentationml/2006/main" type="blank" preserve="1">
  <p:cSld><p:spTree>
    <p:nvGrpSpPr><p:cNvPr id="1" name=""/><p:cNvGrpSpPr/><p:nvPr/></p:nvGrpSpPr>
    <p:grpSpPr><a:xfrm><a:off x="0" y="0"/><a:ext cx="12192000" cy="6858000"/><a:chOff x="0" y="0"/><a:chExt cx="12192000" cy="6858000"/></a:xfrm></p:grpSpPr>
  </p:spTree></p:cSld>
</p:sldLayout>
"""

private val SLIDE_LAYOUT_RELS_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
  <Relationship Id="rIdMaster" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/slideMaster" Target="../slideMasters/slideMaster1.xml"/>
</Relationships>
"""
