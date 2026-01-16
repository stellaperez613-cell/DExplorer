package com.example.dexplorer.domain.usecase

import com.example.dexplorer.data.model.FileItem
import com.example.dexplorer.domain.repository.FileSystemRepository
import javax.inject.Inject

class GetDirectoryContentsUseCase @Inject constructor(
    private val fileSystemRepository: FileSystemRepository
) {
    suspend operator fun invoke(path: String): Result<List<FileItem>> {
        return fileSystemRepository.getDirectoryContents(path)
    }
}