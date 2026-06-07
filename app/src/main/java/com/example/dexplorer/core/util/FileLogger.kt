package com.example.dexplorer.core.util

import android.content.Context
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileLogger {
    private var writer: BufferedWriter? = null
    private var logFilePath: String? = null
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    fun init(context: Context) {
        try {
            val logsDir = File(context.getExternalFilesDir(null), "logs")
            if (!logsDir.exists()) logsDir.mkdirs()

            val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
            val logFile = File(logsDir, "dexplorer_$timestamp.txt")
            logFilePath = logFile.absolutePath
            writer = BufferedWriter(FileWriter(logFile, true))

            log("===== DExplorer Log Started =====")
            log("Log file: $logFilePath")
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun log(message: String) {
        try {
            android.util.Log.d("DEXPLORER", message)
            writer?.apply {
                write("[${timeFormat.format(Date())}] $message")
                newLine()
                flush()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getLogFilePath(): String? = logFilePath
}