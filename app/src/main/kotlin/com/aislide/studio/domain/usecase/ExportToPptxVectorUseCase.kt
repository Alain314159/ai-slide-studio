package com.aislide.studio.domain.usecase

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.documentfile.provider.DocumentFile
import com.aislide.studio.data.model.Presentation
import com.aislide.studio.data.model.Slide
import com.aislide.studio.data.repository.ProjectRepository
import com.aislide.studio.domain.service.HtmlSlideRenderer
import com.aislide.studio.domain.service.PptxZipWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Exports a presentation to .pptx in "visual" mode — each slide is rendered
 * to a high-resolution bitmap via WebView capture, then inserted as a
 * full-slide picture in the .pptx.
 *
 * v0.3: uses [PptxZipWriter] for the .pptx ZIP+XML skeleton and adds
 * PNG media files for each slide. No Apache POI dependency.
 *
 * Result: visual fidelity is high, but text is NOT selectable/editable
 * (it's part of the picture).
 */
@Singleton
class ExportToPptxVectorUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val htmlRenderer: HtmlSlideRenderer,
    private val zipWriter: PptxZipWriter,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(
        projectId: Long,
        outputUri: Uri,
        fileName: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        runCatching {
            val presentation = projectRepository.getProject(projectId)
                ?: error("Project not found: $projectId")

            // 1. Write the ZIP skeleton (slides.xml, presentation.xml, etc.)
            val zipBytes = ByteArrayOutputStream().use { out ->
                zipWriter.export(presentation, out)
                out.toByteArray()
            }

            // 2. Capture each slide as PNG
            val slideImages: List<ByteArray> = presentation.slides.map { slide ->
                captureSlideToPng(slide, presentation)
            }

            // 3. Re-zip the skeleton + add the PNG media files
            // Read the existing zip, copy entries, and append ppt/media/imageN.png entries
            val finalBytes = ByteArrayOutputStream().use { finalOut ->
                ZipOutputStream(finalOut).use { newZip ->
                    // Read the existing zip and copy entries
                    val originalZip = java.util.zip.ZipInputStream(zipBytes.inputStream())
                    var entry = originalZip.nextEntry
                    while (entry != null) {
                        newZip.putNextEntry(ZipEntry(entry.name))
                        originalZip.copyTo(newZip)
                        newZip.closeEntry()
                        originalZip.closeEntry()
                        entry = originalZip.nextEntry
                    }
                    originalZip.close()
                    // Add PNG images
                    slideImages.forEachIndexed { idx, bytes ->
                        newZip.putNextEntry(ZipEntry("ppt/media/image${idx + 1}.png"))
                        newZip.write(bytes)
                        newZip.closeEntry()
                    }
                }
                finalOut.toByteArray()
            }

            // 4. Write to SAF document
            val target = DocumentFile.fromTreeUri(context, outputUri)
                ?: error("Invalid output directory")
            target.findFile("$fileName.pptx")?.delete()
            val file = target.createFile(
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "$fileName.pptx"
            ) ?: error("Failed to create file")
            context.contentResolver.openOutputStream(file.uri)?.use { out ->
                out.write(finalBytes)
                out.flush()
            } ?: error("Failed to open output stream")

            finalBytes.size.toLong()
        }
    }

    private suspend fun captureSlideToPng(slide: Slide, presentation: Presentation): ByteArray {
        val html = htmlRenderer.render(slide, presentation.palette, presentation.typography)
        val widthPx = 1920
        val heightPx = 1080

        return suspendCancellableCoroutine { cont ->
            val mainHandler = android.os.Handler(android.os.Looper.getMainLooper())
            mainHandler.post {
                val webView = WebView(context)
                webView.layoutParams = ViewGroup.LayoutParams(widthPx, heightPx)
                webView.settings.javaScriptEnabled = false
                webView.settings.loadWithOverviewMode = true
                webView.settings.useWideViewPort = true
                webView.measure(
                    android.view.View.MeasureSpec.makeMeasureSpec(widthPx, android.view.View.MeasureSpec.EXACTLY),
                    android.view.View.MeasureSpec.makeMeasureSpec(heightPx, android.view.View.MeasureSpec.EXACTLY)
                )
                webView.layout(0, 0, widthPx, heightPx)

                webView.webViewClient = object : WebViewClient() {
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
                            (webView.parent as? ViewGroup)?.removeView(webView)
                            webView.destroy()
                            if (cont.isActive) cont.resume(bytes)
                        }, 300L)
                    }
                }
                webView.loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        }
    }
}
