package com.example.dexplorer.data.model

enum class ViewMode {
    EXTRA_LARGE_ICONS,
    LARGE_ICONS,
    MEDIUM_ICONS,
    SMALL_ICONS,
    LIST,
    DETAILS
}

enum class SortOption {
    NAME_ASC,
    NAME_DESC,
    DATE_MODIFIED_DESC,
    DATE_MODIFIED_ASC,
    SIZE_DESC,
    SIZE_ASC,
    TYPE_ASC
}

data class ClipboardState(
    val items: List<FileItem> = emptyList(),
    val operation: ClipboardOperation = ClipboardOperation.NONE
)

enum class ClipboardOperation {
    NONE,
    COPY,
    CUT
}

data class QuickAccessFolder(
    val name: String,
    val path: String,
    val isPinned: Boolean = true
)