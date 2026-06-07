package com.example.dexplorer.domain.usecase

import com.example.dexplorer.data.model.FileItem
import com.example.dexplorer.domain.repository.FileSystemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class SearchFilesUseCase @Inject constructor(
    private val repository: FileSystemRepository
) {
    suspend operator fun invoke(
        startPath: String,
        query: String,
        searchSubfolders: Boolean = true
    ): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            if (query.isBlank()) {
                return@withContext Result.success(emptyList())
            }

            val results = mutableListOf<FileItem>()
            val startDirectory = File(startPath)

            if (!startDirectory.exists() || !startDirectory.isDirectory) {
                return@withContext Result.failure(
                    IllegalArgumentException("Invalid start path")
                )
            }

            searchDirectory(startDirectory, query.lowercase(), searchSubfolders, results)

            // Sort results: directories first, then alphabetically
            val sorted = results.sortedWith(
                compareByDescending<FileItem> { it.isDirectory }
                    .thenBy { it.name.lowercase() }
            )

            Result.success(sorted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun searchDirectory(
        directory: File,
        query: String,
        searchSubfolders: Boolean,
        results: MutableList<FileItem>
    ) {
        try {
            val files = directory.listFiles() ?: return

            for (file in files) {
                // Check if filename matches query
                if (file.name.lowercase().contains(query)) {
                    // Convert to FileItem using repository
                    val fileItem = fileToFileItem(file)
                    results.add(fileItem)
                }

                // Recursively search subfolders if enabled
                if (searchSubfolders && file.isDirectory) {
                    searchDirectory(file, query, searchSubfolders, results)
                }
            }
        } catch (e: SecurityException) {
            // Skip directories we don't have permission to read
        }
    }

    private fun fileToFileItem(file: File): FileItem {
        return FileItem(
            name = file.name,
            path = file.absolutePath,
            size = if (file.isFile) file.length() else 0L,
            isDirectory = file.isDirectory,
            lastModified = file.lastModified(),
            mimeType = getMimeType(file),
            extension = file.extension,
            permissions = com.example.dexplorer.data.model.FilePermissions(
                canRead = file.canRead(),
                canWrite = file.canWrite(),
                canExecute = file.canExecute()
            )
        )
    }

    private fun getMimeType(file: File): String? {
        if (file.isDirectory) return null

        val extension = file.extension.lowercase()
        return when (extension) {
            "jpg", "jpeg", "png", "gif", "webp" -> "image/$extension"
            "mp4", "mkv", "avi", "mov" -> "video/$extension"
            "mp3", "wav", "flac", "m4a" -> "audio/$extension"
            "pdf" -> "application/pdf"
            "txt" -> "text/plain"
            "zip", "rar", "7z" -> "application/$extension"
            else -> "application/octet-stream"
        }
    }
}