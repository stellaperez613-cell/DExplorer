package com.example.dexplorer.core.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.media.ThumbnailUtils
import android.os.Build
import android.provider.MediaStore
import android.util.Size
import java.io.File
import java.io.FileOutputStream
import android.graphics.pdf.PdfRenderer
import android.util.LruCache

object ThumbnailGenerator {

    private const val THUMBNAIL_SIZE = 512
    private lateinit var cacheDir: File

    // In-memory cache: absolute file path -> cached thumbnail path on disk
    private val pathCache = LruCache<String, String>(500)

    fun init(context: Context) {
        // Create thumbnails directory in cache
        cacheDir = File(context.cacheDir, "thumbnails")
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
    }

    /**
     * Generate thumbnail for a file based on its type
     * Returns the path to the cached thumbnail, or null if failed
     */
    /**
     * Generate thumbnail for a file based on its type
     * Returns the path to the cached thumbnail, or null if failed
     */
    fun generateThumbnail(filePath: String, mimeType: String?): String? {
        if (!::cacheDir.isInitialized) return null

        pathCache.get(filePath)?.let { return it }

        val file = File(filePath)
        if (!file.exists()) return null

        val cacheFileName = "${file.absolutePath.hashCode()}.jpg"
        val cachedThumbnail = File(cacheDir, cacheFileName)

        if (cachedThumbnail.exists() && cachedThumbnail.lastModified() >= file.lastModified()) {
            pathCache.put(filePath, cachedThumbnail.absolutePath)
            return cachedThumbnail.absolutePath
        }

        // Generate new thumbnail based on type
        val thumbnail = when {
            mimeType?.startsWith("image/") == true -> generateImageThumbnail(filePath)
            mimeType?.startsWith("video/") == true -> generateVideoThumbnail(filePath)
            mimeType?.startsWith("audio/") == true -> generateAudioThumbnail(filePath)
            mimeType == "application/pdf" -> generatePdfThumbnail(filePath)  // ADD THIS
            // Office documents
            mimeType?.contains("wordprocessingml") == true -> generateDocumentPlaceholder("DOC")  // ADD THIS
            mimeType?.contains("spreadsheetml") == true -> generateDocumentPlaceholder("XLS")     // ADD THIS
            mimeType?.contains("presentationml") == true -> generateDocumentPlaceholder("PPT")    // ADD THIS
            else -> null
        }

        return if (thumbnail != null) {
            saveThumbnail(thumbnail, cachedThumbnail)
            thumbnail.recycle()
            pathCache.put(filePath, cachedThumbnail.absolutePath)
            cachedThumbnail.absolutePath
        } else {
            null
        }
    }

