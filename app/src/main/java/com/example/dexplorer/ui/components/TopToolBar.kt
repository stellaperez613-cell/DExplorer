package com.example.dexplorer.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileRenameOutline
import androidx.compose.material.icons.filled.ViewModule
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dexplorer.data.model.ViewMode
import androidx.compose.material.icons.filled.Check
@Composable
fun TopToolbar(
    hasSelection: Boolean,
    hasSingleSelection: Boolean,
    hasClipboard: Boolean,
    selectedCount: Int,
    currentViewMode: ViewMode,
    onCopy: () -> Unit,
    onCut: () -> Unit,
    onPaste: () -> Unit,
    onDelete: () -> Unit,
    onRename: () -> Unit,
    onNewFolder: () -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    modifier: Modifier = Modifier
) {
    var showViewMenu by remember { mutableStateOf(false) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Top divider line
        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left side - File operation buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Copy
                IconButton(
                    onClick = onCopy,
                    enabled = hasSelection
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = "Copy",
                        tint = if (hasSelection) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }

                // Cut
                IconButton(
                    onClick = onCut,
                    enabled = hasSelection
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCut,
                        contentDescription = "Cut",
                        tint = if (hasSelection) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }

                // Paste
                IconButton(
                    onClick = onPaste,
                    enabled = hasClipboard
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentPaste,
                        contentDescription = "Paste",
                        tint = if (hasClipboard) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }

                // New Folder
                IconButton(onClick = onNewFolder) {
                    Icon(
                        imageVector = Icons.Filled.CreateNewFolder,
                        contentDescription = "New Folder",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }

                // Rename (only enabled for single selection)
                IconButton(
                    onClick = onRename,
                    enabled = hasSingleSelection
                ) {
                    Icon(
                        imageVector = Icons.Filled.DriveFileRenameOutline,
                        contentDescription = "Rename",
                        tint = if (hasSingleSelection) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }

                // Delete
                IconButton(
                    onClick = onDelete,
                    enabled = hasSelection
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = "Delete",
                        tint = if (hasSelection) {
                            MaterialTheme.colorScheme.error
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        }
                    )
                }
            }

            // Right side - View mode and selection count
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selection count
                if (hasSelection) {
                    Text(
                        text = "$selectedCount selected",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                // View mode selector
                Box {
                    IconButton(onClick = { showViewMenu = true }) {
                        Icon(
                            imageVector = Icons.Filled.ViewModule,
                            contentDescription = "Change view mode",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    DropdownMenu(
                        expanded = showViewMenu,
                        onDismissRequest = { showViewMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Extra large icons") },
                            onClick = {
                                onViewModeChange(ViewMode.EXTRA_LARGE_ICONS)
                                showViewMenu = false
                            },
                            leadingIcon = if (currentViewMode == ViewMode.EXTRA_LARGE_ICONS) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )

                        DropdownMenuItem(
                            text = { Text("Large icons") },
                            onClick = {
                                onViewModeChange(ViewMode.LARGE_ICONS)
                                showViewMenu = false
                            },
                            leadingIcon = if (currentViewMode == ViewMode.LARGE_ICONS) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )

                        DropdownMenuItem(
                            text = { Text("Medium icons") },
                            onClick = {
                                onViewModeChange(ViewMode.MEDIUM_ICONS)
                                showViewMenu = false
                            },
                            leadingIcon = if (currentViewMode == ViewMode.MEDIUM_ICONS) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )

                        DropdownMenuItem(
                            text = { Text("Small icons") },
                            onClick = {
                                onViewModeChange(ViewMode.SMALL_ICONS)
                                showViewMenu = false
                            },
                            leadingIcon = if (currentViewMode == ViewMode.SMALL_ICONS) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )

                        DropdownMenuItem(
                            text = { Text("List") },
                            onClick = {
                                onViewModeChange(ViewMode.LIST)
                                showViewMenu = false
                            },
                            leadingIcon = if (currentViewMode == ViewMode.LIST) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )

                        DropdownMenuItem(
                            text = { Text("Details") },
                            onClick = {
                                onViewModeChange(ViewMode.DETAILS)
                                showViewMenu = false
                            },
                            leadingIcon = if (currentViewMode == ViewMode.DETAILS) {
                                { Icon(Icons.Default.Check, contentDescription = null) }
                            } else null
                        )
                    }
                }
            }
        }

        // Bottom divider line
        HorizontalDivider()
    }
}