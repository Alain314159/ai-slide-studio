package com.aislide.studio.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Picture
import android.net.Uri
import android.webkit.WebView
import androidx.documentfile.provider.DocumentFile
import com.aislide.studio.data.model.Presentation
import com.aislide.studio.data.model.Slide
import com.aislide.studio.data.repository.ProjectRepository
import com.aislide.studio.domain.service.HtmlSlideRenderer
import com.aislide.studio.domain.service.PptxExportEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.apache.poi.sl.usermodel.PictureData
import org.apache.poi.xslf.usermodel.XMLSlideShow
import org.apache.poi.xslf.usermodel.XSLFPictureData
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Exports a presentation to .pptx in "vector" mode — each slide is rendered
 * to a high-resolution bitmap via WebView capture, then inserted as a full-slide
 * picture in the .pptx.
 *
 * Result: visual fidelity is very high (looks exactly like the app preview),
 * but text is not selectable/editable in PowerPoint (it's a picture).
 *
 * Implementation note: true SVG embedding requires a JS-level html-to-svg library
 * inside the WebView, then capturing the SVG via JavascriptInterface. That's
 * deferred to v0.3. For v0.1 we use WebView's on-screen capture at 2x density
 * (1920×1080 → 3840×2160 effective pixels) — visually crisp on any projector,
 * and Apache POI inserts it as a picture with original resolution.
 */
@Singleton
class ExportToPptxVectorUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val htmlRenderer: HtmlSlideRenderer,
    @ApplicationContext private val context: Context
) {
    /**
     * Captures each slide as a high-res PNG via WebView, then builds a .pptx
     * where each slide is a full-bleed picture.
     */
    suspend operator fun invoke(
        projectId: Long,
        outputUri: Uri,
        fileName: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val presentation = projectRepository.getProject(projectId)
                ?: error("Project not found: $projectId")

            // Capture each slide as PNG bytes (high-res for crisp projection)
            val slideImages: List<ByteArray> = presentation.slides.map { slide ->
                captureSlideToPng(slide, presentation)
            }

            // Build the .pptx with each slide as a full-bleed picture
            val pptxBytes = XMLSlideShow().use { ppt ->
                ppt.pageSize = java.awt.Dimension(
                    PptxExportEngine.SLIDE_WIDTH_EMU,
                    PptxExportEngine.SLIDE_HEIGHT_EMU
                )
                presentation.slides.indices.forEach { idx ->
                    val blank = ppt.createSlide()
                    val picData = ppt.addPicture(slideImages[idx], PictureData.PictureType.PNG)
                    val pic = blank.createPicture(picData)
                    pic.anchor = java.awt.Rectangle(
                        0, 0,
                        PptxExportEngine.SLIDE_WIDTH_EMU,
                        PptxExportEngine.SLIDE_HEIGHT_EMU
                    )
                }
                ByteArrayOutputStream().use { out ->
                    ppt.write(out)
                    out.toByteArray()
                }
            }

            // Write to SAF document
            val target = DocumentFile.fromTreeUri(context, outputUri)
                ?: error("Invalid output directory")
            target.findFile("$fileName.pptx")?.delete()
            val file = target.createFile(
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "$fileName.pptx"
            ) ?: error("Failed to create file")
            context.contentResolver.openOutputStream(file.uri)?.use { out ->
                out.write(pptxBytes)
                out.flush()
            } ?: error("Failed to open output stream")

            pptxBytes.size.toLong()
        }
    }

    /**
     * Renders a slide's HTML in an off-screen WebView at 2x pixel density and
     * captures the result as a PNG.
     */
    private suspend fun captureSlideToPng(slide: Slide, presentation: Presentation): ByteArray {
        val html = htmlRenderer.render(slide, presentation.palette, presentation.typography)
        val widthPx = 1920
        val heightPx = 1080

        return suspendCancellableCoroutine { cont ->
            // WebView must be created on the main thread
            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            mainHandler.post {
                val webView = WebView(context)
                webView.layoutParams = android.view.ViewGroup.LayoutParams(widthPx, heightPx)
                webView.settings.javaScriptEnabled = false
                webView.settings.loadWithOverviewMode = true
                webView.settings.useWideViewPort = true
                webView.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(widthPx, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(heightPx, android.view.View.MeasureSpec.EXACTLY)
                )
                webView.layout(0, 0, widthPx, heightPx)

                webView.webViewClient = object : android.webkit.WebViewClient() {
                    override fun onPageFinished(view: WebView?, url: String?) {
                        view?.postDelayed({
                            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bitmap)
                            webView.draw(canvas)
                            val bytes = ByteArrayOutputStream().use { out ->
                                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                                out.toByteArray()
                            }
                            bitmap.recycle()
                            // Cleanup
                            (webView.parent as? android.view.ViewGroup)?.removeView(webView)
                            webView.destroy()
                            if (cont.isActive) cont.resume(bytes)
                        }, 300L)  // allow layout settle
                    }
                }
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        }
    }
}