    /**
     * Generate thumbnail for image file
     */
    private fun generateImageThumbnail(imagePath: String): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Modern API for Android 10+
                val file = File(imagePath)
                ThumbnailUtils.createImageThumbnail(
                    file,
                    Size(THUMBNAIL_SIZE, THUMBNAIL_SIZE),
                    null
                )
            } else {
                // Legacy approach for older Android
                val options = BitmapFactory.Options().apply {
                    inJustDecodeBounds = true
                }
                BitmapFactory.decodeFile(imagePath, options)

                // Calculate sample size
                options.inSampleSize = calculateInSampleSize(
                    options.outWidth,
                    options.outHeight,
                    THUMBNAIL_SIZE,
                    THUMBNAIL_SIZE
                )

                // Decode with sample size
                options.inJustDecodeBounds = false
                BitmapFactory.decodeFile(imagePath, options)
            }
        } catch (e: Exception) {
            FileLogger.log("Failed to generate image thumbnail: ${e.message}")
            null
        }
    }

    /**
     * Generate thumbnail for video file
     */
    private fun generateVideoThumbnail(videoPath: String): Bitmap? {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                // Modern API for Android 10+
                val file = File(videoPath)
                ThumbnailUtils.createVideoThumbnail(
                    file,
                    Size(THUMBNAIL_SIZE, THUMBNAIL_SIZE),
                    null
                )
            } else {
                // Legacy approach
                val retriever = MediaMetadataRetriever()
                retriever.setDataSource(videoPath)
                val bitmap = retriever.frameAtTime
                retriever.release()

                // Scale down if needed
                bitmap?.let { scaleBitmap(it, THUMBNAIL_SIZE) }
            }
        } catch (e: Exception) {
            FileLogger.log("Failed to generate video thumbnail: ${e.message}")
            null
        }
    }

    /**
     * Generate thumbnail for audio file (extract album art)
     */
    private fun generateAudioThumbnail(audioPath: String): Bitmap? {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(audioPath)

            // Extract embedded album art
            val albumArt = retriever.embeddedPicture
            retriever.release()

            if (albumArt != null) {
                val bitmap = BitmapFactory.decodeByteArray(albumArt, 0, albumArt.size)
                scaleBitmap(bitmap, THUMBNAIL_SIZE)
            } else {
                null
            }
        } catch (e: Exception) {
            FileLogger.log("Failed to generate audio thumbnail: ${e.message}")
            null
        }
    }
    /**
     * Generate thumbnail for PDF file (first page)
     */
    private fun generatePdfThumbnail(pdfPath: String): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            return null // PdfRenderer requires API 21+
        }

        return try {
            val file = File(pdfPath)
            val fileDescriptor = android.os.ParcelFileDescriptor.open(
                file,
                android.os.ParcelFileDescriptor.MODE_READ_ONLY
            )

            val pdfRenderer = android.graphics.pdf.PdfRenderer(fileDescriptor)

            if (pdfRenderer.pageCount > 0) {
                val page = pdfRenderer.openPage(0) // First page

                // Calculate dimensions maintaining aspect ratio
                val aspectRatio = page.width.toFloat() / page.height.toFloat()
                val width: Int
                val height: Int

                if (aspectRatio > 1) {
                    // Landscape
                    width = THUMBNAIL_SIZE
                    height = (THUMBNAIL_SIZE / aspectRatio).toInt()
                } else {
                    // Portrait
                    height = THUMBNAIL_SIZE
                    width = (THUMBNAIL_SIZE * aspectRatio).toInt()
                }

                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)

                // Render page to bitmap with white background
                val canvas = android.graphics.Canvas(bitmap)
                canvas.drawColor(android.graphics.Color.WHITE)
                page.render(bitmap, null, null, android.graphics.pdf.PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                page.close()
                pdfRenderer.close()
                fileDescriptor.close()

                bitmap
            } else {
                pdfRenderer.close()
                fileDescriptor.close()
                null
            }
        } catch (e: Exception) {
            FileLogger.log("Failed to generate PDF thumbnail: ${e.message}")
            null
        }
    }
    /**
     * Generate a styled placeholder thumbnail for Office documents
     */
    private fun generateDocumentPlaceholder(type: String): Bitmap {
        val bitmap = Bitmap.createBitmap(THUMBNAIL_SIZE, THUMBNAIL_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmap)

        // Background color based on document type
        val bgColor = when (type) {
            "DOC" -> android.graphics.Color.parseColor("#2B579A") // Word blue
            "XLS" -> android.graphics.Color.parseColor("#217346") // Excel green
            "PPT" -> android.graphics.Color.parseColor("#D24726") // PowerPoint orange
            else -> android.graphics.Color.parseColor("#757575")   // Gray default
        }

        canvas.drawColor(bgColor)

        // Draw white text with file type
        val paint = android.graphics.Paint().apply {
            color = android.graphics.Color.WHITE
            textSize = THUMBNAIL_SIZE / 4f
            textAlign = android.graphics.Paint.Align.CENTER
            isAntiAlias = true
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }

        val xPos = canvas.width / 2f
        val yPos = (canvas.height / 2f) - ((paint.descent() + paint.ascent()) / 2f)

        canvas.drawText(type, xPos, yPos, paint)

        return bitmap
    }
    /**
     * Save bitmap to file as JPEG
     */
    private fun saveThumbnail(bitmap: Bitmap, file: File) {
        try {
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }
        } catch (e: Exception) {
            FileLogger.log("Failed to save thumbnail: ${e.message}")
        }
    }

    /**
     * Calculate sample size for efficient image loading
     */
    private fun calculateInSampleSize(
        width: Int,
        height: Int,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight &&
                (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Scale bitmap to fit within max size while maintaining aspect ratio
     */
    private fun scaleBitmap(bitmap: Bitmap, maxSize: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxSize && height <= maxSize) {
            return bitmap
        }

        val scale = minOf(
            maxSize.toFloat() / width,
            maxSize.toFloat() / height
        )

        val newWidth = (width * scale).toInt()
        val newHeight = (height * scale).toInt()

        val scaled = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)

        if (scaled != bitmap) {
            bitmap.recycle()
        }

        return scaled
    }

    /**
     * Clear all cached thumbnails
     */
    fun clearCache() {
        pathCache.evictAll()
        if (::cacheDir.isInitialized) {
            cacheDir.listFiles()?.forEach { it.delete() }
        }
    }
    /**
     * Generate icon for file types that don't have thumbnails
     */
    fun generateFileTypeIcon(extension: String): String? {
        if (!::cacheDir.isInitialized) return null

        val cacheKey = "ext:$extension"
        pathCache.get(cacheKey)?.let { return it }

        val cacheFileName = "filetype_${extension.lowercase()}.png"
        val cachedIcon = File(cacheDir, cacheFileName)

        if (cachedIcon.exists()) {
            pathCache.put(cacheKey, cachedIcon.absolutePath)
            return cachedIcon.absolutePath
        }

        // Generate new icon
        val iconBitmap = FileTypeIconGenerator.generateIcon(extension)

        return try {
            saveThumbnail(iconBitmap, cachedIcon)
            iconBitmap.recycle()
            pathCache.put(cacheKey, cachedIcon.absolutePath)
            cachedIcon.absolutePath
        } catch (e: Exception) {
            FileLogger.log("Failed to generate file type icon: ${e.message}")
            null
        }
    }
    /**
     * Get cache size in bytes
     */
    fun getCacheSize(): Long {
        if (!::cacheDir.isInitialized) return 0

        return cacheDir.listFiles()?.sumOf { it.length() } ?: 0
    }
}