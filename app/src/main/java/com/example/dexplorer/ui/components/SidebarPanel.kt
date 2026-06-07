package com.example.dexplorer.ui.components

import android.os.Environment
import android.text.format.Formatter
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Eject
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.SdCard
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import com.example.dexplorer.data.model.StorageVolumeInfo
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.dexplorer.data.model.QuickAccessFolder

internal fun quickAccessIcon(path: String): ImageVector = when {
    path == Environment.getExternalStorageDirectory().absolutePath -> Icons.Default.Storage
    path.endsWith("Downloads") -> Icons.Default.FileDownload
    path.endsWith("Documents") -> Icons.Default.Description
    path.endsWith("Pictures")  -> Icons.Default.Image
    path.endsWith("Music")     -> Icons.Default.MusicNote
    path.endsWith("Movies")    -> Icons.Default.Movie
    path.endsWith("DCIM")      -> Icons.Default.CameraAlt
    else                       -> Icons.Default.Folder
}

internal fun storageVolumeIcon(name: String): ImageVector = when {
    name.contains("SD", ignoreCase = true) ||
    name.contains("card", ignoreCase = true) ||
    name.contains("micro", ignoreCase = true) -> Icons.Filled.SdCard
    else -> Icons.Filled.Usb
}

@Composable
fun SidebarPanel(
    currentPath: String,
    pinnedFolders: List<QuickAccessFolder>,
    storageVolumes: List<StorageVolumeInfo> = emptyList(),
    onNavigate: (String) -> Unit,
    onUnpin: (String) -> Unit,
    isProtected: (String) -> Boolean,
    onVolumeClick: (StorageVolumeInfo) -> Unit = {},
    onEjectVolume: (StorageVolumeInfo) -> Unit = {},
    onFormatVolume: (StorageVolumeInfo) -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(220.dp)
            .fillMaxHeight()
            .verticalScroll(rememberScrollState())
            .padding(bottom = 8.dp)
    ) {
        HorizontalDivider()
        Text(
            text = "Quick Access",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        pinnedFolders.forEach { folder ->
            SidebarQuickAccessItem(
                folder = folder,
                isSelected = currentPath == folder.path,
                isProtected = isProtected(folder.path),
                onClick = { onNavigate(folder.path) },
                onUnpin = { onUnpin(folder.path) }
            )
        }
        if (storageVolumes.isNotEmpty()) {
            HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
            Text(
                text = "Devices",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            storageVolumes.forEach { volume ->
                SidebarDeviceItem(
                    volume = volume,
                    isSelected = currentPath == volume.path,
                    onClick = { onVolumeClick(volume) },
                    onEject = { onEjectVolume(volume) },
                    onFormat = { onFormatVolume(volume) }
                )
            }
        }
    }
}

@Composable
private fun SidebarQuickAccessItem(
    folder: QuickAccessFolder,
    isSelected: Boolean,
    isProtected: Boolean,
    onClick: () -> Unit,
    onUnpin: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        NavigationDrawerItem(
            icon = { Icon(quickAccessIcon(folder.path), contentDescription = null) },
            label = { Text(folder.name) },
            badge = {
                Icon(
                    Icons.Filled.PushPin,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            selected = isSelected,
            onClick = onClick,
            modifier = Modifier
                .pointerInput(folder.path) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                event.changes.forEach { it.consume() }
                                showMenu = true
                            }
                        }
                    }
                }
                .pointerInput(folder.path + "_lp") {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (down.type != PointerType.Touch) return@awaitEachGesture
                        val timedOut = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                if (event.changes.any { !it.pressed && it.previousPressed }) break
                            }
                        } == null
                        if (timedOut) {
                            showMenu = true
                            var anyPressed = true
                            while (anyPressed) {
                                val e = awaitPointerEvent(PointerEventPass.Initial)
                                e.changes.forEach { it.consume() }
                                anyPressed = e.changes.any { it.pressed }
                            }
                        }
                    }
                }
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Open") },
                onClick = { onClick(); showMenu = false },
                leadingIcon = { Icon(Icons.Filled.FolderOpen, contentDescription = null) }
            )
            HorizontalDivider()
            DropdownMenuItem(
                text = { Text("Remove from Quick Access") },
                onClick = { if (!isProtected) { onUnpin(); showMenu = false } },
                enabled = !isProtected,
                leadingIcon = {
                    Icon(
                        Icons.Filled.PushPin,
                        contentDescription = null,
                        tint = if (isProtected)
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        else
                            MaterialTheme.colorScheme.error
                    )
                }
            )
        }
    }
}

@Composable
private fun SidebarDeviceItem(
    volume: StorageVolumeInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEject: () -> Unit,
    onFormat: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp)
    ) {
        NavigationDrawerItem(
            icon = { Icon(storageVolumeIcon(volume.name), contentDescription = null) },
            label = {
                Column {
                    Text(volume.name)
                    if (volume.isMounted && volume.totalBytes > 0) {
                        val freeText = "${Formatter.formatShortFileSize(context, volume.freeBytes)} free"
                        Text(
                            if (volume.isReadOnly) "$freeText · read-only" else freeText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (volume.isMounted && volume.isReadOnly) {
                        Text(
                            "Read-only",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (!volume.isMounted) {
                        Text(
                            "Not mounted",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            },
            selected = isSelected,
            onClick = { if (volume.isMounted) onClick() },
            modifier = Modifier
                .pointerInput(volume.path) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent()
                            if (event.type == PointerEventType.Press && event.buttons.isSecondaryPressed) {
                                event.changes.forEach { it.consume() }
                                showMenu = true
                            }
                        }
                    }
                }
                .pointerInput(volume.path + "_lp") {
                    awaitEachGesture {
                        val down = awaitFirstDown(requireUnconsumed = false)
                        if (down.type != PointerType.Touch) return@awaitEachGesture
                        val timedOut = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
                            while (true) {
                                val event = awaitPointerEvent(PointerEventPass.Main)
                                if (event.changes.any { !it.pressed && it.previousPressed }) break
                            }
                        } == null
                        if (timedOut) {
                            showMenu = true
                            var anyPressed = true
                            while (anyPressed) {
                                val e = awaitPointerEvent(PointerEventPass.Initial)
                                e.changes.forEach { it.consume() }
                                anyPressed = e.changes.any { it.pressed }
                            }
                        }
                    }
                }
        )

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            if (volume.isMounted) {
                DropdownMenuItem(
                    text = { Text("Open") },
                    onClick = { onClick(); showMenu = false },
                    leadingIcon = { Icon(Icons.Filled.FolderOpen, null) }
                )
                HorizontalDivider()
            }
            DropdownMenuItem(
                text = { Text("Eject") },
                onClick = { onEject(); showMenu = false },
                leadingIcon = { Icon(Icons.Filled.Eject, null) }
            )
            DropdownMenuItem(
                text = { Text("Format…") },
                onClick = { onFormat(); showMenu = false },
                enabled = volume.isMounted,
                leadingIcon = { Icon(Icons.Filled.Storage, null) }
            )
        }
    }
}
