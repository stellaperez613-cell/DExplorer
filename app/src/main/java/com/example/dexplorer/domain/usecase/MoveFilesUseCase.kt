package com.example.dexplorer.domain.usecase

import com.example.dexplorer.domain.repository.FileSystemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class MoveFilesUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository,
    private val copyFilesUseCase: CopyFilesUseCase
) {
    suspend operator fun invoke(sourcePaths: List<String>, destinationPath: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val destination = File(destinationPath)

                if (!destination.exists() || !destination.isDirectory) {
                    return@withContext Result.failure(Exception("Invalid destination"))
                }

                // Try simple rename first (if same volume)
                val canRename = sourcePaths.all { sourcePath ->
                    val source = File(sourcePath)
                    source.parent == destinationPath
                }

                if (canRename) {
                    // All files in same directory, just rename
                    sourcePaths.forEach { sourcePath ->
                        val source = File(sourcePath)
                        val destFile = File(destination, source.name)
                        if (!source.renameTo(destFile)) {
                            return@withContext Result.failure(Exception("Failed to move: ${source.name}"))
                        }
                    }
                } else {
                    // Different volumes or directories - copy then delete
                    val copyResult = copyFilesUseCase(sourcePaths, destinationPath)
                    if (copyResult.isFailure) {
                        return@withContext copyResult
                    }

                    // Delete source files after successful copy
                    sourcePaths.forEach { sourcePath ->
                        val source = File(sourcePath)
                        if (!source.deleteRecursively()) {
                            return@withContext Result.failure(Exception("Failed to delete source: ${source.name}"))
                        }
                    }
                }

                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}