package com.aislide.studio.domain.usecase

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.aislide.studio.data.repository.ProjectRepository
import com.aislide.studio.domain.service.PptxExportEngine
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Exports a presentation to .pptx (editable mode — text selectable, images
 * at original resolution). Saves to a user-selected SAF document.
 */
@Singleton
class ExportToPptxUseCase @Inject constructor(
    private val projectRepository: ProjectRepository,
    private val exportEngine: PptxExportEngine,
    @ApplicationContext private val context: Context
) {
    /**
     * @param projectId The presentation to export
     * @param outputUri SAF tree/document URI where the .pptx will be written
     * @param fileName The .pptx filename (without extension)
     */
    suspend operator fun invoke(projectId: Long, outputUri: android.net.Uri, fileName: String): Result<Long> =
        withContext(Dispatchers.IO) {
            runCatching {
                val presentation = projectRepository.getProject(projectId)
                    ?: error("Project not found: $projectId")

                val bytes = exportEngine.exportToBytes(presentation)

                val target = DocumentFile.fromTreeUri(context, outputUri)
                    ?: error("Invalid output directory")
                val existing = target.findFile("$fileName.pptx")
                existing?.delete()
                val file = target.createFile(
                    "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                    "$fileName.pptx"
                ) ?: error("Failed to create file")

                context.contentResolver.openOutputStream(file.uri)?.use { out ->
                    out.write(bytes)
                    out.flush()
                } ?: error("Failed to open output stream")

                bytes.size.toLong()
            }
        }
}
