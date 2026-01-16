package com.example.dexplorer.domain.usecase

import com.example.dexplorer.domain.repository.FileSystemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

class CreateFolderUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository
) {
    suspend operator fun invoke(parentPath: String, folderName: String): Result<String> =
        withContext(Dispatchers.IO) {
            try {
                val parent = File(parentPath)
                if (!parent.exists() || !parent.isDirectory) {
                    return@withContext Result.failure(Exception("Invalid parent directory"))
                }

                var newFolder = File(parent, folderName)
                var counter = 1

                // Handle duplicate names by adding (1), (2), etc.
                while (newFolder.exists()) {
                    newFolder = File(parent, "$folderName ($counter)")
                    counter++
                }

                if (!newFolder.mkdirs()) {
                    return@withContext Result.failure(Exception("Failed to create folder"))
                }

                Result.success(newFolder.absolutePath)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
}