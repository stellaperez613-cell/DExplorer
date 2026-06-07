package com.example.dexplorer.domain.usecase

import com.example.dexplorer.data.model.FileItem
import com.example.dexplorer.data.model.FilePermissions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import android.webkit.MimeTypeMap
import com.example.dexplorer.core.util.DefaultAppManager

class GetDirectoryContentsUseCase @Inject constructor(
    private val defaultAppManager: DefaultAppManager
) {
    suspend operator fun invoke(path: String): Result<List<FileItem>> = withContext(Dispatchers.IO) {
        try {
            val directory = File(path)

            if (!directory.exists() || !directory.isDirectory) {
                return@withContext Result.failure(Exception("Invalid directory"))
            }

            val files = directory.listFiles()?.map { file ->
                fileToFileItem(file)
            }?.sortedWith(
                compareBy<FileItem> { !it.isDirectory }
                    .thenBy { it.name.lowercase() }
            ) ?: emptyList()

            Result.success(files)
        } catch (e: SecurityException) {
            Result.failure(Exception("Permission denied"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fileToFileItem(file: File): FileItem {
        val mimeType = getMimeType(file)

        val defaultAppIcon = if (!file.isDirectory) {
            val packageName = defaultAppManager.getDefaultApp(mimeType, file.extension)
            if (packageName != null) defaultAppManager.getAppIcon(packageName) else null
        } else {
            null
        }

        return FileItem(
            name = file.name,
            path = file.absolutePath,
            isDirectory = file.isDirectory,
            size = if (file.isDirectory) 0 else file.length(),
            lastModified = file.lastModified(),
            mimeType = mimeType,
            extension = file.extension,
            permissions = getFilePermissions(file),
            thumbnailPath = null,
            defaultAppIcon = defaultAppIcon
        )
    }

    private fun getMimeType(file: File): String? {
        val extension = file.extension.lowercase()

        // Be more specific for common types
        return when (extension) {
            "flac" -> "audio/flac"
            "mp3" -> "audio/mpeg"
            "m4a", "aac" -> "audio/mp4"
            "ogg" -> "audio/ogg"
            else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
        }
    }

    private fun getFilePermissions(file: File): FilePermissions {
        return FilePermissions(
            canRead = file.canRead(),
            canWrite = file.canWrite(),
            canExecute = file.canExecute()
        )
    }
}