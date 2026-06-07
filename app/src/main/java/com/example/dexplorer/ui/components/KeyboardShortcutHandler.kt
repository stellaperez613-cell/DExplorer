package com.example.dexplorer.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type

data class KeyboardShortcuts(
    val onCopy: () -> Unit = {},
    val onCut: () -> Unit = {},
    val onPaste: () -> Unit = {},
    val onDelete: () -> Unit = {},
    val onRename: () -> Unit = {},
    val onSelectAll: () -> Unit = {},
    val onRefresh: () -> Unit = {},
    val onNewFolder: () -> Unit = {},
    val onBack: () -> Unit = {}
)

fun Modifier.keyboardShortcuts(
    shortcuts: KeyboardShortcuts
): Modifier = this.onKeyEvent { event ->
    if (event.type == KeyEventType.KeyDown) {
        when {
            // Ctrl+C - Copy
            event.isCtrlPressed && event.key == Key.C -> {
                shortcuts.onCopy()
                true
            }
            // Ctrl+X - Cut
            event.isCtrlPressed && event.key == Key.X -> {
                shortcuts.onCut()
                true
            }
            // Ctrl+V - Paste
            event.isCtrlPressed && event.key == Key.V -> {
                shortcuts.onPaste()
                true
            }
            // Delete - Delete selected
            event.key == Key.Delete -> {
                shortcuts.onDelete()
                true
            }
            // F2 - Rename
            event.key == Key.F2 -> {
                shortcuts.onRename()
                true
            }
            // Ctrl+A - Select All
            event.isCtrlPressed && event.key == Key.A -> {
                shortcuts.onSelectAll()
                true
            }
            // F5 - Refresh
            event.key == Key.F5 -> {
                shortcuts.onRefresh()
                true
            }
            // Ctrl+Shift+N - New Folder
            event.isCtrlPressed && event.key == Key.N -> {
                shortcuts.onNewFolder()
                true
            }
            // Backspace - Navigate back
            event.key == Key.Backspace -> {
                shortcuts.onBack()
                true
            }
            else -> false
        }
    } else {
        false
    }
}

@Composable
fun KeyboardShortcutScope(
    shortcuts: KeyboardShortcuts,
    content: @Composable (Modifier) -> Unit
) {
    val focusRequester = FocusRequester()

    DisposableEffect(Unit) {
        focusRequester.requestFocus()
        onDispose { }
    }

    content(
        Modifier
            .focusRequester(focusRequester)
            .keyboardShortcuts(shortcuts)
    )
}