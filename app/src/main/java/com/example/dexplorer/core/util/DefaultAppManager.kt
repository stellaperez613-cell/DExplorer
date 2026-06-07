package com.example.dexplorer.core.util

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import androidx.core.content.FileProvider
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DefaultAppManager @Inject constructor(
    private val context: Context
) {
    private val prefs = context.getSharedPreferences("default_apps", Context.MODE_PRIVATE)

    companion object {
        private const val PREFIX_MIME = "mime_"
        private const val PREFIX_EXT = "ext_"
    }

    /**
     * Set user's preferred app for a file type
     */
    fun setDefaultApp(mimeType: String?, extension: String?, packageName: String) {
        FileLogger.log("========================================")
        FileLogger.log("DefaultAppManager.setDefaultApp()")
        FileLogger.log("  mimeType: $mimeType")
        FileLogger.log("  extension: $extension")
        FileLogger.log("  packageName: $packageName")

        prefs.edit().apply {
            if (!mimeType.isNullOrEmpty()) {
                val key = "$PREFIX_MIME$mimeType"
                putString(key, packageName)
                FileLogger.log("  Saved: $key = $packageName")
            }
            if (!extension.isNullOrEmpty()) {
                val key = "$PREFIX_EXT${extension.lowercase()}"
                putString(key, packageName)
                FileLogger.log("  Saved: $key = $packageName")
            }
            apply()
        }

        FileLogger.log("Default app saved successfully")
    }

    /**
     * Get user's preferred app for a file type
     */
    fun getDefaultApp(mimeType: String?, extension: String?): String? {
        if (!mimeType.isNullOrEmpty()) {
            val packageName = prefs.getString("$PREFIX_MIME$mimeType", null)
            if (packageName != null) return packageName
        }

        if (!extension.isNullOrEmpty()) {
            val packageName = prefs.getString("$PREFIX_EXT${extension.lowercase()}", null)
            if (packageName != null) return packageName
        }

        return null
    }

    /**
     * Clear default app for a file type
     */
    fun clearDefaultApp(mimeType: String?, extension: String?) {
        FileLogger.log("========================================")
        FileLogger.log("DefaultAppManager.clearDefaultApp()")
        FileLogger.log("  mimeType: $mimeType")
        FileLogger.log("  extension: $extension")

        prefs.edit().apply {
            if (!mimeType.isNullOrEmpty()) {
                val key = "$PREFIX_MIME$mimeType"
                remove(key)
                FileLogger.log("  Removed: $key")
            }
            if (!extension.isNullOrEmpty()) {
                val key = "$PREFIX_EXT${extension.lowercase()}"
                remove(key)
                FileLogger.log("  Removed: $key")
            }
            apply()
        }

        FileLogger.log("Default app cleared successfully")
    }

    /**
     * Get all apps that can handle this file type
     */
    fun getAvailableApps(filePath: String, mimeType: String?): List<AppInfo> {
        FileLogger.log("========================================")
        FileLogger.log("DefaultAppManager.getAvailableApps()")
        FileLogger.log("  filePath: $filePath")
        FileLogger.log("  mimeType: $mimeType")

        return try {
            val file = File(filePath)
            if (!file.exists()) {
                FileLogger.log("  File doesn't exist!")
                return emptyList()
            }

            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType ?: "*/*")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            val apps = context.packageManager.queryIntentActivities(
                intent,
                PackageManager.MATCH_ALL
            )

            FileLogger.log("  Total apps found: ${apps.size}")
            apps.forEach { resolveInfo ->
                FileLogger.log("    - ${resolveInfo.loadLabel(context.packageManager)} (${resolveInfo.activityInfo.packageName})")
            }

            val filtered = apps.filter { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                val isSystemPicker = pkg == "android" ||
                        pkg == "com.android.internal.app" ||
                        pkg == "com.samsung.android.app.soundpicker"

                if (isSystemPicker) {
                    FileLogger.log("  Filtered out system picker: $pkg")
                }

                !isSystemPicker
            }.map { resolveInfo ->
                AppInfo(
                    packageName = resolveInfo.activityInfo.packageName,
                    appName = resolveInfo.loadLabel(context.packageManager).toString(),
                    icon = drawableToBitmap(
                        resolveInfo.loadIcon(context.packageManager)
                    )
                )
            }

            FileLogger.log("  After filtering: ${filtered.size} apps")
            filtered.forEach { app ->
                FileLogger.log("    - ${app.appName} (${app.packageName})")
            }

            filtered
        } catch (e: Exception) {
            FileLogger.log("  ERROR: ${e.message}")
            e.printStackTrace()
            emptyList()
        }
    }

    /**
     * Get app icon for a package name
     */
    fun getAppIcon(packageName: String): Bitmap? {
        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            val icon = context.packageManager.getApplicationIcon(appInfo)
            drawableToBitmap(icon)
        } catch (e: Exception) {
            FileLogger.log("Error getting app icon for $packageName: ${e.message}")
            null
        }
    }

    /**
     * Get app name for a package name
     */
    fun getAppName(packageName: String): String? {
        FileLogger.log("========================================")
        FileLogger.log("DefaultAppManager.getAppName()")
        FileLogger.log("  packageName: $packageName")

        return try {
            val appInfo = context.packageManager.getApplicationInfo(packageName, 0)
            val name = context.packageManager.getApplicationLabel(appInfo).toString()
            FileLogger.log("  Result: $name")
            name
        } catch (e: Exception) {
            FileLogger.log("  ERROR: ${e.message}")
            null
        }
    }

    /**
     * Convert Drawable to Bitmap
     */
    private fun drawableToBitmap(drawable: android.graphics.drawable.Drawable): Bitmap {
        if (drawable is BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }

        val bitmap = if (drawable.intrinsicWidth <= 0 || drawable.intrinsicHeight <= 0) {
            Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        } else {
            Bitmap.createBitmap(
                drawable.intrinsicWidth,
                drawable.intrinsicHeight,
                Bitmap.Config.ARGB_8888
            )
        }

        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    /**
     * Clear all default apps (for settings)
     */
    fun clearAllDefaults() {
        FileLogger.log("Clearing all default apps")
        prefs.edit().clear().apply()
    }

    /**
     * Get all stored defaults (for settings UI)
     */
    fun getAllDefaults(): Map<String, String> {
        return prefs.all.filterKeys { it.startsWith(PREFIX_MIME) || it.startsWith(PREFIX_EXT) }
            .mapValues { it.value as String }
    }
}

data class AppInfo(
    val packageName: String,
    val appName: String,
    val icon: Bitmap?
)