package com.example.dexplorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.example.dexplorer.ui.screen.ConflictResolution
import com.example.dexplorer.ui.screen.FileConflict
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ConflictResolutionDialog(
    conflict: FileConflict,
    conflictIndex: Int,
    totalConflicts: Int,
    onReplace: (applyToAll: Boolean) -> Unit,
    onKeepBoth: (applyToAll: Boolean) -> Unit,
    onSkip: (applyToAll: Boolean) -> Unit,
    onCancel: () -> Unit
) {
    var applyToAll by remember(conflictIndex) { mutableStateOf(false) }

    Dialog(onDismissRequest = onCancel) {
        Surface(
            shape = MaterialTheme.shapes.large,
            tonalElevation = 6.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Title
                Text(
                    text = if (totalConflicts > 1) "Name Conflict ($conflictIndex of $totalConflicts)"
                           else "Name Conflict",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "\"${conflict.name}\" already exists at this location.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))

                // File comparison row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    FileInfoCard(
                        label = "Incoming",
                        size = conflict.sourceSize,
                        modified = conflict.sourceModified,
                        isDirectory = conflict.isDirectory,
                        modifier = Modifier.weight(1f),
                        isHighlighted = false
                    )
                    FileInfoCard(
                        label = "Existing",
                        size = conflict.existingSize,
                        modified = conflict.existingModified,
                        isDirectory = conflict.isDirectory,
                        modifier = Modifier.weight(1f),
                        isHighlighted = true
                    )
                }

                if (totalConflicts > 1) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Checkbox(
                            checked = applyToAll,
                            onCheckedChange = { applyToAll = it }
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Apply to all ${totalConflicts} conflicts",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onCancel) {
                        Text("Cancel")
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    TextButton(onClick = { onSkip(applyToAll) }) {
                        Text("Skip")
                    }
                    OutlinedButton(onClick = { onKeepBoth(applyToAll) }) {
                        Text("Keep Both")
                    }
                    Button(
                        onClick = { onReplace(applyToAll) },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Replace")
                    }
                }
            }
        }
    }
}

@Composable
private fun FileInfoCard(
    label: String,
    size: Long,
    modified: Long,
    isDirectory: Boolean,
    modifier: Modifier = Modifier,
    isHighlighted: Boolean
) {
    val containerColor = if (isHighlighted)
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.4f)
    else
        MaterialTheme.colorScheme.surfaceContainerHigh

    Column(
        modifier = modifier
            .background(containerColor, MaterialTheme.shapes.small)
            .padding(12.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontWeight = FontWeight.Medium
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isDirectory) "Folder" else formatConflictSize(size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = formatConflictDate(modified),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

private fun formatConflictSize(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "%.1f KB".format(bytes / 1024.0)
    bytes < 1024L * 1024 * 1024 -> "%.1f MB".format(bytes / (1024.0 * 1024))
    else -> "%.2f GB".format(bytes / (1024.0 * 1024 * 1024))
}

private fun formatConflictDate(timestamp: Long): String =
    SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date(timestamp))
