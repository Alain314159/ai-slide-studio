package com.aislide.studio.domain.usecase

import android.content.Context
import android.net.Uri
import com.aislide.studio.data.local.ImageStorageManager
import com.aislide.studio.data.model.ObjectPosition
import com.aislide.studio.data.model.ObjectStyle
import com.aislide.studio.data.model.ObjectType
import com.aislide.studio.data.model.Palette
import com.aislide.studio.data.model.Presentation
import com.aislide.studio.data.model.Slide
import com.aislide.studio.data.model.SlideLayout
import com.aislide.studio.data.model.SlideObject
import com.aislide.studio.data.model.Typography
import com.aislide.studio.data.repository.ProjectRepository
import com.aislide.studio.domain.service.HtmlSlideRenderer
import com.aislide.studio.domain.service.PptxZipWriter
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Imports a .pptx file into a new project.
 *
 * v0.3: implements basic PPTX import by parsing the ZIP+XML manually
 * (no Apache POI dependency, which requires java.awt.*).
 *
 * Reads each slide XML and extracts:
 * - Text runs from <a:p> elements (concatenates text per shape)
 * - Image references from <a:blip r:embed="rIdX"/>
 *
 * Lossy: ignores shapes, charts, tables, layouts. Each slide becomes
 * TITLE or TITLE_AND_BULLETS layout.
 */
