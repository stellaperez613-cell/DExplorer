package com.example.dexplorer.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TextSnippet
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpOffset

@Composable
fun EmptySpaceContextMenu(
    expanded: Boolean,
    offset: DpOffset,
    hasPasteItems: Boolean,
    isCurrentFolderPinned: Boolean,
    isCurrentFolderProtected: Boolean,
    onDismiss: () -> Unit,
    onNewFolder: () -> Unit,
    onNewFile: (defaultName: String) -> Unit,
    onSelectAll: () -> Unit,
    onPaste: () -> Unit,
    onRefresh: () -> Unit,
    onTogglePinCurrentFolder: () -> Unit
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss,
        offset = offset
    ) {
        DropdownMenuItem(
            text = { Text("New Folder") },
            onClick = { onNewFolder(); onDismiss() },
            leadingIcon = { Icon(Icons.Filled.CreateNewFolder, null) }
        )

        HorizontalDivider()

        DropdownMenuItem(
            text = { Text("New Text Document") },
            onClick = { onNewFile("New Text Document.txt"); onDismiss() },
            leadingIcon = { Icon(Icons.Filled.TextSnippet, null) }
        )
        DropdownMenuItem(
            text = { Text("New Word Document") },
            onClick = { onNewFile("New Document.docx"); onDismiss() },
            leadingIcon = { Icon(Icons.Filled.Description, null) }
        )
        DropdownMenuItem(
            text = { Text("New Excel Spreadsheet") },
            onClick = { onNewFile("New Spreadsheet.xlsx"); onDismiss() },
            leadingIcon = { Icon(Icons.Filled.TableChart, null) }
        )
        DropdownMenuItem(
            text = { Text("New PowerPoint") },
            onClick = { onNewFile("New Presentation.pptx"); onDismiss() },
            leadingIcon = { Icon(Icons.Filled.Slideshow, null) }
        )

        HorizontalDivider()

        DropdownMenuItem(
            text = { Text("Select All") },
            onClick = { onSelectAll(); onDismiss() },
            leadingIcon = { Icon(Icons.Filled.SelectAll, null) }
        )

        if (hasPasteItems) {
            DropdownMenuItem(
                text = { Text("Paste") },
                onClick = { onPaste(); onDismiss() },
                leadingIcon = { Icon(Icons.Filled.ContentPaste, null) }
            )
        }

        if (!isCurrentFolderProtected) {
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text(if (isCurrentFolderPinned) "Remove from Quick Access" else "Add to Quick Access") },
                onClick = { onTogglePinCurrentFolder(); onDismiss() },
                leadingIcon = { Icon(Icons.Filled.PushPin, null) }
            )
        }

        HorizontalDivider()

        DropdownMenuItem(
            text = { Text("Refresh") },
            onClick = { onRefresh(); onDismiss() },
            leadingIcon = { Icon(Icons.Filled.Refresh, null) }
        )
    }
}
