package com.example.dexplorer.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import com.example.dexplorer.data.model.StorageVolumeInfo

@Composable
fun EjectConfirmationDialog(
    volume: StorageVolumeInfo,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Eject ${volume.name}") },
        text = { Text("The notification panel will open. Tap \"${volume.name}\" to safely remove it.") },
        confirmButton = { Button(onClick = onConfirm) { Text("Open Notifications") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
