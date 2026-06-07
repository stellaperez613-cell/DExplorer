package com.example.dexplorer.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.*
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.dexplorer.R
import com.example.dexplorer.data.model.FileItem
import com.example.dexplorer.data.model.ViewMode
import androidx.compose.foundation.border
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.onGloballyPositioned

@Composable
fun IconGridView(
    files: List<FileItem>,
    selectedItems: Set<String>,
    viewMode: ViewMode,
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
    val gridState = rememberLazyGridState()
    val currentSelectedItems by rememberUpdatedState(selectedItems)
    val (iconSize, columns, showThumbnails) = when (viewMode) {
        ViewMode.EXTRA_LARGE_ICONS -> Triple(128.dp, GridCells.Adaptive(140.dp), true)
        ViewMode.LARGE_ICONS -> Triple(96.dp, GridCells.Adaptive(110.dp), true)
        ViewMode.MEDIUM_ICONS -> Triple(64.dp, GridCells.Adaptive(80.dp), true)
        ViewMode.SMALL_ICONS -> Triple(48.dp, GridCells.Adaptive(60.dp), true)
        else -> Triple(48.dp, GridCells.Adaptive(60.dp), false)
    }

    if (files.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Empty folder",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    } else {
        LazyVerticalGrid(
            columns = columns,
            state = gridState,
            modifier = modifier.fillMaxSize(),
            contentPadding = PaddingValues(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(files, key = { it.path }) { file ->
                val isSelected = file.path in currentSelectedItems
                IconGridItem(
                    file = file,
                    isSelected = isSelected,
                    iconSize = iconSize,
                    showThumbnail = showThumbnails,
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
                    onCopy = { onCopy(file) },
                    onCut = { onCut(file) },
                    onRename = { onRename(file) },
                    onDelete = { onDelete(file) },
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

@Composable
private fun IconGridItem(
    file: FileItem,
    isSelected: Boolean,
    iconSize: androidx.compose.ui.unit.Dp,
    showThumbnail: Boolean,
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
    var contextMenuOffset by remember { mutableStateOf(Offset.Zero) }
    var lastTouchPosition by remember { mutableStateOf(Offset.Zero) }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    var lastPointerType by remember { mutableStateOf(PointerType.Unknown) }
    var isDragHovering by remember { mutableStateOf(false) }
    var isCtrlDown by remember { mutableStateOf(false) }
    val density = LocalDensity.current
    val dragSelectState = LocalDragSelectState.current
    DisposableEffect(file.path) {
        onDispose { dragSelectState?.itemRects?.remove(file.path) }
    }

    Box {
        Column(
            modifier = Modifier
                .width(iconSize + 16.dp)
                .clip(MaterialTheme.shapes.small)
                .fileItemDragSource(file)
                .folderDropTarget(
                    enabled = file.isDirectory,
                    tag = file.name,
                    onHoverChange = { isDragHovering = it },
                    onDrop = { sourcePaths ->
                        val toMove = sourcePaths.filter { it != file.path }
                        if (toMove.isNotEmpty()) onMoveHere?.invoke(toMove)
                    }
                )
                .background(
                    when {
                        isDragHovering -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                        isSelected -> MaterialTheme.colorScheme.primaryContainer
                        else -> MaterialTheme.colorScheme.surface
                    }
                )
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.changes.isNotEmpty()) {
                                lastTouchPosition = event.changes.first().position
                                lastPointerType = event.changes.first().type
                            }
                            isCtrlDown = event.keyboardModifiers.isCtrlPressed
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                event.changes.forEach { it.consume() }
                                contextMenuOffset = lastTouchPosition
                                onSelectionToggle()
                                showContextMenu = true
                            }
                        }
                    }
                }
                .combinedClickable(
                    onClick = {
                        val now = System.currentTimeMillis()
                        val delta = now - lastClickTime
                        lastClickTime = now
                        if (delta in 1L..300L) {
                            onDoubleClick()
                        } else if (isCtrlDown) {
                            onCtrlClick()
                        } else {
                            onSelectionToggle()
                        }
                    },
                    onLongClick = {
                        if (lastPointerType == PointerType.Touch) {
                            contextMenuOffset = lastTouchPosition
                            onSelectionToggle()
                            showContextMenu = true
                        }
                    }
                )
                .padding(8.dp)
                .onGloballyPositioned { coords ->
                    dragSelectState?.updateItemRect(file.path, coords)
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Icon or Thumbnail

            Box(modifier = Modifier.size(iconSize)) {
                if (showThumbnail && !file.isDirectory && file.thumbnailPath != null) {
                    // Show thumbnail for images/videos/music
                    AsyncImage(
                        model = file.thumbnailPath,
                        contentDescription = file.name,
                        modifier = Modifier
                            .size(iconSize)
                            .clip(MaterialTheme.shapes.small),
                        contentScale = ContentScale.Crop,
                        error = painterResource(getFileIconResource(file))
                    )
                } else {
                    // Show icon
                    Icon(
                        painter = painterResource(getFileIconResource(file)),
                        contentDescription = file.name,
                        modifier = Modifier.size(iconSize),
                        tint = if (file.isDirectory) {
                            androidx.compose.ui.graphics.Color.Unspecified
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }

                // App badge overlay (bottom-right corner)
                if (!file.isDirectory && file.defaultAppIcon != null) {
                    // Calculate badge size
                    val badgeSize = when {
                        iconSize >= 96.dp -> iconSize * 0.30f  // Extra Large/Large: 30% (~38dp/28dp)
                        iconSize >= 64.dp -> 28.dp              // Medium: 28dp
                        else -> 20.dp                           // Small: 24dp
                    }

                    Image(
                        bitmap = file.defaultAppIcon.asImageBitmap(),
                        contentDescription = "Default app",
                        modifier = Modifier
                            .size(badgeSize)
                            .align(Alignment.BottomEnd)
                            .offset(x = (-2).dp, y = (-2).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                            .padding(2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // File name
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        // Context menu
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            offset = DpOffset(
                x = with(density) { contextMenuOffset.x.toDp() },
                y = 0.dp
            )
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

private fun getFileIconResource(file: FileItem): Int {
    return if (file.isDirectory) {
        com.example.dexplorer.R.drawable.ic_folder_yellow
    } else {
        when {
            file.mimeType?.startsWith("image/") == true -> com.example.dexplorer.R.drawable.ic_image
            file.mimeType?.startsWith("video/") == true -> com.example.dexplorer.R.drawable.ic_video
            file.mimeType?.startsWith("audio/") == true -> com.example.dexplorer.R.drawable.ic_audio
            file.mimeType == "application/pdf" -> com.example.dexplorer.R.drawable.ic_pdf
            file.mimeType?.contains("zip") == true -> com.example.dexplorer.R.drawable.ic_archive
            else -> com.example.dexplorer.R.drawable.ic_file
        }
    }
}