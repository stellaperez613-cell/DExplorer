package com.example.dexplorer.ui.components

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.dexplorer.data.model.FileItem

@Composable
fun FileListView(
    files: List<FileItem>,
    selectedItems: Set<String>,
    onItemClick: (FileItem) -> Unit,
    onItemDoubleClick: (FileItem) -> Unit,
    modifier: Modifier = Modifier
) {
    if (files.isEmpty()) {
        Text(
            text = "Empty folder",
            modifier = Modifier.padding(16.dp)
        )
    } else {
        LazyColumn(
            modifier = modifier.fillMaxSize()
        ) {
            items(files, key = { it.path }) { file ->
                FileListItem(
                    file = file,
                    isSelected = file.path in selectedItems,
                    onSelectionToggle = { onItemClick(file) },
                    onDoubleClick = { onItemDoubleClick(file) }
                )
            }
        }
    }
}