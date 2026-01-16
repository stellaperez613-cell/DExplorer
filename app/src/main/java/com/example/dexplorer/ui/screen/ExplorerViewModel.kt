package com.example.dexplorer.ui.screen

import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dexplorer.data.model.ClipboardOperation
import com.example.dexplorer.data.model.ClipboardState
import com.example.dexplorer.data.model.FileItem
import com.example.dexplorer.data.model.SortOption
import com.example.dexplorer.data.model.ViewMode
import com.example.dexplorer.domain.usecase.GetDirectoryContentsUseCase
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
    val error: String? = null
)

@HiltViewModel
class ExplorerViewModel @Inject constructor(
    private val getDirectoryContentsUseCase: GetDirectoryContentsUseCase
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
}