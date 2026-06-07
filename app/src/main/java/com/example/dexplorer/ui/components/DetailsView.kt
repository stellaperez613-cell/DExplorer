package com.example.dexplorer.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.dexplorer.R
import com.example.dexplorer.data.model.FileItem
import java.text.SimpleDateFormat
import java.util.*
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.layout.onGloballyPositioned


@Composable
fun DetailsView(
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
    onMoveToFolder: ((destFolder: FileItem, sourcePaths: List<String>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val currentSelectedItems by rememberUpdatedState(selectedItems)
    Column(modifier = modifier.fillMaxSize()) {
        // Header row
        DetailsHeader()

        HorizontalDivider()

        // File list
        if (files.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Empty folder",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files, key = { it.path }) { file ->
                    val isSelected = file.path in currentSelectedItems
                    DetailsItem(
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
}

@Composable
private fun DetailsHeader() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Name column (50%)
        Text(
            text = "Name",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.5f)
        )

        Text(
            text = " | ",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        // Date Modified column (20%)
        Text(
            text = "Date modified",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.2f)
        )

        Text(
            text = " | ",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        // Type column (15%)
        Text(
            text = "Type",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.15f)
        )

        Text(
            text = " | ",
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        // Size column (15%)
        Text(
            text = "Size",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(0.15f)
        )
    }
}

@Composable
private fun DetailsItem(
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
        Row(
            modifier = Modifier
                .fillMaxWidth()
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
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .onGloballyPositioned { coords ->
                    dragSelectState?.updateItemRect(file.path, coords)
                },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon + Name (50%)

            // Icon + Name (50%)
            Row(
                modifier = Modifier.weight(0.5f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Icon with badge
                Box(modifier = Modifier.size(24.dp)) {
                    if (file.isDirectory) {
                        // Yellow folder
                        Icon(
                            painter = painterResource(getFileIconResource(file)),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            tint = androidx.compose.ui.graphics.Color.Unspecified
                        )
                    } else if (file.thumbnailPath != null) {
                        // Show thumbnail (album art, PDF preview, or file type placeholder)
                        AsyncImage(
                            model = file.thumbnailPath,
                            contentDescription = file.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Fit
                        )

                        // App badge (smaller in details view)
                        if (file.defaultAppIcon != null) {
                            Image(
                                bitmap = file.defaultAppIcon.asImageBitmap(),
                                contentDescription = "Default app",
                                modifier = Modifier
                                    .size(14.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-1).dp, y = (-1).dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                    .padding(1.dp)
                            )
                        }
                    } else {
                        // Fallback Material icon
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            tint = MaterialTheme.colorScheme.onSurface
                        )

                        // App badge
                        if (file.defaultAppIcon != null) {
                            Image(
                                bitmap = file.defaultAppIcon.asImageBitmap(),
                                contentDescription = "Default app",
                                modifier = Modifier
                                    .size(14.dp)
                                    .align(Alignment.BottomEnd)
                                    .offset(x = (-1).dp, y = (-1).dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surface)
                                    .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                                    .padding(1.dp)
                            )
                        }
                    }
                }

                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Date Modified (20%)
            Text(
                text = formatDate(file.lastModified),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(0.2f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = " | ",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )

            // Type (15%)
            Text(
                text = getFileType(file),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(0.15f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = " | ",
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )

            // Size (15%)
            Text(
                text = if (file.isDirectory) "--" else formatFileSize(file.size),
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.weight(0.15f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
        R.drawable.ic_folder_yellow
    } else {
        when {
            file.mimeType?.startsWith("image/") == true -> R.drawable.ic_image
            file.mimeType?.startsWith("video/") == true -> R.drawable.ic_video
            file.mimeType?.startsWith("audio/") == true -> R.drawable.ic_audio
            file.mimeType == "application/pdf" -> R.drawable.ic_pdf
            file.mimeType?.contains("zip") == true -> R.drawable.ic_archive
            else -> R.drawable.ic_file
        }
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getFileType(file: FileItem): String {
    return if (file.isDirectory) {
        "Folder"
    } else {
        file.extension?.uppercase()?.ifEmpty { "File" } ?: "File"
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.1f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.1f MB".format(mb)
    val gb = mb / 1024.0
    return "%.1f GB".format(gb)
}