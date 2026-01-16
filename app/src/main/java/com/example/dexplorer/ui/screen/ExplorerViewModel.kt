package com.example.dexplorer.ui.screen

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dexplorer.data.model.ClipboardOperation
import com.example.dexplorer.data.model.ClipboardState
import com.example.dexplorer.data.model.FileItem
import com.example.dexplorer.data.model.SortOption
import com.example.dexplorer.data.model.ViewMode
import com.example.dexplorer.domain.usecase.CopyFilesUseCase
import com.example.dexplorer.domain.usecase.CreateFolderUseCase
import com.example.dexplorer.domain.usecase.DeleteFilesUseCase
import com.example.dexplorer.domain.usecase.GetDirectoryContentsUseCase
import com.example.dexplorer.domain.usecase.MoveFilesUseCase
import com.example.dexplorer.domain.usecase.RenameFileUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ExplorerUiState(
    val currentPath: String = "",
    val items: List<FileItem> = emptyList(),
    val selectedItems: Set<String> = emptySet(),
    val clipboard: ClipboardState = ClipboardState(),
    val viewMode: ViewMode = ViewMode.LIST,
    val sortOption: SortOption = SortOption.NAME_ASC,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ExplorerViewModel @Inject constructor(
    private val getDirectoryContentsUseCase: GetDirectoryContentsUseCase,
    private val copyFilesUseCase: CopyFilesUseCase,
    private val moveFilesUseCase: MoveFilesUseCase,
    private val deleteFilesUseCase: DeleteFilesUseCase,
    private val renameFileUseCase: RenameFileUseCase,
    private val createFolderUseCase: CreateFolderUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExplorerUiState())
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()

    init {
        // Start at internal storage root
        val initialPath = Environment.getExternalStorageDirectory().absolutePath
        navigateToDirectory(initialPath)
    }

    fun navigateToDirectory(path: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            getDirectoryContentsUseCase(path).fold(
                onSuccess = { files ->
                    _uiState.update {
                        it.copy(
                            currentPath = path,
                            items = files,
                            selectedItems = emptySet(), // Clear selection on navigation
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to load directory"
                        )
                    }
                }
            )
        }
    }

    fun toggleItemSelection(path: String) {
        _uiState.update { state ->
            val newSelection = if (path in state.selectedItems) {
                state.selectedItems - path
            } else {
                state.selectedItems + path
            }
            state.copy(selectedItems = newSelection)
        }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedItems = emptySet()) }
    }

    fun navigateUp() {
        val currentPath = _uiState.value.currentPath
        val parentPath = currentPath.substringBeforeLast("/")
        if (parentPath.isNotEmpty()) {
            navigateToDirectory(parentPath)
        }
    }

    fun refreshCurrentDirectory() {
        val currentPath = _uiState.value.currentPath
        if (currentPath.isNotEmpty()) {
            navigateToDirectory(currentPath)
        }
    }

    fun setViewMode(viewMode: ViewMode) {
        _uiState.update { it.copy(viewMode = viewMode) }
    }

    fun setSortOption(sortOption: SortOption) {
        _uiState.update { it.copy(sortOption = sortOption) }
    }

    // File Operations

    fun copyToClipboard() {
        val selectedPaths = _uiState.value.selectedItems.toList()
        if (selectedPaths.isEmpty()) return

        val selectedFiles = _uiState.value.items.filter { it.path in selectedPaths }
        _uiState.update {
            it.copy(
                clipboard = ClipboardState(
                    items = selectedFiles,
                    operation = ClipboardOperation.COPY
                )
            )
        }
    }

    fun cutToClipboard() {
        val selectedPaths = _uiState.value.selectedItems.toList()
        if (selectedPaths.isEmpty()) return

        val selectedFiles = _uiState.value.items.filter { it.path in selectedPaths }
        _uiState.update {
            it.copy(
                clipboard = ClipboardState(
                    items = selectedFiles,
                    operation = ClipboardOperation.CUT
                )
            )
        }
    }

    fun paste() {
        val clipboard = _uiState.value.clipboard
        if (clipboard.items.isEmpty()) return

        val destinationPath = _uiState.value.currentPath
        val sourcePaths = clipboard.items.map { it.path }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = when (clipboard.operation) {
                ClipboardOperation.COPY -> copyFilesUseCase(sourcePaths, destinationPath)
                ClipboardOperation.CUT -> moveFilesUseCase(sourcePaths, destinationPath)
                ClipboardOperation.NONE -> Result.success(false)
            }

            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            clipboard = ClipboardState(), // Clear clipboard
                            successMessage = "Operation completed successfully"
                        )
                    }
                    refreshCurrentDirectory()
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Operation failed"
                        )
                    }
                }
            )
        }
    }

    fun deleteSelected() {
        val selectedPaths = _uiState.value.selectedItems.toList()
        if (selectedPaths.isEmpty()) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            deleteFilesUseCase(selectedPaths).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(successMessage = "Files deleted successfully")
                    }
                    refreshCurrentDirectory()
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to delete files"
                        )
                    }
                }
            )
        }
    }

    fun renameFile(path: String, newName: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            renameFileUseCase(path, newName).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(successMessage = "File renamed successfully")
                    }
                    refreshCurrentDirectory()
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to rename file"
                        )
                    }
                }
            )
        }
    }

    fun createFolder(folderName: String) {
        val currentPath = _uiState.value.currentPath

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            createFolderUseCase(currentPath, folderName).fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(successMessage = "Folder created successfully")
                    }
                    refreshCurrentDirectory()
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = exception.message ?: "Failed to create folder"
                        )
                    }
                }
            )
        }
    }

    fun clearMessages() {
        _uiState.update { it.copy(error = null, successMessage = null) }
    }

    fun hasExtensionChanged(oldName: String, newName: String): Boolean {
        return renameFileUseCase.hasExtensionChanged(oldName, newName)
    }
}