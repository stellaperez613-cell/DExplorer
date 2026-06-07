package com.example.dexplorer.data.model

enum class FormatType(val label: String, val description: String) {
    EXFAT(
        "exFAT",
        "Best cross-platform: Windows, macOS, Linux. No file size limit. Recommended."
    ),
    FAT32(
        "FAT32",
        "Universal compatibility including older devices. Max 4 GB per file."
    ),
    NTFS(
        "NTFS",
        "Windows-native. Linux supports read/write natively. macOS needs third-party tools."
    ),
    EXT4(
        "ext4",
        "Linux-native. Windows requires additional software to read."
    )
}
