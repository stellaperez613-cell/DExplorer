package com.example.dexplorer.core.util

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb

object FileTypeIconGenerator {

    private const val ICON_SIZE = 512
    private const val CORNER_RADIUS = 48f

    data class FileTypeStyle(
        val backgroundColor: Color,
        val displayText: String,
        val textColor: Color = Color.White
    )

    /**
     * Get file type style based on extension
     */
    fun getFileTypeStyle(extension: String): FileTypeStyle {
        return when (extension.lowercase()) {
            // Images
            "psd", "psb" -> FileTypeStyle(
                backgroundColor = Color(0xFF31A8FF),
                displayText = "PSD"
            )
            "ai" -> FileTypeStyle(
                backgroundColor = Color(0xFFFF9A00),
                displayText = "AI"
            )
            "svg" -> FileTypeStyle(
                backgroundColor = Color(0xFFFFB13B),
                displayText = "SVG"
            )
            "png" -> FileTypeStyle(
                backgroundColor = Color(0xFF4CAF50),
                displayText = "PNG"
            )
            "jpg", "jpeg" -> FileTypeStyle(
                backgroundColor = Color(0xFF2196F3),
                displayText = "JPG"
            )
            "gif" -> FileTypeStyle(
                backgroundColor = Color(0xFFE91E63),
                displayText = "GIF"
            )
            "webp" -> FileTypeStyle(
                backgroundColor = Color(0xFF9C27B0),
                displayText = "WEBP"
            )
            "bmp" -> FileTypeStyle(
                backgroundColor = Color(0xFF795548),
                displayText = "BMP"
            )

            // Video
            "mp4" -> FileTypeStyle(
                backgroundColor = Color(0xFFE53935),
                displayText = "MP4"
            )
            "mkv" -> FileTypeStyle(
                backgroundColor = Color(0xFFD32F2F),
                displayText = "MKV"
            )
            "avi" -> FileTypeStyle(
                backgroundColor = Color(0xFFC62828),
                displayText = "AVI"
            )
            "mov" -> FileTypeStyle(
                backgroundColor = Color(0xFFB71C1C),
                displayText = "MOV"
            )
            "webm" -> FileTypeStyle(
                backgroundColor = Color(0xFF8E24AA),
                displayText = "WEBM"
            )
            "flv" -> FileTypeStyle(
                backgroundColor = Color(0xFF6A1B9A),
                displayText = "FLV"
            )

            // Audio
            "mp3" -> FileTypeStyle(
                backgroundColor = Color(0xFF1976D2),
                displayText = "MP3"
            )
            "flac" -> FileTypeStyle(
                backgroundColor = Color(0xFF0D47A1),
                displayText = "FLAC"
            )
            "wav" -> FileTypeStyle(
                backgroundColor = Color(0xFF1565C0),
                displayText = "WAV"
            )
            "aac" -> FileTypeStyle(
                backgroundColor = Color(0xFF0277BD),
                displayText = "AAC"
            )
            "ogg" -> FileTypeStyle(
                backgroundColor = Color(0xFF01579B),
                displayText = "OGG"
            )
            "m4a" -> FileTypeStyle(
                backgroundColor = Color(0xFF0288D1),
                displayText = "M4A"
            )
            "wma" -> FileTypeStyle(
                backgroundColor = Color(0xFF039BE5),
                displayText = "WMA"
            )

            // Documents
            "pdf" -> FileTypeStyle(
                backgroundColor = Color(0xFFE53935),
                displayText = "PDF"
            )
            "doc", "docx" -> FileTypeStyle(
                backgroundColor = Color(0xFF2B579A),
                displayText = "DOC"
            )
            "xls", "xlsx" -> FileTypeStyle(
                backgroundColor = Color(0xFF217346),
                displayText = "XLS"
            )
            "ppt", "pptx" -> FileTypeStyle(
                backgroundColor = Color(0xFFD24726),
                displayText = "PPT"
            )
            "odt" -> FileTypeStyle(
                backgroundColor = Color(0xFF0369A1),
                displayText = "ODT"
            )
            "ods" -> FileTypeStyle(
                backgroundColor = Color(0xFF059669),
                displayText = "ODS"
            )
            "odp" -> FileTypeStyle(
                backgroundColor = Color(0xFFDC2626),
                displayText = "ODP"
            )

            // Code
            "java" -> FileTypeStyle(
                backgroundColor = Color(0xFFEA2D2E),
                displayText = "JAVA"
            )
            "kt", "kts" -> FileTypeStyle(
                backgroundColor = Color(0xFF7F52FF),
                displayText = "KT"
            )
            "py" -> FileTypeStyle(
                backgroundColor = Color(0xFF3776AB),
                displayText = "PY"
            )
            "js" -> FileTypeStyle(
                backgroundColor = Color(0xFFF7DF1E),
                displayText = "JS",
                textColor = Color.Black
            )
            "ts" -> FileTypeStyle(
                backgroundColor = Color(0xFF3178C6),
                displayText = "TS"
            )
            "jsx", "tsx" -> FileTypeStyle(
                backgroundColor = Color(0xFF61DAFB),
                displayText = "REACT",
                textColor = Color.Black
            )
            "html", "htm" -> FileTypeStyle(
                backgroundColor = Color(0xFFE34F26),
                displayText = "HTML"
            )
            "css" -> FileTypeStyle(
                backgroundColor = Color(0xFF1572B6),
                displayText = "CSS"
            )
            "cpp", "cc", "cxx" -> FileTypeStyle(
                backgroundColor = Color(0xFF00599C),
                displayText = "C++"
            )
            "c" -> FileTypeStyle(
                backgroundColor = Color(0xFFA8B9CC),
                displayText = "C",
                textColor = Color.Black
            )
            "h", "hpp" -> FileTypeStyle(
                backgroundColor = Color(0xFF555555),
                displayText = "H"
            )
            "cs" -> FileTypeStyle(
                backgroundColor = Color(0xFF239120),
                displayText = "C#"
            )
            "php" -> FileTypeStyle(
                backgroundColor = Color(0xFF777BB4),
                displayText = "PHP"
            )
            "rb" -> FileTypeStyle(
                backgroundColor = Color(0xFFCC342D),
                displayText = "RUBY"
            )
            "go" -> FileTypeStyle(
                backgroundColor = Color(0xFF00ADD8),
                displayText = "GO"
            )
            "rs" -> FileTypeStyle(
                backgroundColor = Color(0xFFDEA584),
                displayText = "RUST",
                textColor = Color.Black
            )
            "swift" -> FileTypeStyle(
                backgroundColor = Color(0xFFFA7343),
                displayText = "SWIFT"
            )
            "xml" -> FileTypeStyle(
                backgroundColor = Color(0xFFFF6600),
                displayText = "XML"
            )
            "json" -> FileTypeStyle(
                backgroundColor = Color(0xFF000000),
                displayText = "JSON"
            )
            "yaml", "yml" -> FileTypeStyle(
                backgroundColor = Color(0xFFCB171E),
                displayText = "YAML"
            )

            // Subtitles
            "srt" -> FileTypeStyle(
                backgroundColor = Color(0xFF6366F1),
                displayText = "SRT"
            )
            "sub" -> FileTypeStyle(
                backgroundColor = Color(0xFF8B5CF6),
                displayText = "SUB"
            )
            "ass", "ssa" -> FileTypeStyle(
                backgroundColor = Color(0xFFA855F7),
                displayText = "ASS"
            )
            "vtt" -> FileTypeStyle(
                backgroundColor = Color(0xFF9333EA),
                displayText = "VTT"
            )

            // Archives
            "zip" -> FileTypeStyle(
                backgroundColor = Color(0xFFFF9800),
                displayText = "ZIP"
            )
            "rar" -> FileTypeStyle(
                backgroundColor = Color(0xFFF57C00),
                displayText = "RAR"
            )
            "7z" -> FileTypeStyle(
                backgroundColor = Color(0xFFEF6C00),
                displayText = "7Z"
            )
            "tar" -> FileTypeStyle(
                backgroundColor = Color(0xFFE65100),
                displayText = "TAR"
            )
            "gz", "gzip" -> FileTypeStyle(
                backgroundColor = Color(0xFFD84315),
                displayText = "GZ"
            )

            // Android/APK
            "apk" -> FileTypeStyle(
                backgroundColor = Color(0xFF3DDC84),
                displayText = "APK",
                textColor = Color.Black
            )
            "aab" -> FileTypeStyle(
                backgroundColor = Color(0xFF34A853),
                displayText = "AAB"
            )

            // Torrents
            "torrent" -> FileTypeStyle(
                backgroundColor = Color(0xFF00ACC1),
                displayText = "TORRENT"
            )

            // Text
            "txt" -> FileTypeStyle(
                backgroundColor = Color(0xFF607D8B),
                displayText = "TXT"
            )
            "md", "markdown" -> FileTypeStyle(
                backgroundColor = Color(0xFF000000),
                displayText = "MD"
            )
            "log" -> FileTypeStyle(
                backgroundColor = Color(0xFF455A64),
                displayText = "LOG"
            )

            // Database
            "db", "sqlite" -> FileTypeStyle(
                backgroundColor = Color(0xFF003B57),
                displayText = "DB"
            )
            "sql" -> FileTypeStyle(
                backgroundColor = Color(0xFF00758F),
                displayText = "SQL"
            )

            // Fonts
            "ttf", "otf" -> FileTypeStyle(
                backgroundColor = Color(0xFF9E9E9E),
                displayText = "FONT"
            )

            // Default
            else -> FileTypeStyle(
                backgroundColor = Color(0xFF757575),
                displayText = extension.uppercase().take(4)
            )
        }
    }

