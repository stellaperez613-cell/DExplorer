package com.example.dexplorer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.dexplorer.data.model.FileItem
import com.example.dexplorer.data.model.ViewMode

@Composable
fun FileListView(
    files: List<FileItem>,
    selectedItems: Set<String>,
    viewMode: ViewMode,
    isSearching: Boolean = false,
    onItemClick: (FileItem) -> Unit,
    onItemDoubleClick: (FileItem) -> Unit,
    onOpen: (FileItem) -> Unit,
    onOpenWith: (FileItem) -> Unit,
    onCopy: (FileItem) -> Unit,
    onCut: (FileItem) -> Unit,
    onRename: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit,
    onProperties: (FileItem) -> Unit,
    onCompress: (FileItem) -> Unit,
    onExtract: (FileItem) -> Unit,
    onExtractTo: (FileItem) -> Unit,
    onItemCtrlClick: (FileItem) -> Unit = onItemClick,
    onMoveToFolder: ((destFolder: FileItem, sourcePaths: List<String>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (isSearching) {
        SearchResultsView(
            files = files,
            selectedItems = selectedItems,
            onItemClick = onItemClick,
            onItemDoubleClick = onItemDoubleClick,
            onOpen = onOpen,
            onOpenWith = onOpenWith,
            onCopy = onCopy,
            onCut = onCut,
            onRename = onRename,
            onDelete = onDelete,
            onProperties = onProperties,
            onCompress = onCompress,
            onExtract = onExtract,
            onExtractTo = onExtractTo,
            onItemCtrlClick = onItemCtrlClick,
            onMoveToFolder = onMoveToFolder,
            modifier = modifier
        )
        return
    }
    when (viewMode) {
        ViewMode.DETAILS -> {
            DetailsView(
                files = files,
                selectedItems = selectedItems,
                onItemClick = onItemClick,
                onItemDoubleClick = onItemDoubleClick,
                onOpen = onOpen,
                onOpenWith = onOpenWith,
                onCopy = onCopy,
                onCut = onCut,
                onRename = onRename,
                onDelete = onDelete,
                onProperties = onProperties,
                onCompress = onCompress,
                onExtract = onExtract,
                onExtractTo = onExtractTo,
                onItemCtrlClick = onItemCtrlClick,
                onMoveToFolder = onMoveToFolder,
                modifier = modifier
            )
        }

        ViewMode.LIST -> {
            ListViewCompact(
                files = files,
                selectedItems = selectedItems,
                onItemClick = onItemClick,
                onItemDoubleClick = onItemDoubleClick,
                onOpen = onOpen,
                onOpenWith = onOpenWith,
                onCopy = onCopy,
                onCut = onCut,
                onRename = onRename,
                onDelete = onDelete,
                onProperties = onProperties,
                onCompress = onCompress,
                onExtract = onExtract,
                onExtractTo = onExtractTo,
                onItemCtrlClick = onItemCtrlClick,
                onMoveToFolder = onMoveToFolder,
                modifier = modifier
            )
        }

        else -> {
            IconGridView(
                files = files,
                selectedItems = selectedItems,
                viewMode = viewMode,
                onItemClick = onItemClick,
                onItemDoubleClick = onItemDoubleClick,
                onOpen = onOpen,
                onOpenWith = onOpenWith,
                onCopy = onCopy,
                onCut = onCut,
                onRename = onRename,
                onDelete = onDelete,
                onProperties = onProperties,
                onCompress = onCompress,
                onExtract = onExtract,
                onExtractTo = onExtractTo,
                onItemCtrlClick = onItemCtrlClick,
                onMoveToFolder = onMoveToFolder,
                modifier = modifier
            )
        }
    }
}

// List view (compact, no thumbnails, just icon + name)
@Composable
private fun ListViewCompact(
    files: List<FileItem>,
    selectedItems: Set<String>,
    onItemClick: (FileItem) -> Unit,
    onItemDoubleClick: (FileItem) -> Unit,
    onOpen: (FileItem) -> Unit,
    onOpenWith: (FileItem) -> Unit,
    onCopy: (FileItem) -> Unit,
    onCut: (FileItem) -> Unit,
    onRename: (FileItem) -> Unit,
    onDelete: (FileItem) -> Unit,
    onProperties: (FileItem) -> Unit,
    onCompress: (FileItem) -> Unit,
    onExtract: (FileItem) -> Unit,
    onExtractTo: (FileItem) -> Unit,
    onItemCtrlClick: (FileItem) -> Unit = onItemClick,
    onMoveToFolder: ((FileItem, List<String>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val currentSelectedItems by rememberUpdatedState(selectedItems)
    Box(modifier = modifier.fillMaxSize()) {
        if (files.isEmpty()) {
            Text(
                text = "Empty folder",
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(16.dp)
            )
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files, key = { it.path }) { file ->
                    val isSelected = file.path in currentSelectedItems
                    FileItemWithContextMenu(
                        file = file,
                        isSelected = isSelected,
                        onSelectionToggle = { onItemClick(file) },
                        onDoubleClick = {
                            if (file.isDirectory) {
                                onItemDoubleClick(file)
                            } else {
                                onOpen(file)
                            }
                        },
                        onOpen = { onOpen(file) },
                        onOpenWith = { onOpenWith(file) },
                        onCopy = {
                            if (file.path !in currentSelectedItems) {
                                onItemClick(file)
                            }
                            onCopy(file)
                        },
                        onCut = {
                            if (file.path !in currentSelectedItems) {
                                onItemClick(file)
                            }
                            onCut(file)
                        },
                        onRename = {
                            if (file.path !in currentSelectedItems) {
                                onItemClick(file)
                            }
                            onRename(file)
                        },
                        onDelete = {
                            if (file.path !in currentSelectedItems) {
                                onItemClick(file)
                            }
                            onDelete(file)
                        },
                        onProperties = { onProperties(file) },
                        onCompress = { onCompress(file) },
                        onExtract = { onExtract(file) },
                        onExtractTo = { onExtractTo(file) },
                        onCtrlClick = { onItemCtrlClick(file) },
                        onMoveHere = if (file.isDirectory) { sourcePaths -> onMoveToFolder?.invoke(file, sourcePaths) } else null
                    )
                }
            }
        }
    }
}

@Composable
private fun FileItemWithContextMenu(
    file: FileItem,
    isSelected: Boolean,
    onSelectionToggle: () -> Unit,
    onDoubleClick: () -> Unit,
    onOpen: () -> Unit,
    onOpenWith: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onProperties: () -> Unit,
    onCompress: () -> Unit,
    onExtract: () -> Unit,
    onExtractTo: () -> Unit,
    onCtrlClick: () -> Unit = onSelectionToggle,
    onMoveHere: ((List<String>) -> Unit)? = null
) {
    var showContextMenu by remember { mutableStateOf(false) }
    var contextMenuOffsetX by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    Box {
        FileListItem(
            file = file,
            isSelected = isSelected,
            onSelectionToggle = onSelectionToggle,
            onDoubleClick = onDoubleClick,
            onContextMenu = { offset ->
                contextMenuOffsetX = with(density) { offset.x.toDp() }
                showContextMenu = true
            },
            onCtrlClick = onCtrlClick,
            onMoveHere = onMoveHere
        )

        // Context menu
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            offset = DpOffset(x = contextMenuOffsetX, y = 0.dp)
        ) {
            FileContextMenuContent(
                file = file,
                onOpen = onOpen,
                onOpenWith = onOpenWith,
                onCopy = onCopy,
                onCut = onCut,
                onRename = onRename,
                onDelete = onDelete,
                onProperties = onProperties,
                onCompress = onCompress,
                onExtract = onExtract,
                onExtractTo = onExtractTo,
                onDismiss = { showContextMenu = false }
            )
        }
    }
}