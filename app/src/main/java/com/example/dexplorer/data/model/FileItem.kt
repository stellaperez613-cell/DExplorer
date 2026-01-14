package com.example.dexplorer.data.model

import android.net.Uri

data class FileItem(
    val name: String,
    val path: String,
    val isDirectory: Boolean,
    val size: Long,
    val lastModified: Long,
    val mimeType: String?,
    val extension: String?,
    val isHidden: Boolean = false,
    val thumbnailUri: Uri? = null,
    val folderPreviewUris: List<Uri> = emptyList(),
    val permissions: FilePermissions? = null
)

data class FilePermissions(
    val canRead: Boolean,
    val canWrite: Boolean,
    val canExecute: Boolean
)