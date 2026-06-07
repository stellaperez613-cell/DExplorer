package com.example.dexplorer.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eject
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import kotlinx.coroutines.withTimeoutOrNull
import com.example.dexplorer.data.model.QuickAccessFolder
import com.example.dexplorer.data.model.StorageVolumeInfo

@Composable
fun NavigationRailPanel(
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
    Column(modifier = modifier.fillMaxHeight()) {
        HorizontalDivider()
        NavigationRail {
            pinnedFolders.forEach { folder ->
                RailQuickAccessItem(
                    folder = folder,
                    isSelected = currentPath == folder.path,
                    isProtected = isProtected(folder.path),
                    onClick = { onNavigate(folder.path) },
                    onUnpin = { onUnpin(folder.path) }
                )
            }
            if (storageVolumes.isNotEmpty()) {
                HorizontalDivider()
                storageVolumes.forEach { volume ->
                    RailDeviceItem(
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
}

@Composable
private fun RailQuickAccessItem(
    folder: QuickAccessFolder,
    isSelected: Boolean,
    isProtected: Boolean,
    onClick: () -> Unit,
    onUnpin: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        NavigationRailItem(
            icon = { Icon(quickAccessIcon(folder.path), contentDescription = folder.name) },
            label = { Text(folder.name.take(8)) },
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
private fun RailDeviceItem(
    volume: StorageVolumeInfo,
    isSelected: Boolean,
    onClick: () -> Unit,
    onEject: () -> Unit,
    onFormat: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        NavigationRailItem(
            icon = { Icon(storageVolumeIcon(volume.name), contentDescription = volume.name) },
            label = { Text(volume.name.take(8)) },
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
