package com.example.dexplorer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun ExtractToDialog(
    archivePath: String,
    currentPath: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val defaultPath = "$currentPath/${File(archivePath).nameWithoutExtension}"
    var destinationPath by remember { mutableStateOf(defaultPath) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Extract to…") },
        text = {
            Column {
                Text("Destination folder path:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = destinationPath,
                    onValueChange = { destinationPath = it },
                    label = { Text("Path") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(destinationPath.trim()) },
                enabled = destinationPath.isNotBlank()
            ) {
                Text("Extract")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
