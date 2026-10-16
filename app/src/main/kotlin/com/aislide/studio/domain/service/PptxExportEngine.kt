package com.aislide.studio.domain.service

import com.aislide.studio.data.model.Presentation
import java.io.ByteArrayOutputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exports a [Presentation] to .pptx — EDITABLE mode (text selectable).
 *
 * v0.3: now uses [PptxZipWriter] (manual ZIP + XML) instead of Apache POI,
 * because Apache POI requires java.awt.* which is NOT available on Android.
 *
 * The output is a valid .pptx file that opens in PowerPoint, LibreOffice,
 * Google Slides, and any OOXML-compliant reader.
 *
 * Text objects become real text frames (selectable/editable in PowerPoint).
 * Images are inserted as picture shapes referencing embedded media files.
 */
@Singleton
class PptxExportEngine @Inject constructor(
    private val zipWriter: PptxZipWriter
) {
    fun export(presentation: Presentation, output: OutputStream) {
        zipWriter.export(presentation, output)
    }

    fun exportToBytes(presentation: Presentation): ByteArray {
        return ByteArrayOutputStream().use { out ->
            export(presentation, out)
            out.toByteArray()
        }
    }

    companion object {
        // EMU constants for 16:9 slide (13.333in × 7.5in) — referenced by zipWriter
        const val SLIDE_WIDTH_EMU = 12192000
        const val SLIDE_HEIGHT_EMU = 6858000
    }
}
