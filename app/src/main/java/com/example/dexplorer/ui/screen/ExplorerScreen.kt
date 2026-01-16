package com.example.dexplorer.ui.screen

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.dexplorer.data.model.ClipboardOperation
import com.example.dexplorer.ui.components.BreadcrumbNavigation
import com.example.dexplorer.ui.components.DeleteConfirmationDialog
import com.example.dexplorer.ui.components.ExtensionWarningDialog
import com.example.dexplorer.ui.components.FileListView
import com.example.dexplorer.ui.components.NewFolderDialog
import com.example.dexplorer.ui.components.RenameDialog
import com.example.dexplorer.ui.components.SelectionToolbar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExplorerScreen(
    viewModel: ExplorerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showExtensionWarning by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var pendingRename by remember { mutableStateOf<Pair<String, String>?>(null) }

    // Show snackbar for errors and success messages
    LaunchedEffect(uiState.error) {
        uiState.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessages()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("DExplorer") },
                navigationIcon = {
                    IconButton(onClick = { viewModel.navigateUp() }) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Navigate up"
                        )
                    }
                },
                actions = {
                    // Show paste button if clipboard has items
                    if (uiState.clipboard.operation != ClipboardOperation.NONE) {
                        IconButton(onClick = { viewModel.paste() }) {
                            Icon(
                                imageVector = Icons.Default.ContentPaste,
                                contentDescription = "Paste"
                            )
                        }
                    }

                    IconButton(onClick = { viewModel.refreshCurrentDirectory() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showNewFolderDialog = true }) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "New Folder"
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Selection Toolbar
            if (uiState.selectedItems.isNotEmpty()) {
                SelectionToolbar(
                    selectedCount = uiState.selectedItems.size,
                    onCopy = { viewModel.copyToClipboard() },
                    onCut = { viewModel.cutToClipboard() },
                    onDelete = { showDeleteDialog = true },
                    onRename = { showRenameDialog = true },
                    onClearSelection = { viewModel.clearSelection() }
                )
            }

            // Breadcrumb Navigation
            BreadcrumbNavigation(
                currentPath = uiState.currentPath,
                onNavigate = { path -> viewModel.navigateToDirectory(path) }
            )

            // Content
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.error != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(text = "Error: ${uiState.error}")
                    }
                }

                else -> {
                    FileListView(
                        files = uiState.items,
                        selectedItems = uiState.selectedItems,
                        onItemClick = { file ->
                            viewModel.toggleItemSelection(file.path)
                        },
                        onItemDoubleClick = { file ->
                            if (file.isDirectory) {
                                viewModel.navigateToDirectory(file.path)
                            }
                            // TODO: Open file with appropriate app in Phase 4
                        }
                    )
                }
            }
        }
    }

    // Dialogs
    if (showDeleteDialog) {
        DeleteConfirmationDialog(
            itemCount = uiState.selectedItems.size,
            onConfirm = {
                viewModel.deleteSelected()
                showDeleteDialog = false
            },
            onDismiss = { showDeleteDialog = false }
        )
    }

    if (showRenameDialog) {
        val selectedPath = uiState.selectedItems.firstOrNull()
        val selectedFile = uiState.items.find { it.path == selectedPath }

        if (selectedFile != null) {
            RenameDialog(
                currentName = selectedFile.name,
                onConfirm = { newName ->
                    pendingRename = selectedFile.path to newName
                    showRenameDialog = false
                    viewModel.renameFile(selectedFile.path, newName)
                },
                onDismiss = { showRenameDialog = false },
                onExtensionWarning = {
                    pendingRename = selectedFile.path to ""
                    showRenameDialog = false
                    showExtensionWarning = true
                },
                hasExtensionChanged = { oldName, newName ->
                    viewModel.hasExtensionChanged(oldName, newName)
                }
            )
        }
    }

    if (showExtensionWarning) {
        ExtensionWarningDialog(
            onConfirm = {
                pendingRename?.let { (path, newName) ->
                    viewModel.renameFile(path, newName)
                }
                pendingRename = null
                showExtensionWarning = false
            },
            onDismiss = {
                pendingRename = null
                showExtensionWarning = false
            }
        )
    }

    if (showNewFolderDialog) {
        NewFolderDialog(
            onConfirm = { folderName ->
                viewModel.createFolder(folderName)
                showNewFolderDialog = false
            },
            onDismiss = { showNewFolderDialog = false }
        )
    }
}