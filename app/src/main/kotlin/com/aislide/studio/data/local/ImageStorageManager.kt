package com.aislide.studio.data.local

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.InputStream
import java.net.URL
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages local image files used by slide objects.
 *
 * v0.3 — IMPORTANT FIX: previously images were stored in [Context.getCacheDir]
 * which Android may clear at any time under storage pressure, causing AI-generated
 * images to silently disappear from saved presentations.
 *
 * Now uses [Context.getFilesDir] for persistent storage. Files survive:
 * - Cache clearing
 * - App updates
 * - System memory pressure
 *
 * Files are organized by purpose:
 * - generated_images/ : images produced by AI generation
 * - imported_images/ : images extracted from imported .pptx
 * - user_uploads/    : images picked from the gallery by the user
 *
 * Each file is named uniquely to avoid collisions: {prefix}_{timestamp}_{uuid8}.png
 */
@Singleton
class ImageStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val rootDir: File by lazy {
        File(context.filesDir, "slide_images").apply { if (!exists()) mkdirs() }
    }

    val generatedDir: File get() = File(rootDir, "generated_images").apply { if (!exists()) mkdirs() }
    val importedDir: File get() = File(rootDir, "imported_images").apply { if (!exists()) mkdirs() }
    val uploadedDir: File get() = File(rootDir, "user_uploads").apply { if (!exists()) mkdirs() }

    /**
     * Downloads an image from [url] and saves it to the persistent generated_images
     * directory. Returns the absolute local file path.
     */
    fun saveGeneratedImage(url: String, hint: String = ""): String {
        val bytes = URL(url).openStream().use { it.readBytes() }
        val safeName = "img_${hint.take(8).ifBlank { "ai" }}_${System.currentTimeMillis()}.png"
        val file = File(generatedDir, safeName)
        file.writeBytes(bytes)
        return file.absolutePath
    }

    /**
     * Saves raw bytes (e.g., from a picked image) to the persistent uploaded_images
     * directory. Returns the absolute local file path.
     */
    fun saveUploadedImage(stream: InputStream, extension: String = "png"): String {
        val safeExt = extension.lowercase().let { if (it == "jpg") "jpeg" else it }
        val safeName = "upload_${System.currentTimeMillis()}_${java.util.UUID.randomUUID().toString().take(8)}.$safeExt"
        val file = File(uploadedDir, safeName)
        file.outputStream().use { out -> stream.copyTo(out) }
        return file.absolutePath
    }

    /**
     * Saves bytes extracted from a .pptx import to the imported_images directory.
     */
    fun saveImportedImage(bytes: ByteArray, sourceName: String = ""): String {
        val safeName = "import_${sourceName.take(8)}_${System.currentTimeMillis()}.png"
        val file = File(importedDir, safeName)
        file.writeBytes(bytes)
        return file.absolutePath
    }

    /**
     * Deletes a stored image by absolute path. Silently ignores non-existent files
     * or files outside our managed directories (don't delete arbitrary user files).
     */
    fun delete(path: String): Boolean {
        val file = File(path)
        // Safety: only delete files inside our managed root
        if (!file.absolutePath.startsWith(rootDir.absolutePath)) return false
        return file.delete()
    }

    /**
     * Returns the total size in bytes used by all stored images.
     */
    fun totalSize(): Long {
        return (generatedDir.listFiles().orEmpty() +
                importedDir.listFiles().orEmpty() +
                uploadedDir.listFiles().orEmpty()).sumOf { it.length() }
    }

    /**
     * Lists all stored image paths (used for cleanup tools or migration).
     */
    fun allFiles(): List<File> {
        return (generatedDir.listFiles().orEmpty() +
                importedDir.listFiles().orEmpty() +
                uploadedDir.listFiles().orEmpty()).toList()
    }
}
