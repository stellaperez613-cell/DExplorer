package com.example.dexplorer.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.dexplorer.R
import com.example.dexplorer.data.model.FileItem
import com.example.dexplorer.domain.usecase.FolderStats
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun FilePropertiesDialog(
    file: FileItem,
    folderStats: FolderStats? = null,
    isCalculating: Boolean = false,
    defaultAppName: String? = null,
    onDismiss: () -> Unit,
    onRememberBadge: (() -> Unit)? = null,
    onClearBadge: (() -> Unit)? = null,
    onChangeDefaultApp: (() -> Unit)? = null
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = "Properties",
                style = MaterialTheme.typography.headlineSmall
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Icon or Thumbnail
                if (file.thumbnailPath != null) {
                    AsyncImage(
                        model = file.thumbnailPath,
                        contentDescription = file.name,
                        modifier = Modifier
                            .size(120.dp)
                            .padding(bottom = 16.dp)
                    )
                } else {
                    Icon(
                        painter = painterResource(
                            if (file.isDirectory) {
                                R.drawable.ic_folder_yellow
                            } else {
                                R.drawable.ic_file
                            }
                        ),
                        contentDescription = null,
                        modifier = Modifier
                            .size(120.dp)
                            .padding(bottom = 16.dp),
                        tint = androidx.compose.ui.graphics.Color.Unspecified
                    )
                }

                // File name
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                Divider(modifier = Modifier.padding(vertical = 8.dp))

                // Properties
                // Properties
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Type
                    PropertyRow(
                        label = "Type",
                        value = if (file.isDirectory) "Folder" else (file.mimeType ?: "File")
                    )

                    // SIZE - Different for folders vs files
                    if (file.isDirectory) {
                        if (folderStats != null) {
                            PropertyRow(
                                label = "Size",
                                value = formatFileSize(folderStats.totalSize)
                            )
                            PropertyRow(
                                label = "Contains",
                                value = buildString {
                                    append("${folderStats.fileCount} file${if (folderStats.fileCount != 1) "s" else ""}")
                                    if (folderStats.folderCount > 0) {
                                        append(", ${folderStats.folderCount} folder${if (folderStats.folderCount != 1) "s" else ""}")
                                    }
                                }
                            )
                        } else {
                            PropertyRow(
                                label = "Size",
                                value = if (isCalculating) "Calculating..." else "--"
                            )
                        }
                    } else {
                        // File size
                        PropertyRow(
                            label = "Size",
                            value = formatFileSize(file.size)
                        )
                    }

                    // Location
                    PropertyRow(
                        label = "Location",
                        value = file.path.substringBeforeLast("/")
                    )

                    // Modified
                    PropertyRow(
                        label = "Modified",
                        value = formatDateTime(file.lastModified)
                    )

                    // Permissions
                    PropertyRow(
                        label = "Permissions",
                        value = buildString {
                            append(if (file.permissions?.canRead == true) "Read " else "")
                            append(if (file.permissions?.canWrite == true) "Write " else "")
                            append(if (file.permissions?.canExecute == true) "Execute" else "")
                        }.ifEmpty { "None" }
                    )

                    // DEFAULT APP (only for files) - ONLY ONE SECTION NOW

                    // DEFAULT APP (only for files)
                    if (!file.isDirectory) {
                        Divider(modifier = Modifier.padding(vertical = 8.dp))

                        // Show current system default
                        if (defaultAppName != null) {
                            PropertyRow(
                                label = "Opens with",
                                value = defaultAppName
                            )

                            // Single button: Clear and set default
                            if (onChangeDefaultApp != null) {
                                Button(
                                    onClick = onChangeDefaultApp,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Apps,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Clear and set default app")
                                }
                            }

                        } else {
                            PropertyRow(
                                label = "Opens with",
                                value = "No default set"
                            )

                            // Button to set default
                            if (onChangeDefaultApp != null) {
                                Button(
                                    onClick = onChangeDefaultApp,
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Apps,
                                        contentDescription = null,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Set default app")
                                }
                            }
                        }
                    }
                    Divider(modifier = Modifier.padding(vertical = 8.dp))

                    // Full path
                    PropertyRow(
                        label = "Path",
                        value = file.path
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
private fun PropertyRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = "$label:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(0.4f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(0.6f)
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val kb = bytes / 1024.0
    if (kb < 1024) return "%.2f KB".format(kb)
    val mb = kb / 1024.0
    if (mb < 1024) return "%.2f MB".format(mb)
    val gb = mb / 1024.0
    return "%.2f GB".format(gb)
}

private fun formatDateTime(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}