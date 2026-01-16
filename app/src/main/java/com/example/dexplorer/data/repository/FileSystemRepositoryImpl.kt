package com.example.dexplorer.data.repository

import android.content.Context
import android.os.Environment
import android.os.StatFs
import com.example.dexplorer.data.model.FileItem
import com.example.dexplorer.data.model.FilePermissions
import com.example.dexplorer.data.model.StorageDevice
import com.example.dexplorer.data.model.StorageType
import com.example.dexplorer.domain.repository.FileSystemRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import android.webkit.MimeTypeMap
import java.io.File
import javax.inject.Inject

class FileSystemRepositoryImpl @Inject constructor(
    private val context: Context
) : FileSystemRepository {

    override suspend fun getDirectoryContents(path: String): Result<List<FileItem>> =
        withContext(Dispatchers.IO) {
            try {
                val directory = File(path)

                if (!directory.exists()) {
                    return@withContext Result.failure(Exception("Directory does not exist"))
                }

                if (!directory.isDirectory) {
                    return@withContext Result.failure(Exception("Path is not a directory"))
                }

                val files = directory.listFiles()?.mapNotNull { file ->
                    try {
                        fileToFileItem(file)
                    } catch (e: Exception) {
                        null // Skip files we can't read
                    }
                } ?: emptyList()

                // Sort: directories first, then by name
                val sortedFiles = files.sortedWith(
                    compareByDescending<FileItem> { it.isDirectory }
                        .thenBy { it.name.lowercase() }
                )

                Result.success(sortedFiles)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getStorageDevices(): Result<List<StorageDevice>> =
        withContext(Dispatchers.IO) {
            try {
                val devices = mutableListOf<StorageDevice>()

                // Add internal storage
                val internalPath = Environment.getExternalStorageDirectory().absolutePath
                val internalStats = StatFs(internalPath)
                devices.add(
                    StorageDevice(
                        name = "Internal Storage",
                        path = internalPath,
                        totalBytes = internalStats.totalBytes,
                        availableBytes = internalStats.availableBytes,
                        type = StorageType.INTERNAL,
                        isRemovable = false,
                        isPrimary = true
                    )
                )

                // TODO: Add SD card and USB detection in later phase

                Result.success(devices)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun exists(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).exists()
    }

    override suspend fun isDirectory(path: String): Boolean = withContext(Dispatchers.IO) {
        File(path).isDirectory
    }

    override suspend fun createDirectory(path: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val directory = File(path)
                val created = directory.mkdirs()
                Result.success(created)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun delete(path: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                val deleted = file.deleteRecursively()
                Result.success(deleted)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun rename(oldPath: String, newPath: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val oldFile = File(oldPath)
                val newFile = File(newPath)
                val renamed = oldFile.renameTo(newFile)
                Result.success(renamed)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun copy(sources: List<String>, destination: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                // TODO: Implement in Phase 3
                Result.success(false)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun move(sources: List<String>, destination: String): Result<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                // TODO: Implement in Phase 3
                Result.success(false)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getFileDetails(path: String): Result<FileItem> =
        withContext(Dispatchers.IO) {
            try {
                val file = File(path)
                if (!file.exists()) {
                    return@withContext Result.failure(Exception("File does not exist"))
                }
                Result.success(fileToFileItem(file))
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override fun searchFiles(directory: String, query: String): Flow<List<FileItem>> = flow {
        // TODO: Implement in Phase 5
        emit(emptyList<FileItem>())
    }.flowOn(Dispatchers.IO)

    /**
     * Convert Java File to FileItem model
     */
    private fun fileToFileItem(file: File): FileItem {
        val extension = file.extension.takeIf { it.isNotEmpty() }
        val mimeType = extension?.let {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(it.lowercase())
        }

        return FileItem(
            name = file.name,
            path = file.absolutePath,
            isDirectory = file.isDirectory,
            size = if (file.isDirectory) 0L else file.length(),
            lastModified = file.lastModified(),
            mimeType = mimeType,
            extension = extension,
            isHidden = file.isHidden,
            permissions = FilePermissions(
                canRead = file.canRead(),
                canWrite = file.canWrite(),
                canExecute = file.canExecute()
            )
        )
    }
}