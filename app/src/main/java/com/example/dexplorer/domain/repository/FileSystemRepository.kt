package com.example.dexplorer.domain.repository

import com.example.dexplorer.data.model.FileItem
import com.example.dexplorer.data.model.StorageDevice
import kotlinx.coroutines.flow.Flow

interface FileSystemRepository {

    /**
     * Get contents of a directory
     */
    suspend fun getDirectoryContents(path: String): Result<List<FileItem>>

    /**
     * Get storage devices (internal, SD card, USB drives)
     */
    suspend fun getStorageDevices(): Result<List<StorageDevice>>

    /**
     * Check if a path exists
     */
    suspend fun exists(path: String): Boolean

    /**
     * Check if a path is a directory
     */
    suspend fun isDirectory(path: String): Boolean

    /**
     * Create a new directory
     */
    suspend fun createDirectory(path: String): Result<Boolean>

    /**
     * Delete a file or directory
     */
    suspend fun delete(path: String): Result<Boolean>

    /**
     * Rename a file or directory
     */
    suspend fun rename(oldPath: String, newPath: String): Result<Boolean>

    /**
     * Copy files to destination
     */
    suspend fun copy(sources: List<String>, destination: String): Result<Boolean>

    /**
     * Move files to destination
     */
    suspend fun move(sources: List<String>, destination: String): Result<Boolean>

    /**
     * Get file details
     */
    suspend fun getFileDetails(path: String): Result<FileItem>

    /**
     * Search files in directory
     */
    fun searchFiles(directory: String, query: String): Flow<List<FileItem>>
}