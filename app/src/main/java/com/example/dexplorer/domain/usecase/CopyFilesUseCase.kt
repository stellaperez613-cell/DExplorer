package com.example.dexplorer.domain.usecase

import com.example.dexplorer.domain.repository.FileSystemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import javax.inject.Inject

class CopyFilesUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository
) {
    suspend operator fun invoke(sourcePaths: List<String>, destinationPath: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val destination = File(destinationPath)

                if (!destination.exists() || !destination.isDirectory) {
                    return@withContext Result.failure(Exception("Invalid destination"))
                }

                sourcePaths.forEach { sourcePath ->
                    val source = File(sourcePath)
                    if (!source.exists()) {
                        return@withContext Result.failure(Exception("Source does not exist: ${source.name}"))
                    }

                    val destFile = File(destination, source.name)

                    if (source.isDirectory) {
                        copyDirectory(source, destFile)
                    } else {
                        copyFile(source, destFile)
                    }
                }

                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    private fun copyFile(source: File, destination: File) {
        FileInputStream(source).use { input ->
            FileOutputStream(destination).use { output ->
                input.copyTo(output)
            }
        }
    }

    private fun copyDirectory(source: File, destination: File) {
        if (!destination.exists()) {
            destination.mkdirs()
        }

        source.listFiles()?.forEach { file ->
            val destFile = File(destination, file.name)
            if (file.isDirectory) {
                copyDirectory(file, destFile)
            } else {
                copyFile(file, destFile)
            }
        }
    }
}