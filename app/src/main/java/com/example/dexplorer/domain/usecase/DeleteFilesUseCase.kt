package com.example.dexplorer.domain.usecase

import com.example.dexplorer.domain.repository.FileSystemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class DeleteFilesUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository
) {
    suspend operator fun invoke(paths: List<String>): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                paths.forEach { path ->
                    val file = File(path)
                    if (!file.exists()) {
                        return@withContext Result.failure(Exception("File does not exist: ${file.name}"))
                    }

                    if (!file.deleteRecursively()) {
                        return@withContext Result.failure(Exception("Failed to delete: ${file.name}"))
                    }
                }

                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}