package com.example.dexplorer.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isCtrlPressed
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.dexplorer.core.util.FileLogger
import com.example.dexplorer.data.model.FileItem
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FileListItem(
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
            .padding(horizontal = 16.dp, vertical = 12.dp)
            .onGloballyPositioned { coords ->
                dragSelectState?.updateItemRect(file.path, coords)
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // File/Folder Icon with badge
        Box(modifier = Modifier.size(40.dp)) {
            if (file.isDirectory) {
                // Yellow folder
                Icon(
                    painter = painterResource(com.example.dexplorer.R.drawable.ic_folder_yellow),
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

                // App badge overlay
                if (file.defaultAppIcon != null) {
                    Image(
                        bitmap = file.defaultAppIcon.asImageBitmap(),
                        contentDescription = "Default app",
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
                // Fallback icon
                Icon(
                    imageVector = getFileIcon(file),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // App badge overlay
                if (file.defaultAppIcon != null) {
                    Image(
                        bitmap = file.defaultAppIcon.asImageBitmap(),
                        contentDescription = "Default app",
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

        Spacer(modifier = Modifier.width(16.dp))

        // File Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (!file.isDirectory) {
                Text(
                    text = "${formatFileSize(file.size)} • ${formatDate(file.lastModified)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    text = "Folder • ${formatDate(file.lastModified)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// Return ImageVector (for files only, not folders)
private fun getFileIcon(file: FileItem) = when {
    file.mimeType?.startsWith("image/") == true -> Icons.Filled.Image
    file.mimeType?.startsWith("video/") == true -> Icons.Filled.Movie
    file.mimeType?.startsWith("audio/") == true -> Icons.Filled.MusicNote
    file.mimeType == "application/pdf" -> Icons.Filled.PictureAsPdf
    else -> Icons.Filled.Description
}
private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> "${bytes / 1024} KB"
        bytes < 1024 * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
        else -> "${bytes / (1024 * 1024 * 1024)} GB"
    }
}

private fun formatDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    return sdf.format(Date(timestamp))
}