package com.example.dexplorer.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp

@Composable
fun NewFileDialog(
    defaultName: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val extension = defaultName.substringAfterLast('.', "")
    val nameWithoutExt = if (extension.isNotEmpty()) defaultName.dropLast(extension.length + 1) else defaultName
    var fieldValue by remember {
        mutableStateOf(
            TextFieldValue(
                text = defaultName,
                selection = TextRange(0, nameWithoutExt.length)
            )
        )
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text("New File") },
        text = {
            Column {
                Text("Enter file name:")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = fieldValue,
                    onValueChange = { fieldValue = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (fieldValue.text.isNotBlank()) onConfirm(fieldValue.text.trim()) },
                enabled = fieldValue.text.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