@Singleton
class ImportPptxUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val htmlRenderer: HtmlSlideRenderer,
    private val imageStorage: ImageStorageManager,
    @ApplicationContext private val context: Context
) {
    suspend operator fun invoke(inputUri: Uri, title: String): Result<Long> =
        withContext(Dispatchers.IO) {
            runCatching {
                val cacheFile = copyUriToCache(inputUri)
                val slides = mutableListOf<Slide>()

                // Read the ZIP entries
                ZipInputStream(cacheFile.inputStream()).use { zip ->
                    val slideEntries = mutableListOf<Pair<String, String>>() // filename → xml
                    val imageEntries = mutableMapOf<String, ByteArray>() // media filename → bytes

                    var entry = zip.nextEntry
                    while (entry != null) {
                        if (entry.name.startsWith("ppt/slides/slide") && entry.name.endsWith(".xml")) {
                            slideEntries += entry.name to zip.readBytes().toString(Charsets.UTF_8)
                        } else if (entry.name.startsWith("ppt/media/")) {
                            imageEntries[entry.name] = zip.readBytes()
                        }
                        zip.closeEntry()
                        entry = zip.nextEntry
                    }

                    // Process each slide in order
                    slideEntries.sortedBy { it.first }.forEachIndexed { idx, (filename, xml) ->
                        val objs = parseSlideXml(xml, idx, imageEntries)
                        val slide = Slide(
                            id = UUID.randomUUID().toString(),
                            index = idx,
                            layout = inferLayout(objs),
                            title = objs.firstOrNull { it.type == ObjectType.TITLE }?.text
                                ?: objs.firstOrNull()?.text
                                ?: "Slide ${idx + 1}",
                            objects = objs
                        )
                        slides += slide.copy(
                            htmlContent = htmlRenderer.render(slide, Palette.default(), Typography.default())
                        )
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

    private fun parseSlideXml(
        xml: String,
        slideIndex: Int,
        images: Map<String, ByteArray>
    ): List<SlideObject> {
        val objs = mutableListOf<SlideObject>()

        // Find all <p:sp> ... </p:sp> blocks (text shapes)
        val textShapeRegex = """<p:sp>(.*?)</p:sp>""".toRegex(RegexOption.DOT_MATCHES_ALL)
        textShapeRegex.findAll(xml).forEachIndexed { shapeIdx, match ->
            val shapeXml = match.groupValues[1]
            // Extract text from <a:t>...</a:t> within this shape
            val textRegex = """<a:t>(.*?)</a:t>""".toRegex(RegexOption.DOT_MATCHES_ALL)
            val texts = textRegex.findAll(shapeXml).map { it.groupValues[1] }.toList()
                .map { unescapeXml(it) }

            if (texts.isEmpty()) return@forEachIndexed

            // Extract position from <a:off> and <a:ext>
            val offMatch = """<a:off x="(\d+)" y="(\d+)"/>""".toRegex().find(shapeXml)
            val extMatch = """<a:ext cx="(\d+)" cy="(\d+)"/>""".toRegex().find(shapeXml)
            val pos = if (offMatch != null && extMatch != null) {
                ObjectPosition(
                    x = (offMatch.groupValues[1].toLong() / PptxZipWriter.SLIDE_WIDTH_EMU).toFloat().coerceIn(0f, 1f),
                    y = (offMatch.groupValues[2].toLong() / PptxZipWriter.SLIDE_HEIGHT_EMU).toFloat().coerceIn(0f, 1f),
                    width = (extMatch.groupValues[1].toLong() / PptxZipWriter.SLIDE_WIDTH_EMU).toFloat().coerceIn(0f, 1f),
                    height = (extMatch.groupValues[2].toLong() / PptxZipWriter.SLIDE_HEIGHT_EMU).toFloat().coerceIn(0f, 1f)
                )
            } else {
                ObjectPosition(0.05f, 0.05f + 0.15f * shapeIdx, 0.9f, 0.15f)
            }

            val isTitle = slideIndex == 0 && shapeIdx == 0
            objs += SlideObject(
                id = UUID.randomUUID().toString(),
                type = if (isTitle) ObjectType.TITLE else ObjectType.TEXT_BOX,
                text = texts.joinToString("\n"),
                position = pos,
                style = ObjectStyle.default()
            )
        }

        // Find all <p:pic> ... </p:pic> blocks (pictures)
        val picRegex = """<p:pic>(.*?)</p:pic>""".toRegex(RegexOption.DOT_MATCHES_ALL)
        picRegex.findAll(xml).forEachIndexed { picIdx, match ->
            val picXml = match.groupValues[1]
            // Extract image reference
            val blipMatch = """r:embed="([^"]+)""".toRegex().find(picXml)
            val offMatch = """<a:off x="(\d+)" y="(\d+)"/>""".toRegex().find(picXml)
            val extMatch = """<a:ext cx="(\d+)" cy="(\d+)"/>""".toRegex().find(picXml)

            val rId = blipMatch?.groupValues?.get(1) ?: return@forEachIndexed
            val pos = if (offMatch != null && extMatch != null) {
                ObjectPosition(
                    x = (offMatch.groupValues[1].toLong() / PptxZipWriter.SLIDE_WIDTH_EMU).toFloat().coerceIn(0f, 1f),
                    y = (offMatch.groupValues[2].toLong() / PptxZipWriter.SLIDE_HEIGHT_EMU).toFloat().coerceIn(0f, 1f),
                    width = (extMatch.groupValues[1].toLong() / PptxZipWriter.SLIDE_WIDTH_EMU).toFloat().coerceIn(0f, 1f),
                    height = (extMatch.groupValues[2].toLong() / PptxZipWriter.SLIDE_HEIGHT_EMU).toFloat().coerceIn(0f, 1f)
                )
            } else {
                ObjectPosition(0.05f, 0.05f + 0.15f * picIdx, 0.9f, 0.15f)
            }
            // TODO: lookup rId in slide rels XML to find actual media filename
            // For now, save a placeholder image
            val imageBytes = images.values.firstOrNull() ?: return@forEachIndexed
            val localPath = imageStorage.saveImportedImage(imageBytes)
            objs += SlideObject(
                id = UUID.randomUUID().toString(),
                type = ObjectType.IMAGE,
                imageUrl = localPath,
                position = pos,
                style = ObjectStyle.default()
            )
        }

        return objs
    }

    private fun unescapeXml(text: String): String =
        text.replace("&lt;", "<").replace("&gt;", ">").replace("&amp;", "&").replace("&quot;", "\"").replace("&apos;", "'")

    private fun inferLayout(objs: List<SlideObject>): SlideLayout {
        val hasImage = objs.any { it.type == ObjectType.IMAGE }
        val hasTitle = objs.any { it.type == ObjectType.TITLE }
        return when {
            hasTitle && hasImage -> SlideLayout.TITLE_AND_IMAGE
            hasTitle -> SlideLayout.TITLE_AND_BULLETS
            else -> SlideLayout.TITLE_AND_BULLETS
        }
    }
}
