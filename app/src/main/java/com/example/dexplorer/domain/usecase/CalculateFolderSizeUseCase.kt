package com.example.dexplorer.domain.usecase

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import kotlin.coroutines.coroutineContext
import kotlinx.coroutines.ensureActive

// FolderStats MUST be at package level (outside the class)
data class FolderStats(
    val totalSize: Long,
    val fileCount: Int,
    val folderCount: Int
)

class CalculateFolderSizeUseCase @Inject constructor() {

    suspend operator fun invoke(path: String): Result<FolderStats> = withContext(Dispatchers.IO) {
        try {
            val folder = File(path)
            if (!folder.exists() || !folder.isDirectory) {
                return@withContext Result.failure(Exception("Not a valid folder"))
            }
            Result.success(calculateRecursive(folder))
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            Result.failure(Exception("Permission denied"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun calculateRecursive(folder: File): FolderStats {
        coroutineContext.ensureActive()
        var totalSize = 0L
        var fileCount = 0
        var folderCount = 0

        try {
            folder.listFiles()?.forEach { file ->
                coroutineContext.ensureActive()
                if (file.isDirectory) {
                    folderCount++
                    val subStats = calculateRecursive(file)
                    totalSize += subStats.totalSize
                    fileCount += subStats.fileCount
                    folderCount += subStats.folderCount
                } else {
                    fileCount++
                    totalSize += file.length()
                }
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: SecurityException) {
            // Skip folders we can't access
        }

        return FolderStats(totalSize, fileCount, folderCount)
    }
}