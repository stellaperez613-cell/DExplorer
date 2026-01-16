package com.example.dexplorer.domain.usecase

import com.example.dexplorer.domain.repository.FileSystemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class RenameFileUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository
) {
    suspend operator fun invoke(oldPath: String, newName: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val oldFile = File(oldPath)
                if (!oldFile.exists()) {
                    return@withContext Result.failure(Exception("File does not exist"))
                }

                val parent = oldFile.parentFile ?: return@withContext Result.failure(Exception("Invalid path"))
                val newFile = File(parent, newName)

                if (newFile.exists()) {
                    return@withContext Result.failure(Exception("A file with this name already exists"))
                }

                if (!oldFile.renameTo(newFile)) {
                    return@withContext Result.failure(Exception("Failed to rename file"))
                }

                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    /**
     * Check if extension changed
     */
    fun hasExtensionChanged(oldName: String, newName: String): Boolean {
        val oldExt = oldName.substringAfterLast('.', "")
        val newExt = newName.substringAfterLast('.', "")
        return oldExt != newExt && oldExt.isNotEmpty()
    }
}