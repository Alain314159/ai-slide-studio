package com.aislide.studio.domain.usecase

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import com.aislide.studio.data.local.database.AppJson
import com.aislide.studio.data.model.Palette
import com.aislide.studio.data.model.Presentation
import com.aislide.studio.data.model.Slide
import com.aislide.studio.data.model.SlideLayout
import com.aislide.studio.data.model.SlideObject
import com.aislide.studio.data.model.ObjectPosition
import com.aislide.studio.data.model.ObjectStyle
import com.aislide.studio.data.model.ObjectType
import com.aislide.studio.data.model.Typography
import com.aislide.studio.data.repository.ProjectRepository
import com.aislide.studio.domain.service.HtmlSlideRenderer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.xslf.usermodel.XMLSlideShow
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Imports a .pptx file into a new project.
 *
 * Reads the .pptx with Apache POI, extracts slides as best-effort text + image
 * objects, generates HTML for each slide so it can be edited in the app.
 *
 * Note: PPTX import is lossy — PPTX has richer layout concepts than our simple
 * object model. We extract text/images positioned by their XSLFShape anchors
 * and round to the closest [SlideLayout].
 */
@Singleton
class ImportPptxUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val htmlRenderer: HtmlSlideRenderer,
    private val imageStorage: com.aislide.studio.data.local.ImageStorageManager,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(inputUri: Uri, title: String): Result<Long> =
        withContext(Dispatchers.IO) {
            runCatching {
                val pptxFile = copyUriToCache(inputUri)
                val slides = mutableListOf<Slide>()

                XMLSlideShow(pptxFile.inputStream()).use { ppt ->
                    ppt.slides.forEachIndexed { idx, xslfSlide ->
                        val objs = mutableListOf<SlideObject>()

                        xslfSlide.shapes.forEach { shape ->
                            val anchor = shape.anchor
                            val pos = ObjectPosition(
                                x = (anchor.x.toFloat() / PptxExportEngineConstants.SLIDE_WIDTH_EMU).coerceIn(0f, 1f),
                                y = (anchor.y.toFloat() / PptxExportEngineConstants.SLIDE_HEIGHT_EMU).coerceIn(0f, 1f),
                                width = (anchor.width.toFloat() / PptxExportEngineConstants.SLIDE_WIDTH_EMU).coerceIn(0f, 1f),
                                height = (anchor.height.toFloat() / PptxExportEngineConstants.SLIDE_HEIGHT_EMU).coerceIn(0f, 1f)
                            )
                            when (shape) {
                                is org.apache.poi.xslf.usermodel.XSLFTextShape -> {
                                    val text = shape.text.ifBlank { return@forEach }
                                    val isTitle = shape.placeholder?.type == org.apache.poi.sl.usermodel.PlaceholderType.TITLE
                                    objs += SlideObject(
                                        id = UUID.randomUUID().toString(),
                                        type = if (isTitle) ObjectType.TITLE else ObjectType.TEXT_BOX,
                                        text = text,
                                        position = pos,
                                        style = ObjectStyle.default()
                                    )
                                }
                                is org.apache.poi.xslf.usermodel.XSLFPictureShape -> {
                                    val picBytes = shape.pictureData.data
                                    val localPath = imageStorage.saveImportedImage(picBytes)
                                    objs += SlideObject(
                                        id = UUID.randomUUID().toString(),
                                        type = ObjectType.IMAGE,
                                        imageUrl = localPath,
                                        position = pos,
                                        style = ObjectStyle.default()
                                    )
                                }
                                else -> { /* skip other shapes for v0.1 */ }
                            }
                        }

                        val slide = Slide(
                            id = UUID.randomUUID().toString(),
                            index = idx,
                            layout = inferLayout(objs),
                            title = objs.firstOrNull { it.type == ObjectType.TITLE }?.text ?: "Slide ${idx + 1}",
                            objects = objs
                        )
                        slides += slide.copy(htmlContent = htmlRenderer.render(slide, Palette.default(), Typography.default()))
                    }
                }

                val presentation = Presentation(
                    title = title,
                    description = "Imported from .pptx",
                    slides = slides
                )
                projectRepository.insertPresentation(presentation)
            }
        }

    private fun copyUriToCache(uri: Uri): File {
        val cacheFile = File(context.cacheDir, "import_${System.currentTimeMillis()}.pptx")
        context.contentResolver.openInputStream(uri)?.use { input ->
            cacheFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Failed to open input URI")
        return cacheFile
    }

    private fun inferLayout(objs: List<SlideObject>): SlideLayout {
        val hasImage = objs.any { it.type == ObjectType.IMAGE }
        val hasTitle = objs.any { it.type == ObjectType.TITLE }
        val hasBullets = objs.any { it.type == ObjectType.BULLET_LIST }
        return when {
            hasTitle && hasImage -> SlideLayout.TITLE_AND_IMAGE
            hasTitle && hasBullets -> SlideLayout.TITLE_AND_BULLETS
            hasTitle -> SlideLayout.TITLE
            else -> SlideLayout.TITLE_AND_BULLETS
        }
    }
}

object PptxExportEngineConstants {
    const val SLIDE_WIDTH_EMU = 12192000
    const val SLIDE_HEIGHT_EMU = 6858000
}
