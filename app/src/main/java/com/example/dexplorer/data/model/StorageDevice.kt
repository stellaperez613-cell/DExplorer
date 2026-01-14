package com.example.dexplorer.data.model

enum class StorageType {
    INTERNAL,
    SD_CARD,
    USB_DRIVE,
    OPTICAL_DRIVE
}

data class StorageDevice(
    val name: String,
    val path: String,
    val totalBytes: Long,
    val availableBytes: Long,
    val type: StorageType,
    val isRemovable: Boolean,
    val isPrimary: Boolean = false
) {
    val usedBytes: Long
        get() = totalBytes - availableBytes

    val usedPercentage: Float
        get() = if (totalBytes > 0) (usedBytes.toFloat() / totalBytes.toFloat()) * 100f else 0f
}