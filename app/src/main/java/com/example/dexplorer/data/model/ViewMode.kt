package com.example.dexplorer.data.model

enum class ViewMode {
    LIST,
    GRID_SMALL,
    GRID_MEDIUM,
    GRID_LARGE
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