package com.example.dexplorer.domain.usecase

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.dexplorer.core.util.FileLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject

class OpenFileUseCase @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * Open file with default app
     */
    /**
     * Open file with default app
     */
    /**
     * Open file with default app or show chooser
     */
    fun openFile(filePath: String, mimeType: String?, forceChooser: Boolean = false): Result<Boolean> {
        FileLogger.log("========================================")
        FileLogger.log("=== OPEN FILE REQUEST ===")
        FileLogger.log("File: $filePath")
        FileLogger.log("MIME: $mimeType")
        FileLogger.log("Thread: ${Thread.currentThread().name}")
        FileLogger.log("Timestamp: ${System.currentTimeMillis()}")

        return try {
            val file = File(filePath)
            if (!file.exists()) {
                FileLogger.log("ERROR: File does not exist")
                return Result.failure(Exception("File does not exist"))
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )
            FileLogger.log("URI created: $uri")

            val detectedMimeType = detectMimeType(file, mimeType)
            FileLogger.log("Detected MIME type: $detectedMimeType")

            // Check what app will handle this
            val testIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, detectedMimeType)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val resolveInfo = context.packageManager.resolveActivity(
                testIntent,
                PackageManager.MATCH_DEFAULT_ONLY
            )

            FileLogger.log("Default handler: ${resolveInfo?.activityInfo?.packageName}")
            FileLogger.log("Default handler name: ${resolveInfo?.loadLabel(context.packageManager)}")

            // Get all possible handlers
            val allHandlers = context.packageManager.queryIntentActivities(
                testIntent,
                PackageManager.MATCH_ALL
            )
            FileLogger.log("Total handlers available: ${allHandlers.size}")
            allHandlers.forEach { info ->
                FileLogger.log("  - ${info.loadLabel(context.packageManager)} (${info.activityInfo.packageName})")
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, detectedMimeType)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            try {
                context.startActivity(intent)
                Result.success(true)
            } catch (e: android.content.ActivityNotFoundException) {
                // No direct handler — fall back to chooser so the user can pick
                try {
                    val chooser = Intent.createChooser(intent, "Open with").apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(chooser)
                    Result.success(true)
                } catch (e2: android.content.ActivityNotFoundException) {
                    Result.failure(Exception("No app available to open this file type"))
                }
            }
        } catch (e: Exception) {
            FileLogger.log("ERROR opening file: ${e.javaClass.simpleName}")
            FileLogger.log("ERROR message: ${e.message}")
            FileLogger.log("ERROR stack trace:")
            e.stackTrace.take(5).forEach { element ->
                FileLogger.log("  at ${element}")
            }
            Result.failure(e)
        } finally {
            FileLogger.log("=== END OPEN FILE REQUEST ===")
            FileLogger.log("========================================")
        }
    }

    /**
     * Open file with chooser (Open with...)
     */
    fun openFileWith(filePath: String, mimeType: String?): Result<Boolean> {
        return try {
            val file = File(filePath)
            if (!file.exists()) {
                return Result.failure(Exception("File does not exist"))
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            // Use improved MIME type detection
            val detectedMimeType = detectMimeType(file, mimeType)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, detectedMimeType)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val chooserIntent = Intent.createChooser(intent, "Open with").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(chooserIntent)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Detect MIME type with fallbacks for common extensions
     */
    private fun detectMimeType(file: File, providedMimeType: String?): String {
        val extension = file.extension.lowercase()

        // Check for text-based files that might have wrong MIME types
        return when (extension) {
            // Subtitle files
            "srt", "sub", "ssa", "ass", "vtt" -> "text/plain"

            // Code files
            "kt", "java", "cpp", "c", "h", "hpp" -> "text/plain"
            "py", "js", "ts", "jsx", "tsx" -> "text/plain"
            "xml", "json", "yaml", "yml" -> "text/plain"
            "html", "htm", "css", "scss" -> "text/plain"
            "md", "markdown" -> "text/plain"
            "gradle", "properties", "gitignore" -> "text/plain"

            // Log files
            "log", "txt", "cfg", "conf", "ini" -> "text/plain"

            // Script files
            "sh", "bash", "bat", "ps1" -> "text/plain"

            // Office documents
            "pdf"  -> "application/pdf"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "pptx" -> "application/vnd.openxmlformats-officedocument.presentationml.presentation"
            "doc"  -> "application/msword"
            "xls"  -> "application/vnd.ms-excel"
            "ppt"  -> "application/vnd.ms-powerpoint"
            "odt"  -> "application/vnd.oasis.opendocument.text"
            "ods"  -> "application/vnd.oasis.opendocument.spreadsheet"
            "odp"  -> "application/vnd.oasis.opendocument.presentation"

            // Archives (be specific)
            "zip" -> "application/zip"
            "rar" -> "application/vnd.rar"
            "7z" -> "application/x-7z-compressed"
            "tar" -> "application/x-tar"
            "gz" -> "application/gzip"

            // APK
            "apk" -> "application/vnd.android.package-archive"

            // If we have a provided MIME type and it's not generic, use it
            else -> {
                if (providedMimeType != null &&
                    providedMimeType != "application/octet-stream" &&
                    providedMimeType.isNotBlank()) {
                    providedMimeType
                } else {
                    // Last resort: try to detect if it's text
                    if (isTextFile(file)) {
                        "text/plain"
                    } else {
                        providedMimeType ?: "*/*"
                    }
                }
            }
        }
    }

    /**
     * Check if file is likely a text file by reading first few bytes
     */
    private fun isTextFile(file: File): Boolean {
        return try {
            if (file.length() == 0L) return true

            val bytes = file.inputStream().use { input ->
                val buffer = ByteArray(512)
                val read = input.read(buffer)
                buffer.take(read)
            }

            // Check if bytes are printable ASCII/UTF-8
            bytes.all { byte ->
                byte in 0x09..0x0D || byte in 0x20..0x7E || byte.toInt() > 0x7F
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Try to open as text file
     */
    private fun openAsText(uri: Uri): Result<Boolean> {
        return try {
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/plain")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val chooserIntent = Intent.createChooser(intent, "Open with").apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }

            context.startActivity(chooserIntent)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(Exception("No text editor available"))
        }
    }
}