    /**
     * Generate a bitmap icon for a file type
     */
    /**
     * Generate a bitmap icon for a file type
     */
    fun generateIcon(extension: String): Bitmap {
        val fileStyle = getFileTypeStyle(extension)  // RENAMED from 'style' to 'fileStyle'

        val bitmap = Bitmap.createBitmap(ICON_SIZE, ICON_SIZE, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Draw rounded rectangle background
        val bgPaint = Paint().apply {
            color = fileStyle.backgroundColor.toArgb()
            isAntiAlias = true
            style = Paint.Style.FILL  // Now 'style' refers to Paint.Style, not our variable
        }

        val rect = RectF(0f, 0f, ICON_SIZE.toFloat(), ICON_SIZE.toFloat())
        canvas.drawRoundRect(rect, CORNER_RADIUS, CORNER_RADIUS, bgPaint)

        // Draw extension text
        val textPaint = Paint().apply {
            color = fileStyle.textColor.toArgb()
            textSize = if (fileStyle.displayText.length <= 3) 180f else 120f
            textAlign = Paint.Align.CENTER
            isAntiAlias = true
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val xPos = ICON_SIZE / 2f
        val yPos = (ICON_SIZE / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)

        canvas.drawText(fileStyle.displayText, xPos, yPos, textPaint)

        return bitmap
    }
}