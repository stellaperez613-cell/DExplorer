package com.example.dexplorer.data.model

import android.os.Environment

data class StorageVolumeInfo(
    val id: String,
    val path: String,
    val name: String,
    val totalBytes: Long,
    val freeBytes: Long,
    val state: String,
    val isRemovable: Boolean
) {
    val usedBytes: Long get() = totalBytes - freeBytes
    val isMounted: Boolean get() = state == Environment.MEDIA_MOUNTED || state == Environment.MEDIA_MOUNTED_READ_ONLY
    val isReadOnly: Boolean get() = state == Environment.MEDIA_MOUNTED_READ_ONLY
}
