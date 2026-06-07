package com.example.dexplorer.core.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.core.content.FileProvider
import java.io.File

object DefaultAppResolver {

    /**
     * Get the default app icon for a file type
     * Returns a Bitmap of the app icon, or null if no default app
     */
    /**
     * Get the default app icon for a file type
     * Returns a Bitmap of the app icon, or null if no default app
     */
    /**
     * Check if a default app is explicitly set (user pressed "Always")
     */
    /**
     * Check if a default app is explicitly set (user pressed "Always")
     */
    /**
     * Check if a default app is explicitly set (user pressed "Always")
     */
    private fun hasExplicitDefault(context: Context, intent: Intent): Boolean {
        return try {
            FileLogger.log("=== Checking for explicit default ===")

            val resolveInfo = context.packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )

            FileLogger.log("ResolveInfo: ${resolveInfo?.activityInfo?.packageName ?: "null"}")
            FileLogger.log("ResolveInfo label: ${resolveInfo?.loadLabel(context.packageManager)}")

            // Get all apps that can handle this
            val allApps = context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_ALL
            )
            FileLogger.log("Total apps that can handle this: ${allApps.size}")
            allApps.forEach { app ->
                FileLogger.log("  - ${app.loadLabel(context.packageManager)} (${app.activityInfo.packageName})")
            }

            // If null or system resolver, no explicit default
            if (resolveInfo == null) {
                FileLogger.log("Result: NO DEFAULT (resolveInfo is null)")
                return false
            }

            // ADD THIS NEW CHECK - Samsung Sound Picker exclusion
            if (resolveInfo.activityInfo.packageName == "com.samsung.android.app.soundpicker") {
                FileLogger.log("Result: NO DEFAULT (Samsung Sound Picker is not a real default)")
                return false
            }

            // Existing checks
            if (resolveInfo.activityInfo.packageName == "android" ||
                resolveInfo.activityInfo.packageName == "com.android.internal.app") {
                FileLogger.log("Result: NO DEFAULT (system resolver: ${resolveInfo.activityInfo.packageName})")
                return false
            }

            // If we got a specific app (not system resolver), user set a default
            FileLogger.log("Result: HAS DEFAULT (${resolveInfo.activityInfo.packageName})")
            return true

        } catch (e: Exception) {
            FileLogger.log("Error checking default: ${e.message}")
            e.printStackTrace()
            false
        }
    }

    /**
     * Get the default app icon - only if user explicitly set default via "Always"
     */
    fun getDefaultAppIcon(context: Context, filePath: String, mimeType: String?): Bitmap? {
        return try {
            FileLogger.log("========================================")
            FileLogger.log("getDefaultAppIcon for: $filePath")
            FileLogger.log("MIME type: $mimeType")

            val file = File(filePath)
            if (!file.exists() || file.isDirectory) {
                FileLogger.log("File doesn't exist or is directory")
                return null
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val detectedMimeType = detectSpecificMimeType(file, mimeType)
            FileLogger.log("Detected MIME type: $detectedMimeType")

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, detectedMimeType)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            // Only show icon if user explicitly set a default
            val hasDefault = hasExplicitDefault(context, intent)
            FileLogger.log("Has explicit default: $hasDefault")

            if (!hasDefault) {
                FileLogger.log("Returning NULL - no explicit default")
                return null
            }

            val resolveInfo = context.packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )

            if (resolveInfo != null && resolveInfo.activityInfo != null) {
                FileLogger.log("Generating icon for: ${resolveInfo.loadLabel(context.packageManager)}")
                val appIcon = resolveInfo.loadIcon(context.packageManager)
                return drawableToBitmap(appIcon)
            }

            FileLogger.log("ResolveInfo was null - returning null")
            null
        } catch (e: Exception) {
            FileLogger.log("ERROR in getDefaultAppIcon: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    /**
     * Get the package name of the default app (for badge tracking)
     */
    fun getDefaultAppPackage(context: Context, filePath: String, mimeType: String?): String? {
        return try {
            val file = File(filePath)
            if (!file.exists() || file.isDirectory) {
                return null
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val detectedMimeType = detectSpecificMimeType(file, mimeType)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, detectedMimeType)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val resolveInfo = context.packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )

            // Return package name (even if it's Samsung Sound Picker - we'll track it)
            resolveInfo?.activityInfo?.packageName
        } catch (e: Exception) {
            null
        }
    }
    /**
     * Get app name - only if user explicitly set as default via "Always"
     */
    fun getDefaultAppName(context: Context, filePath: String, mimeType: String?): String? {
        return try {
            FileLogger.log("========================================")
            FileLogger.log("getDefaultAppName for: $filePath")
            FileLogger.log("MIME type: $mimeType")

            val file = File(filePath)
            if (!file.exists() || file.isDirectory) {
                FileLogger.log("File doesn't exist or is directory")
                return null
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val detectedMimeType = detectSpecificMimeType(file, mimeType)
            FileLogger.log("Detected MIME type: $detectedMimeType")

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, detectedMimeType)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            // Only show name if user explicitly set a default
            val hasDefault = hasExplicitDefault(context, intent)
            FileLogger.log("Has explicit default: $hasDefault")

            if (!hasDefault) {
                FileLogger.log("Returning NULL - no explicit default")
                return null
            }

            val resolveInfo = context.packageManager.resolveActivity(
                intent,
                PackageManager.MATCH_DEFAULT_ONLY
            )

            val appName = resolveInfo?.loadLabel(context.packageManager)?.toString()
            FileLogger.log("App name: $appName")

            appName
        } catch (e: Exception) {
            FileLogger.log("ERROR in getDefaultAppName: ${e.message}")
            e.printStackTrace()
            null
        }
    }
    /**
     * Detect more specific MIME types (same logic as OpenFileUseCase)
     */
    private fun detectSpecificMimeType(file: File, providedMimeType: String?): String {
        val extension = file.extension.lowercase()

        // Be more specific for audio files
        return when (extension) {
            "flac" -> "audio/flac"
            "mp3" -> "audio/mpeg"
            "m4a", "aac" -> "audio/mp4"
            "ogg" -> "audio/ogg"
            "wav" -> "audio/wav"
            "opus" -> "audio/opus"

            // Video
            "mp4" -> "video/mp4"
            "mkv" -> "video/x-matroska"
            "avi" -> "video/x-msvideo"
            "webm" -> "video/webm"

            // Text files
            "srt", "sub", "ssa", "ass", "vtt" -> "text/plain"
            "txt", "log" -> "text/plain"

            else -> providedMimeType ?: "*/*"
        }
    }

    /**
     * Get app name for display
     */
    /**
     * Get app name for display
     */


    /**
     * Convert Drawable to Bitmap
     */
    fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is BitmapDrawable) {
            return drawable.bitmap
        }

        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 1
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 1

        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }
    /**
     * Get all apps that can open this file (for debugging)
     */
    fun getAllCompatibleApps(context: Context, filePath: String, mimeType: String?): List<String> {
        return try {
            val file = File(filePath)
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val detectedMimeType = detectSpecificMimeType(file, mimeType)
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, detectedMimeType)
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            context.packageManager.queryIntentActivities(intent, 0)
                .map { it.loadLabel(context.packageManager).toString() }
        } catch (e: Exception) {
            emptyList()
        }
    }
}