package com.example.dexplorer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dexplorer.data.model.FileItem


@Composable
fun FileContextMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    file: FileItem?,

    isSelected: Boolean,
    onOpen: () -> Unit,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    onProperties: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = onDismiss,
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            tonalElevation = 3.dp
        ) {
            // Open (only for folders)
            if (file?.isDirectory == true) {
                DropdownMenuItem(
                    text = { Text("Open") },
                    onClick = {
                        onOpen()
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.FolderOpen, contentDescription = null)
                    }
                )
            }

            // Copy
            DropdownMenuItem(
                text = { Text("Copy") },
                onClick = {
                    onCopy()
                    onDismiss()
                },
                leadingIcon = {
                    Icon(Icons.Default.ContentCopy, contentDescription = null)
                }
            )

            // Cut
            DropdownMenuItem(
                text = { Text("Cut") },
                onClick = {
                    onCut()
                    onDismiss()
                },
                leadingIcon = {
                    Icon(Icons.Default.ContentCut, contentDescription = null)
                }
            )

            // Rename (only if single item)
            if (!isSelected || isSelected) {
                DropdownMenuItem(
                    text = { Text("Rename") },
                    onClick = {
                        onRename()
                        onDismiss()
                    },
                    leadingIcon = {
                        Icon(Icons.Default.DriveFileRenameOutline, contentDescription = null)
                    }
                )
            }

            // Delete
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    onDelete()
                    onDismiss()
                },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null)
                }
            )

            // Properties
            DropdownMenuItem(
                text = { Text("Properties") },
                onClick = {
                    onProperties()
                    onDismiss()
                },
                leadingIcon = {
                    Icon(Icons.Default.Info, contentDescription = null)
                }
            )

        }
    }
}
@Composable
fun FileContextMenuContent(
    file: FileItem?,
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
    onDismiss: () -> Unit
) {
    // FILES: Open and Open With
    if (file?.isDirectory == false) {  // FIX #1: Safe call
        DropdownMenuItem(
            text = { Text("Open") },
            onClick = {
                onOpen()
                onDismiss()  // Now works - we have the parameter
            },
            leadingIcon = {
                Icon(Icons.AutoMirrored.Filled.Launch, contentDescription = null)  // FIX #3: Material 3 icon
            }
        )

        DropdownMenuItem(
            text = { Text("Open with...") },
            onClick = {
                onOpenWith()
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Filled.Apps, contentDescription = null)
            }
        )

        HorizontalDivider()
    }

    // FOLDERS: Open to navigate + Quick Access pin
    if (file?.isDirectory == true) {
        DropdownMenuItem(
            text = { Text("Open") },
            onClick = {
                onOpen()
                onDismiss()
            },
            leadingIcon = {
                Icon(Icons.Filled.FolderOpen, contentDescription = null)
            }
        )

        val pinHandlers = LocalPinHandlers.current
        if (pinHandlers != null && !pinHandlers.isProtected(file.path)) {
            val pinned by rememberUpdatedState(pinHandlers.isPinned(file.path))
            DropdownMenuItem(
                text = { Text(if (pinned) "Remove from Quick Access" else "Add to Quick Access") },
                onClick = { pinHandlers.onToggle(file.path); onDismiss() },
                leadingIcon = { Icon(Icons.Filled.PushPin, contentDescription = null) }
            )
        }

        HorizontalDivider()
    }

    // Copy
    DropdownMenuItem(
        text = { Text("Copy") },
        onClick = {
            onCopy()
            onDismiss()
        },
        leadingIcon = {
            Icon(Icons.Filled.ContentCopy, contentDescription = null)
        }
    )

    // Cut
    DropdownMenuItem(
        text = { Text("Cut") },
        onClick = {
            onCut()
            onDismiss()
        },
        leadingIcon = {
            Icon(Icons.Filled.ContentCut, contentDescription = null)
        }
    )

    // Rename
    DropdownMenuItem(
        text = { Text("Rename") },
        onClick = {
            onRename()
            onDismiss()
        },
        leadingIcon = {
            Icon(Icons.Filled.DriveFileRenameOutline, contentDescription = null)
        }
    )

    // Delete
    DropdownMenuItem(
        text = { Text("Delete") },
        onClick = {
            onDelete()
            onDismiss()
        },
        leadingIcon = {
            Icon(Icons.Filled.Delete, contentDescription = null)
        }
    )

    HorizontalDivider()

    // Compress to ZIP
    DropdownMenuItem(
        text = { Text("Compress to ZIP") },
        onClick = { onCompress(); onDismiss() },
        leadingIcon = { Icon(Icons.Filled.Archive, contentDescription = null) }
    )

    // Extract here (only for archives)
    val isArchive = file?.extension?.lowercase()?.let {
        it == "zip" || it == "rar" || it == "7z"
    } == true
    if (isArchive) {
        DropdownMenuItem(
            text = { Text("Extract here") },
            onClick = { onExtract(); onDismiss() },
            leadingIcon = { Icon(Icons.Filled.Unarchive, contentDescription = null) }
        )
        DropdownMenuItem(
            text = { Text("Extract to…") },
            onClick = { onExtractTo(); onDismiss() },
            leadingIcon = { Icon(Icons.Filled.Unarchive, contentDescription = null) }
        )
    }

    HorizontalDivider()

    // Properties
    DropdownMenuItem(
        text = { Text("Properties") },
        onClick = {
            onProperties()
            onDismiss()
        },
        leadingIcon = {
            Icon(Icons.Filled.Info, contentDescription = null)
        }
    )
}