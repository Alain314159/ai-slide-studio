package com.aislide.studio.domain.service

import com.aislide.studio.data.model.Palette
import com.aislide.studio.data.model.Slide
import com.aislide.studio.data.model.SlideObject
import com.aislide.studio.data.model.Typography
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Renders a [Slide] to a complete, self-contained HTML/CSS document.
 *
 * The HTML is rendered by an Android WebView in the editor screen.
 * The same HTML will be parsed by [PptxExportEngine] to produce the .pptx.
 *
 * Why HTML/CSS instead of native Android Canvas:
 * - The LLM knows HTML/CSS extremely well; it generates it natively.
 * - HTML/CSS gives infinite visual freedom (gradients, shadows, animations, fonts).
 * - WebView is fast and standard in Android.
 * - The same source becomes both the editor preview AND the export source.
 *
 * Output: a self-contained HTML document with inline CSS, no external deps.
 */
@Singleton
class HtmlSlideRenderer @Inject constructor() {

    /**
     * Renders a slide to a full HTML document sized for a 16:9 slide.
     */
    fun render(slide: Slide, palette: Palette, typography: Typography): String {
        val titleStyle = "font-family: '${typography.headingFont}', sans-serif; font-weight: ${typography.headingWeight};"
        val bodyStyle = "font-family: '${typography.bodyFont}', sans-serif; font-weight: ${typography.bodyWeight};"
        val slideBg = slide.backgroundHex ?: palette.background
        val objects = slide.objects.joinToString("\n") { renderObject(it, palette, typography) }
        val titleBlock = if (slide.title.isNotBlank() && slide.objects.none { it.text == slide.title }) {
            ""
        } else ""

        return """<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Slide ${slide.index + 1}</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; }
    html, body { width: 100%; height: 100%; overflow: hidden; }
    body {
      background: $slideBg;
      color: ${palette.onBackground};
      $bodyStyle
      font-size: 18px;
    }
    .slide {
      position: relative;
      width: 100%;
      height: 100%;
      overflow: hidden;
    }
    .obj {
      position: absolute;
      display: flex;
      flex-direction: column;
      justify-content: flex-start;
      align-items: flex-start;
      padding: 8px 12px;
      word-break: break-word;
      overflow: hidden;
    }
    .obj.title $titleStyle
    .obj.text, .obj.subtitle $bodyStyle
    .obj.bullets ul { list-style: none; padding-left: 0; width: 100%; }
    .obj.bullets li {
      padding: 6px 0 6px 24px;
      position: relative;
      $bodyStyle
    }
    .obj.bullets li::before {
      content: "";
      position: absolute;
      left: 4px;
      top: 50%;
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background: ${palette.accent};
      transform: translateY(-50%);
    }
    .obj.image img {
      width: 100%;
      height: 100%;
      object-fit: cover;
      border-radius: 4px;
    }
  </style>
</head>
<body>
  <div class="slide" data-slide-id="${slide.id}" data-slide-index="${slide.index}" data-layout="${slide.layout}">
$objects
$titleBlock
  </div>
</body>
</html>"""
    }

    private fun renderObject(obj: SlideObject, palette: Palette, typography: Typography): String {
        val left = "%.2f%%".format(obj.position.x * 100)
        val top = "%.2f%%".format(obj.position.y * 100)
        val width = "%.2f%%".format(obj.position.width * 100)
        val height = "%.2f%%".format(obj.position.height * 100)
        val rotation = if (obj.position.rotation != 0f) "transform: rotate(${obj.position.rotation}deg);" else ""

        val style = buildString {
            append("left: $left; top: $top; width: $width; height: $height;")
            append(rotation)
            obj.style.backgroundColorHex?.let { append(" background: $it;") }
            append(" color: ${obj.style.textColorHex};")
            append(" font-size: ${obj.style.fontSizeSp}px;")
            append(" font-weight: ${obj.style.fontWeight};")
            append(" opacity: ${obj.style.opacity};")
            if (obj.style.borderRadiusDp > 0) append(" border-radius: ${obj.style.borderRadiusDp}px;")
            if (obj.style.hasShadow) append(" box-shadow: 0 4px 12px rgba(0,0,0,0.25);")
            append(" text-align: ${obj.style.textAlign.cssValue};")
            append(" z-index: ${obj.style.zIndex};")
        }

        val class_ = when (obj.type) {
            com.aislide.studio.data.model.ObjectType.TITLE -> "obj title"
            com.aislide.studio.data.model.ObjectType.SUBTITLE -> "obj subtitle"
            com.aislide.studio.data.model.ObjectType.TEXT_BOX -> "obj text"
            com.aislide.studio.data.model.ObjectType.BULLET_LIST -> "obj bullets"
            com.aislide.studio.data.model.ObjectType.IMAGE, com.aislide.studio.data.model.ObjectType.BACKGROUND_IMAGE -> "obj image"
            else -> "obj shape"
        }

        val content = when (obj.type) {
            com.aislide.studio.data.model.ObjectType.BULLET_LIST ->
                "<ul>${obj.bullets.joinToString("") { "<li>${escape(it)}</li>" }}</ul>"
            com.aislide.studio.data.model.ObjectType.IMAGE, com.aislide.studio.data.model.ObjectType.BACKGROUND_IMAGE ->
                obj.imageUrl?.let { "<img src=\"$it\" alt=\"${escape(obj.imageDescription ?: "")}\" />" } ?: ""
            com.aislide.studio.data.model.ObjectType.TITLE,
            com.aislide.studio.data.model.ObjectType.SUBTITLE,
            com.aislide.studio.data.model.ObjectType.TEXT_BOX -> escape(obj.text)
            else -> ""
        }

        return """    <div class="$class_" style="$style" data-obj-id="${obj.id}">$content</div>"""
    }

    private fun escape(text: String): String =
        text
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\n", "<br>")
}

private val com.aislide.studio.data.model.TextAlign.cssValue: String
    get() = when (this) {
        com.aislide.studio.data.model.TextAlign.LEFT -> "left"
        com.aislide.studio.data.model.TextAlign.CENTER -> "center"
        com.aislide.studio.data.model.TextAlign.RIGHT -> "right"
        com.aislide.studio.data.model.TextAlign.JUSTIFIED -> "justify"
    }
