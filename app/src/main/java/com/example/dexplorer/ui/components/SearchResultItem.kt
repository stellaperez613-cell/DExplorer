package com.example.dexplorer.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.dexplorer.data.model.FileItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchResultItem(
    file: FileItem,
    isSelected: Boolean,
    onSelectionToggle: () -> Unit,
    onDoubleClick: () -> Unit,
    onContextMenu: (Offset) -> Unit,
    onCtrlClick: () -> Unit = onSelectionToggle,
    onMoveHere: ((List<String>) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var lastTouchPosition by remember { mutableStateOf(Offset.Zero) }
    var lastClickTime by remember { mutableLongStateOf(0L) }
    var lastPointerType by remember { mutableStateOf(PointerType.Unknown) }
    var isDragHovering by remember { mutableStateOf(false) }
    var isCtrlDown by remember { mutableStateOf(false) }
    val dragSelectState = LocalDragSelectState.current
    DisposableEffect(file.path) {
        onDispose { dragSelectState?.itemRects?.remove(file.path) }
    }

    Row(
        modifier = modifier
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
                            onContextMenu(lastTouchPosition)
                        }
                    }
                }
            }
            .combinedClickable(
                onClick = {
                    val now = System.currentTimeMillis()
                    val delta = now - lastClickTime
                    lastClickTime = now
                    if (delta in 1L..300L) onDoubleClick() else if (isCtrlDown) onCtrlClick() else onSelectionToggle()
                },
                onLongClick = {
                    if (lastPointerType == PointerType.Touch) onContextMenu(lastTouchPosition)
                }
            )
            .padding(horizontal = 16.dp, vertical = 10.dp)
            .onGloballyPositioned { coords ->
                dragSelectState?.updateItemRect(file.path, coords)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Icon
        Box(modifier = Modifier.size(40.dp)) {
            if (file.isDirectory) {
                Icon(
                    painter = painterResource(com.example.dexplorer.R.drawable.ic_folder_yellow),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = androidx.compose.ui.graphics.Color.Unspecified
                )
            } else if (file.thumbnailPath != null) {
                AsyncImage(
                    model = file.thumbnailPath,
                    contentDescription = file.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )
                if (file.defaultAppIcon != null) {
                    Image(
                        bitmap = file.defaultAppIcon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = (-1).dp, y = (-1).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                            .padding(2.dp)
                    )
                }
            } else {
                val iconVector = when {
                    file.mimeType?.startsWith("image/") == true -> Icons.Filled.Image
                    file.mimeType?.startsWith("video/") == true -> Icons.Filled.Movie
                    file.mimeType?.startsWith("audio/") == true -> Icons.Filled.MusicNote
                    file.mimeType == "application/pdf"          -> Icons.Filled.PictureAsPdf
                    else                                        -> Icons.Filled.Description
                }
                Icon(
                    imageVector = iconVector,
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (file.defaultAppIcon != null) {
                    Image(
                        bitmap = file.defaultAppIcon.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = (-1).dp, y = (-1).dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                            .border(0.5.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f), CircleShape)
                            .padding(2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Name + size/date
        Column(modifier = Modifier.weight(0.45f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = if (file.isDirectory) "Folder • ${srFormatDate(file.lastModified)}"
                       else "${srFormatSize(file.size)} • ${srFormatDate(file.lastModified)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // Location
        Text(
            text = srFormatPath(file.path),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(0.55f)
                .padding(start = 8.dp)
        )
    }
}

@Composable
internal fun SearchResultsView(
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
    val currentSelectedItems by rememberUpdatedState(selectedItems)
    Column(modifier = modifier.fillMaxSize()) {
        // Column headers — aligned with item layout (icon 40dp + spacer 12dp = 52dp offset)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                .padding(horizontal = 16.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Spacer(modifier = Modifier.width(52.dp))
            Text(
                text = "Name",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(0.45f)
            )
            Text(
                text = "Location",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .weight(0.55f)
                    .padding(start = 8.dp)
            )
        }
        HorizontalDivider()

        if (files.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No results found", modifier = Modifier.padding(16.dp))
            }
        } else {
            LazyColumn(
                state = rememberLazyListState(),
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(files, key = { it.path }) { file ->
                    val isSelected = file.path in currentSelectedItems
                    SearchResultItemWithMenu(
                        file = file,
                        isSelected = isSelected,
                        onSelectionToggle = { onItemClick(file) },
                        onDoubleClick = {
                            if (file.isDirectory) onItemDoubleClick(file) else onOpen(file)
                        },
                        onOpen = { onOpen(file) },
                        onOpenWith = { onOpenWith(file) },
                        onCopy = {
                            if (file.path !in currentSelectedItems) onItemClick(file)
                            onCopy(file)
                        },
                        onCut = {
                            if (file.path !in currentSelectedItems) onItemClick(file)
                            onCut(file)
                        },
                        onRename = {
                            if (file.path !in currentSelectedItems) onItemClick(file)
                            onRename(file)
                        },
                        onDelete = {
                            if (file.path !in currentSelectedItems) onItemClick(file)
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
private fun SearchResultItemWithMenu(
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
        SearchResultItem(
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

private fun srFormatPath(path: String): String {
    val parent = path.substringBeforeLast("/")
    return parent
        .replace("/storage/emulated/0", "Internal Storage")
        .ifEmpty { "Internal Storage" }
}

private fun srFormatSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> "${bytes / (1024 * 1024 * 1024)} GB"
}

private fun srFormatDate(timestamp: Long): String =
    SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(Date(timestamp))
