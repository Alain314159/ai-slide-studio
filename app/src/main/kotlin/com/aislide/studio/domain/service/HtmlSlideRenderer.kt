package com.aislide.studio.domain.service

import android.webkit.JavascriptInterface
import com.aislide.studio.data.model.Palette
import com.aislide.studio.data.model.Slide
import com.aislide.studio.data.model.SlideObject
import com.aislide.studio.data.model.TextAlign
import com.aislide.studio.data.model.Typography
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Renders a [Slide] to a complete, self-contained HTML/CSS document.
 *
 * v0.2: now injects a small JS script that listens for taps on objects with
 * `data-obj-id` and calls back to the host via [ObjectSelectionBridge].
 *
 * The HTML is rendered by an Android WebView in the editor screen.
 * The same HTML will be parsed by [PptxExportEngine] to produce the .pptx.
 *
 * Output: a self-contained HTML document with inline CSS + JS, no external deps.
 */
@Singleton
class HtmlSlideRenderer @Inject constructor() {

    /**
     * Renders a slide to a full HTML document sized for a 16:9 slide.
     * @param interactive When true, injects JS that lets users tap-to-select
     *                    objects in the WebView. Default false for export.
     */
    fun render(
        slide: Slide,
        palette: Palette,
        typography: Typography,
        interactive: Boolean = false
    ): String {
        val slideBg = slide.backgroundHex ?: palette.background
        val objects = slide.objects.joinToString("\n") { renderObject(it, palette, typography) }

        val selectionScript = if (interactive) SELECTION_JS else ""

        return """<!DOCTYPE html>
<html lang="es">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>Slide ${slide.index + 1}</title>
  <style>
    * { box-sizing: border-box; margin: 0; padding: 0; -webkit-tap-highlight-color: transparent; }
    html, body { width: 100%; height: 100%; overflow: hidden; }
    body {
      background: $slideBg;
      color: ${palette.onBackground};
      font-family: '${typography.bodyFont}', sans-serif;
      font-weight: ${typography.bodyWeight};
      font-size: 18px;
      user-select: none;
    }
    .slide { position: relative; width: 100%; height: 100%; overflow: hidden; }
    .obj {
      position: absolute;
      display: flex;
      flex-direction: column;
      justify-content: flex-start;
      align-items: flex-start;
      padding: 8px 12px;
      word-break: break-word;
      overflow: hidden;
      cursor: pointer;
      transition: outline 0.15s ease, background 0.15s ease;
    }
    .obj.selected {
      outline: 3px solid ${palette.accent};
      outline-offset: -3px;
      background: ${palette.accent}1A;  /* 10% overlay */
    }
    .obj.title { font-family: '${typography.headingFont}', sans-serif; font-weight: ${typography.headingWeight}; }
    .obj.text, .obj.subtitle { font-family: '${typography.bodyFont}', sans-serif; font-weight: ${typography.bodyWeight}; }
    .obj.bullets ul { list-style: none; padding-left: 0; width: 100%; }
    .obj.bullets li {
      padding: 6px 0 6px 24px;
      position: relative;
      font-family: '${typography.bodyFont}', sans-serif;
      font-weight: ${typography.bodyWeight};
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
    .obj.image img { width: 100%; height: 100%; object-fit: cover; border-radius: 4px; }
  </style>
</head>
<body>
  <div class="slide" data-slide-id="${slide.id}" data-slide-index="${slide.index}" data-layout="${slide.layout}">
$objects
  </div>
$selectionScript
</body>
</html>"""
    }

    private fun renderObject(obj: SlideObject, palette: Palette, typography: Typography): String {
        val left = "%.2f%%".format(obj.position.x * 100)
        val top = "%.2f%%".format(obj.position.y * 100)
        val width = "%.2f%%".format(obj.position.width * 100)
        val height = "%.2f%%".format(obj.position.height * 100)
        val rotation = if (obj.position.rotation != 0f) {
            val rotStr = if (obj.position.rotation == obj.position.rotation.toInt().toFloat()) {
                obj.position.rotation.toInt().toString()
            } else {
                obj.position.rotation.toString()
            }
            "transform: rotate(${rotStr}deg);"
        } else ""

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

    /**
     * JS injected when `interactive=true`. Listens for taps on .obj elements
     * and calls back to the Android host via `AndroidBridge.onObjectTapped(id)`.
     */
    private val SELECTION_JS = """
<script>
(function() {
  function highlight(id) {
    document.querySelectorAll('.obj').forEach(function(el) { el.classList.remove('selected'); });
    if (id) {
      var el = document.querySelector('[data-obj-id="' + id + '"]');
      if (el) el.classList.add('selected');
    }
  }
  document.addEventListener('click', function(e) {
    var target = e.target;
    while (target && !target.classList.contains('obj')) {
      target = target.parentElement;
      if (!target || target === document.body) {
        // tapped on background — deselect
        if (window.AndroidBridge) AndroidBridge.onObjectTapped('');
        highlight('');
        e.preventDefault();
        return;
      }
    }
    if (target && target.hasAttribute('data-obj-id')) {
      var id = target.getAttribute('data-obj-id');
      highlight(id);
      if (window.AndroidBridge) AndroidBridge.onObjectTapped(id);
    }
    e.preventDefault();
  }, { passive: false });

  // Expose a function the host can call to set selection programmatically
  window.__setSelection = function(id) { highlight(id); };
})();
</script>
"""

    companion object {
        const val JS_BRIDGE_NAME = "AndroidBridge"
    }
}

/**
 * JavascriptInterface implementation. Injected into the WebView as
 * `AndroidBridge` so the JS in [SELECTION_JS] can call `onObjectTapped(id)`.
 *
 * IMPORTANT (security.md rule of android skill): only add a JavascriptInterface
 * when needed (interactive editor only), and ensure no sensitive methods are
 * exposed. This one just relays a string ID to a callback.
 */
class ObjectSelectionBridge(
    private val onTap: (String) -> Unit
) {
    @JavascriptInterface
    fun onObjectTapped(id: String) {
        onTap(id)
    }
}

private val com.aislide.studio.data.model.TextAlign.cssValue: String
    get() = when (this) {
        com.aislide.studio.data.model.TextAlign.LEFT -> "left"
        com.aislide.studio.data.model.TextAlign.CENTER -> "center"
        com.aislide.studio.data.model.TextAlign.RIGHT -> "right"
        com.aislide.studio.data.model.TextAlign.JUSTIFIED -> "justify"
    }
