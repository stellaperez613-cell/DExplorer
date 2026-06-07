package com.example.dexplorer.ui.screen

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.os.Environment
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.dexplorer.core.util.AppInfo
import com.example.dexplorer.core.util.DefaultAppManager
import com.example.dexplorer.core.util.DefaultAppResolver
import com.example.dexplorer.core.util.FileLogger
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
import com.example.dexplorer.domain.usecase.OpenFileUseCase
import com.example.dexplorer.domain.usecase.RenameFileUseCase
import com.example.dexplorer.domain.usecase.SearchFilesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import com.example.dexplorer.core.util.FileTemplateCreator
import com.example.dexplorer.data.model.FormatType
import com.example.dexplorer.data.model.QuickAccessFolder
import com.example.dexplorer.data.model.StorageVolumeInfo
import com.example.dexplorer.data.repository.QuickAccessRepository
import com.example.dexplorer.data.repository.StorageVolumeRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import com.example.dexplorer.core.util.ThumbnailGenerator
import com.example.dexplorer.domain.usecase.CalculateFolderSizeUseCase
import com.example.dexplorer.domain.usecase.FolderStats
import com.example.dexplorer.domain.usecase.ZipUseCase
import java.io.File


enum class SearchScope { CURRENT_FOLDER, ENTIRE_DEVICE }

enum class ConflictResolution { REPLACE, KEEP_BOTH, SKIP }

data class FileConflict(
    val sourcePath: String,
    val destPath: String,
    val sourceSize: Long,
    val sourceModified: Long,
    val existingSize: Long,
    val existingModified: Long,
    val isDirectory: Boolean
) {
    val name: String get() = File(sourcePath).name
}

data class PasteConflictState(
    val conflicts: List<FileConflict>,
    val currentIndex: Int = 0,
    val resolutions: Map<String, ConflictResolution> = emptyMap(),
    val operation: ClipboardOperation,
    val allSourcePaths: List<String>,
    val destDirectory: String
) {
    val current: FileConflict get() = conflicts[currentIndex]
    val isLast: Boolean get() = currentIndex == conflicts.lastIndex
}

data class ExplorerUiState(
    val currentPath: String = "",
    val items: List<FileItem> = emptyList(),
    val selectedItems: Set<String> = emptySet(),
    val clipboard: ClipboardState = ClipboardState(),
    val viewMode: ViewMode = ViewMode.LIST,
    val sortOption: SortOption = SortOption.NAME_ASC,
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val searchQuery: String = "",
    val isSearching: Boolean = false,
    val searchTooShort: Boolean = false,
    val searchScope: SearchScope = SearchScope.CURRENT_FOLDER,
    val canNavigateForward: Boolean = false,
    val canNavigateBack: Boolean = false,
    val pasteConflict: PasteConflictState? = null,
    val pinnedFolders: List<QuickAccessFolder> = emptyList(),
    val storageVolumes: List<StorageVolumeInfo> = emptyList(),
    val pendingFormatVolume: StorageVolumeInfo? = null
)

@HiltViewModel
class ExplorerViewModel @Inject constructor(
    private val getDirectoryContentsUseCase: GetDirectoryContentsUseCase,
    private val copyFilesUseCase: CopyFilesUseCase,
    private val moveFilesUseCase: MoveFilesUseCase,
    private val deleteFilesUseCase: DeleteFilesUseCase,
    private val renameFileUseCase: RenameFileUseCase,
    private val createFolderUseCase: CreateFolderUseCase,
    private val searchFilesUseCase: SearchFilesUseCase,
    private val openFileUseCase: OpenFileUseCase,
    private val calculateFolderSizeUseCase: CalculateFolderSizeUseCase,
    private val zipUseCase: ZipUseCase,
    private val defaultAppManager: DefaultAppManager,
    private val quickAccessRepository: QuickAccessRepository,
    private val storageVolumeRepository: StorageVolumeRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExplorerUiState())
    val uiState: StateFlow<ExplorerUiState> = _uiState.asStateFlow()
    private var searchJob: Job? = null
    private var tooShortJob: Job? = null
    private var thumbnailJob: Job? = null
    private val navigationHistory = mutableListOf<String>()
    private var currentHistoryIndex = -1
    private val _folderStats = MutableStateFlow<Map<String, FolderStats>>(emptyMap())
    val folderStats: StateFlow<Map<String, FolderStats>> = _folderStats.asStateFlow()

    private val _calculatingFolderPaths = MutableStateFlow<Set<String>>(emptySet())
    val calculatingFolderPaths: StateFlow<Set<String>> = _calculatingFolderPaths.asStateFlow()
    private val folderSizeJobs = mutableMapOf<String, Job>()

    // File tracking for badge management
    private val _fileWaitingForDefaultChange = MutableStateFlow<FileItem?>(null)
    val fileWaitingForDefaultChange = _fileWaitingForDefaultChange.asStateFlow()

    private val _fileWaitingForBadgeTracking = MutableStateFlow<FileItem?>(null)
    val fileWaitingForBadgeTracking = _fileWaitingForBadgeTracking.asStateFlow()

    data class EjectResult(val volumeName: String, val needsManualEject: Boolean)
    private val _ejectResultEvent = MutableSharedFlow<EjectResult>()
    val ejectResultEvent: SharedFlow<EjectResult> = _ejectResultEvent.asSharedFlow()

    private val _formatVolumeEvent = MutableSharedFlow<StorageVolumeInfo>()
    val formatVolumeEvent: SharedFlow<StorageVolumeInfo> = _formatVolumeEvent.asSharedFlow()

    init {
        val initialPath = Environment.getExternalStorageDirectory().absolutePath
        navigationHistory.add(initialPath)
        currentHistoryIndex = 0
        loadDirectory(initialPath)
        loadPinnedFolders()
        viewModelScope.launch {
            storageVolumeRepository.volumes.collect { volumes ->
                _uiState.update { it.copy(storageVolumes = volumes) }
            }
        }
    }

    private fun loadDirectory(path: String, silent: Boolean = false) {
        thumbnailJob?.cancel()
        if (!silent) {
            _uiState.update {
                it.copy(
                    searchQuery = "",
                    isSearching = false,
                    isLoading = true,
                    error = null
                )
            }
        }

        viewModelScope.launch {
            getDirectoryContentsUseCase(path).fold(
                onSuccess = { files ->
                    _uiState.update {
                        it.copy(
                            currentPath = path,
                            items = files,
                            selectedItems = emptySet(),
                            isLoading = false,
                            error = null,
                            canNavigateForward = canNavigateForwardInternal(),
                            canNavigateBack = canNavigateBackInternal()
                        )
                    }
                    generateThumbnailsAsync(files)
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

    fun navigateToDirectory(path: String) {
        FileLogger.log("========== NAVIGATING ==========")
        FileLogger.log("To: $path")
        FileLogger.log("History BEFORE: $navigationHistory")
        FileLogger.log("Index BEFORE: $currentHistoryIndex")

        if (currentHistoryIndex < navigationHistory.size - 1) {
            navigationHistory.subList(currentHistoryIndex + 1, navigationHistory.size).clear()
        }

        if (navigationHistory.isEmpty() || navigationHistory.getOrNull(currentHistoryIndex) != path) {
            navigationHistory.add(path)
            currentHistoryIndex = navigationHistory.size - 1
        }

        FileLogger.log("History AFTER: $navigationHistory")
        FileLogger.log("Index AFTER: $currentHistoryIndex")

        loadDirectory(path)
    }

    fun toggleItemSelection(path: String) {
        FileLogger.log("SELECT: toggleItemSelection -> '${path.substringAfterLast('/')}'")
        _uiState.update { state ->
            val newSelected = setOf(path)
            FileLogger.log("SELECT: state updated, selectedItems=${newSelected.map { it.substringAfterLast('/') }}")
            state.copy(selectedItems = newSelected)
        }
    }

    fun toggleItemSelectionMulti(path: String) {
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

    fun setSelection(paths: Set<String>) {
        _uiState.update { it.copy(selectedItems = paths) }
    }

    fun moveFilesToDirectory(sourcePaths: List<String>, destDirectory: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val destDir = File(destDirectory)
            val conflicts = detectConflicts(sourcePaths, destDir)
            if (conflicts.isNotEmpty()) {
                _uiState.update {
                    it.copy(pasteConflict = PasteConflictState(
                        conflicts = conflicts,
                        operation = ClipboardOperation.CUT,
                        allSourcePaths = sourcePaths,
                        destDirectory = destDirectory
                    ))
                }
            } else {
                moveFilesUseCase(sourcePaths, destDirectory).fold(
                    onSuccess = { refreshCurrentDirectory() },
                    onFailure = { e -> _uiState.update { it.copy(error = e.message ?: "Move failed") } }
                )
            }
        }
    }

    fun selectAll() {
        _uiState.update { state ->
            val allPaths = state.items.map { it.path }.toSet()
            state.copy(selectedItems = allPaths)
        }
    }

    fun navigateUp() {
        val currentPath = _uiState.value.currentPath

        if (currentPath == "/" || currentPath.isEmpty() || currentPath == "/storage/emulated/0") {
            return
        }

        val parentPath = currentPath.substringBeforeLast("/")

        val targetPath = if (parentPath == "/storage/emulated" || parentPath == "/storage") {
            "/storage/emulated/0"
        } else if (parentPath.isNotEmpty()) {
            parentPath
        } else {
            "/storage/emulated/0"
        }

        val indexOfTarget = navigationHistory.lastIndexOf(targetPath)
        if (indexOfTarget >= 0 && indexOfTarget < currentHistoryIndex) {
            FileLogger.log("NavigateUp: Moving back in history to index $indexOfTarget")
            currentHistoryIndex = indexOfTarget
            loadDirectory(targetPath)
        } else {
            FileLogger.log("NavigateUp: Adding new path to history")
            navigateToDirectory(targetPath)
        }
    }

    fun navigateBack() {
        if (canNavigateBackInternal()) {
            thumbnailJob?.cancel()
            currentHistoryIndex--
            val prevPath = navigationHistory[currentHistoryIndex]
            _uiState.update {
                it.copy(searchQuery = "", isSearching = false, isLoading = true, error = null)
            }
            viewModelScope.launch {
                getDirectoryContentsUseCase(prevPath).fold(
                    onSuccess = { files ->
                        _uiState.update {
                            it.copy(
                                currentPath = prevPath,
                                items = files,
                                selectedItems = emptySet(),
                                isLoading = false,
                                error = null,
                                canNavigateForward = canNavigateForwardInternal(),
                                canNavigateBack = canNavigateBackInternal()
                            )
                        }
                        generateThumbnailsAsync(files)
                    },
                    onFailure = { exception ->
                        _uiState.update {
                            it.copy(isLoading = false, error = exception.message ?: "Failed to load directory")
                        }
                    }
                )
            }
        }
    }

    fun navigateForward() {
        if (canNavigateForwardInternal()) {
            thumbnailJob?.cancel()
            currentHistoryIndex++
            val nextPath = navigationHistory[currentHistoryIndex]

            _uiState.update {
                it.copy(
                    searchQuery = "",
                    isSearching = false,
                    isLoading = true,
                    error = null
                )
            }

            viewModelScope.launch {
                getDirectoryContentsUseCase(nextPath).fold(
                    onSuccess = { files ->
                        _uiState.update {
                            it.copy(
                                currentPath = nextPath,
                                items = files,
                                selectedItems = emptySet(),
                                isLoading = false,
                                error = null,
                                canNavigateForward = canNavigateForwardInternal(),
                                canNavigateBack = canNavigateBackInternal()
                            )
                        }
                        generateThumbnailsAsync(files)
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
    }

    private fun canNavigateForwardInternal(): Boolean {
        val canGo = currentHistoryIndex < navigationHistory.size - 1
        FileLogger.log("canNavigateForward = $canGo, index=$currentHistoryIndex, size=${navigationHistory.size}")
        return canGo
    }

    private fun canNavigateBackInternal(): Boolean = currentHistoryIndex > 0

    fun getCurrentFolderName(): String {
        val path = _uiState.value.currentPath
        return when {
            path.isEmpty() || path == "/" -> "DExplorer"
            path == "/storage/emulated/0" -> "Internal Storage"
            else -> path.substringAfterLast("/").replaceFirstChar { it.uppercase() }
        }
    }

    private fun generateThumbnailsAsync(items: List<FileItem>) {
        thumbnailJob = viewModelScope.launch(Dispatchers.IO) {
            items
                .filter { !it.isDirectory }
                .chunked(8)
                .forEach { chunk ->
                    ensureActive()
                    val updates = chunk.mapNotNull { item ->
                        val path = when {
                            item.mimeType?.startsWith("image/") == true ||
                            item.mimeType?.startsWith("video/") == true ||
                            item.mimeType?.startsWith("audio/") == true ||
                            item.mimeType == "application/pdf" ||
                            item.mimeType?.contains("wordprocessingml") == true ||
                            item.mimeType?.contains("spreadsheetml") == true ||
                            item.mimeType?.contains("presentationml") == true ->
                                ThumbnailGenerator.generateThumbnail(item.path, item.mimeType)
                            !item.extension.isNullOrEmpty() ->
                                ThumbnailGenerator.generateFileTypeIcon(item.extension)
                            else -> null
                        }
                        path?.let { item.path to it }
                    }
                    if (updates.isNotEmpty()) {
                        val updateMap = updates.toMap()
                        _uiState.update { state ->
                            state.copy(
                                items = state.items.map { item ->
                                    updateMap[item.path]?.let { item.copy(thumbnailPath = it) } ?: item
                                }
                            )
                        }
                    }
                }
        }
    }

    fun refreshCurrentDirectory() {
        val currentPath = _uiState.value.currentPath
        if (currentPath.isNotEmpty()) {
            loadDirectory(currentPath, silent = true)
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

        viewModelScope.launch(Dispatchers.IO) {
            val destDir = File(destinationPath)
            val conflicts = detectConflicts(sourcePaths, destDir)
            if (conflicts.isNotEmpty()) {
                _uiState.update {
                    it.copy(pasteConflict = PasteConflictState(
                        conflicts = conflicts,
                        operation = clipboard.operation,
                        allSourcePaths = sourcePaths,
                        destDirectory = destinationPath
                    ))
                }
            } else {
                executePaste(sourcePaths, destinationPath, clipboard.operation, emptyMap())
            }
        }
    }

    fun resolveCurrentConflict(resolution: ConflictResolution, applyToAll: Boolean) {
        val state = _uiState.value.pasteConflict ?: return
        val newResolutions = state.resolutions + (state.current.sourcePath to resolution)

        if (applyToAll) {
            val allResolutions = state.conflicts.associate { it.sourcePath to resolution }
            _uiState.update { it.copy(pasteConflict = null) }
            viewModelScope.launch { executePaste(state.allSourcePaths, state.destDirectory, state.operation, allResolutions) }
        } else if (state.isLast) {
            _uiState.update { it.copy(pasteConflict = null) }
            viewModelScope.launch { executePaste(state.allSourcePaths, state.destDirectory, state.operation, newResolutions) }
        } else {
            _uiState.update {
                it.copy(pasteConflict = state.copy(currentIndex = state.currentIndex + 1, resolutions = newResolutions))
            }
        }
    }

    fun cancelPasteConflict() {
        _uiState.update { it.copy(pasteConflict = null) }
    }

    private suspend fun executePaste(
        allSourcePaths: List<String>,
        destDirectory: String,
        operation: ClipboardOperation,
        resolutions: Map<String, ConflictResolution>
    ) = withContext(Dispatchers.IO) {
        try {
            val destDir = File(destDirectory)
            val normalPaths = mutableListOf<String>()
            val keepBothPaths = mutableListOf<String>()

            for (path in allSourcePaths) {
                when (resolutions[path]) {
                    ConflictResolution.REPLACE  -> normalPaths.add(path)
                    ConflictResolution.KEEP_BOTH -> keepBothPaths.add(path)
                    ConflictResolution.SKIP      -> { /* excluded */ }
                    null                          -> normalPaths.add(path)
                }
            }

            if (normalPaths.isNotEmpty()) {
                when (operation) {
                    ClipboardOperation.COPY -> copyFilesUseCase(normalPaths, destDirectory)
                    ClipboardOperation.CUT  -> moveFilesUseCase(normalPaths, destDirectory)
                    ClipboardOperation.NONE -> {}
                }
            }

            for (sourcePath in keepBothPaths) {
                val source = File(sourcePath)
                val newName = generateUniqueName(destDir, source.name, source.isDirectory)
                val dest = File(destDir, newName)
                when (operation) {
                    ClipboardOperation.COPY -> {
                        if (source.isDirectory) source.copyRecursively(dest) else source.copyTo(dest)
                    }
                    ClipboardOperation.CUT -> {
                        if (!source.renameTo(dest)) {
                            if (source.isDirectory) { source.copyRecursively(dest); source.deleteRecursively() }
                            else { source.copyTo(dest); source.delete() }
                        }
                    }
                    ClipboardOperation.NONE -> {}
                }
            }

            _uiState.update {
                if (operation == ClipboardOperation.CUT) it.copy(clipboard = ClipboardState()) else it
            }
            refreshCurrentDirectory()
        } catch (e: Exception) {
            _uiState.update { it.copy(error = e.message ?: "Operation failed") }
        }
    }

    private fun detectConflicts(sourcePaths: List<String>, destDir: File): List<FileConflict> =
        sourcePaths.mapNotNull { sourcePath ->
            val source = File(sourcePath)
            val dest = File(destDir, source.name)
            if (dest.exists() && dest.canonicalPath != source.canonicalPath) FileConflict(
                sourcePath = sourcePath,
                destPath = dest.path,
                sourceSize = if (source.isDirectory) -1L else source.length(),
                sourceModified = source.lastModified(),
                existingSize = if (dest.isDirectory) -1L else dest.length(),
                existingModified = dest.lastModified(),
                isDirectory = source.isDirectory
            ) else null
        }

    private fun generateUniqueName(destDir: File, name: String, isDirectory: Boolean): String {
        if (isDirectory) {
            var n = 1
            while (File(destDir, "$name ($n)").exists()) n++
            return "$name ($n)"
        }
        val dot = name.lastIndexOf('.')
        val base = if (dot > 0) name.substring(0, dot) else name
        val ext  = if (dot > 0) name.substring(dot) else ""
        var n = 1
        while (File(destDir, "$base ($n)$ext").exists()) n++
        return "$base ($n)$ext"
    }

    fun deleteSelected() {
        val selectedPaths = _uiState.value.selectedItems.toList()
        if (selectedPaths.isEmpty()) return

        viewModelScope.launch {
            deleteFilesUseCase(selectedPaths).fold(
                onSuccess = {
                    refreshCurrentDirectory()
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            error = exception.message ?: "Failed to delete files"
                        )
                    }
                }
            )
        }
    }

    fun renameFile(path: String, newName: String) {
        viewModelScope.launch {
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

    fun search(query: String) {
        searchJob?.cancel()
        tooShortJob?.cancel()
        _uiState.update { it.copy(searchQuery = query, searchTooShort = false) }

        if (query.isBlank()) {
            _uiState.update { it.copy(isSearching = false) }
            refreshCurrentDirectory()
            return
        }

        if (query.length < 3) {
            _uiState.update { it.copy(isSearching = false) }
            refreshCurrentDirectory()
            tooShortJob = viewModelScope.launch {
                delay(2000)
                _uiState.update { it.copy(searchTooShort = true) }
            }
            return
        }

        searchJob = viewModelScope.launch {
            delay(300)
            _uiState.update { it.copy(isSearching = true, isLoading = true) }

            val startPath = when (_uiState.value.searchScope) {
                SearchScope.CURRENT_FOLDER -> _uiState.value.currentPath
                SearchScope.ENTIRE_DEVICE -> Environment.getExternalStorageDirectory().absolutePath
            }

            searchFilesUseCase(
                startPath = startPath,
                query = query,
                searchSubfolders = true
            ).fold(
                onSuccess = { results ->
                    _uiState.update {
                        it.copy(
                            items = results,
                            isSearching = true,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { exception ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isSearching = false,
                            error = exception.message ?: "Search failed"
                        )
                    }
                }
            )
        }
    }

    fun setSearchScope(scope: SearchScope) {
        _uiState.update { it.copy(searchScope = scope) }
        val query = _uiState.value.searchQuery
        if (query.length >= 3) {
            search(query)
        }
    }

    fun clearSearch() {
        searchJob?.cancel()
        tooShortJob?.cancel()
        _uiState.update {
            it.copy(
                searchQuery = "",
                isSearching = false,
                searchTooShort = false
            )
        }
        refreshCurrentDirectory()
    }

    // FILE OPENING FUNCTIONS
    fun openFile(file: FileItem) {
        if (file.isDirectory) {
            navigateToDirectory(file.path)
            return
        }

        FileLogger.log("Opening file: ${file.path}, mimeType: ${file.mimeType}")

        viewModelScope.launch {
            openFileUseCase.openFile(file.path, file.mimeType ?: "*/*", forceChooser = false).fold(
                onSuccess = {
                    FileLogger.log("File opened successfully")
                },
                onFailure = { exception ->
                    FileLogger.log("Failed to open file: ${exception.message}")
                    _uiState.update {
                        it.copy(error = exception.message ?: "Failed to open file")
                    }
                }
            )
        }
    }

    fun openFileWith(file: FileItem) {
        if (file.isDirectory) {
            navigateToDirectory(file.path)
            return
        }

        FileLogger.log("Opening file with chooser: ${file.path}")

        viewModelScope.launch {
            openFileUseCase.openFileWith(file.path, file.mimeType ?: "*/*").fold(
                onSuccess = {
                    FileLogger.log("Chooser opened successfully")
                },
                onFailure = { exception ->
                    FileLogger.log("Failed to open chooser: ${exception.message}")
                    _uiState.update {
                        it.copy(error = exception.message ?: "Failed to open file")
                    }
                }
            )
        }
    }

    fun openFileWithChooser(file: FileItem) {
        FileLogger.log("Opening file with CHOOSER: ${file.path}, mimeType: ${file.mimeType}")

        viewModelScope.launch {
            openFileUseCase.openFile(file.path, file.mimeType ?: "*/*", forceChooser = true).fold(
                onSuccess = {
                    FileLogger.log("File opened with chooser successfully")
                },
                onFailure = { exception ->
                    FileLogger.log("Failed to open file with chooser: ${exception.message}")
                    _uiState.update {
                        it.copy(error = exception.message ?: "Failed to open file")
                    }
                }
            )
        }
    }

    // FOLDER SIZE CALCULATION
    fun calculateFolderSize(path: String) {
        folderSizeJobs[path]?.cancel()
        _folderStats.update { it - path }
        _calculatingFolderPaths.update { it + path }
        folderSizeJobs[path] = viewModelScope.launch {
            calculateFolderSizeUseCase(path).fold(
                onSuccess = { stats ->
                    _folderStats.update { current -> current + (path to stats) }
                },
                onFailure = { }
            )
            _calculatingFolderPaths.update { it - path }
            folderSizeJobs.remove(path)
        }
    }

    fun cancelFolderSizeCalculation(path: String) {
        folderSizeJobs[path]?.cancel()
        folderSizeJobs.remove(path)
        _calculatingFolderPaths.update { it - path }
    }

    fun getFolderStats(path: String): FolderStats? {
        return _folderStats.value[path]
    }

    // ZIP / ARCHIVE OPERATIONS
    fun compress(file: FileItem) {
        val sourcePaths = if (file.path in _uiState.value.selectedItems) {
            _uiState.value.selectedItems.toList()
        } else {
            listOf(file.path)
        }
        val zipName = if (sourcePaths.size == 1) {
            "${File(sourcePaths.first()).nameWithoutExtension}.zip"
        } else {
            "Archive.zip"
        }
        val zipPath = "${_uiState.value.currentPath}/$zipName"
        viewModelScope.launch {
            zipUseCase.compress(sourcePaths, zipPath).fold(
                onSuccess = {
                    refreshCurrentDirectory()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message ?: "Compression failed") }
                }
            )
        }
    }

    fun extractArchive(file: FileItem) {
        val destDir = "${File(file.path).parent}/${File(file.path).nameWithoutExtension}"
        viewModelScope.launch {
            zipUseCase.extract(file.path, destDir).fold(
                onSuccess = {
                    refreshCurrentDirectory()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message ?: "Extraction failed") }
                }
            )
        }
    }

    fun extractArchiveTo(file: FileItem, destinationPath: String) {
        viewModelScope.launch {
            zipUseCase.extract(file.path, destinationPath).fold(
                onSuccess = {
                    refreshCurrentDirectory()
                },
                onFailure = { e ->
                    _uiState.update { it.copy(error = e.message ?: "Extraction failed") }
                }
            )
        }
    }

    // Quick Access pin management
    private fun loadPinnedFolders() {
        val paths = quickAccessRepository.getPinnedPaths()
        val folders = paths.map { path ->
            QuickAccessFolder(
                name = quickAccessName(path),
                path = path,
                isPinned = true
            )
        }
        _uiState.update { it.copy(pinnedFolders = folders) }
    }

    fun pinFolder(path: String) {
        quickAccessRepository.addPath(path)
        loadPinnedFolders()
    }

    fun unpinFolder(path: String) {
        quickAccessRepository.removePath(path)
        loadPinnedFolders()
    }

    fun isFolderPinned(path: String): Boolean =
        _uiState.value.pinnedFolders.any { it.path == path }

    fun isFolderProtected(path: String): Boolean =
        quickAccessRepository.isProtected(path)

    private fun quickAccessName(path: String): String =
        if (path == Environment.getExternalStorageDirectory().absolutePath) "Internal Storage"
        else File(path).name

    fun createEmptyFile(fileName: String) {
        val currentPath = _uiState.value.currentPath
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val destDir = File(currentPath)
                val finalName = if (File(destDir, fileName).exists())
                    generateUniqueName(destDir, fileName, false)
                else fileName
                FileTemplateCreator.createFromExtension(File(destDir, finalName))
                refreshCurrentDirectory()
            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Failed to create file: ${e.message}") }
            }
        }
    }

    // DRAG AND DROP
    fun handleDroppedFiles(uris: List<Uri>, context: Context) {
        val currentPath = _uiState.value.currentPath
        viewModelScope.launch(Dispatchers.IO) {
            var anyFailure = false
            for (uri in uris) {
                try {
                    val fileName = getFileNameFromUri(context, uri) ?: continue
                    val destFile = File(currentPath, fileName)
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        destFile.outputStream().use { output -> input.copyTo(output) }
                    }
                } catch (e: Exception) {
                    FileLogger.log("Drop failed for $uri: ${e.message}")
                    anyFailure = true
                }
            }
            if (anyFailure) {
                _uiState.update { it.copy(error = "Some files could not be received") }
            }
            refreshCurrentDirectory()
        }
    }

    private fun getFileNameFromUri(context: Context, uri: Uri): String? {
        if (uri.scheme == "content") {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) return cursor.getString(idx)
                }
            }
        }
        return uri.lastPathSegment?.substringAfterLast('/')
    }

    // DEFAULT APP / BADGE MANAGEMENT
    fun getAvailableApps(filePath: String, mimeType: String?): List<AppInfo> {
        return defaultAppManager.getAvailableApps(filePath, mimeType)
    }

    fun clearDefaultApp(mimeType: String?, extension: String?) {
        defaultAppManager.clearDefaultApp(mimeType, extension)
    }

    fun getDefaultAppName(context: Context, filePath: String, mimeType: String?): String? {
        FileLogger.log("========================================")
        FileLogger.log("ExplorerViewModel.getDefaultAppName()")
        FileLogger.log("  filePath: $filePath")
        FileLogger.log("  mimeType: $mimeType")

        val file = File(filePath)
        FileLogger.log("  extension: ${file.extension}")

        val packageName = defaultAppManager.getDefaultApp(mimeType, file.extension)
        val name = packageName?.let { defaultAppManager.getAppName(it) }

        FileLogger.log("  Result: $name")
        return name
    }

    fun getSystemDefaultAppName(context: Context, filePath: String, mimeType: String?): String? {
        return DefaultAppResolver.getDefaultAppName(context, filePath, mimeType)
    }

    fun getBadgeAppName(filePath: String, mimeType: String?): String? {
        val file = File(filePath)
        val packageName = defaultAppManager.getDefaultApp(mimeType, file.extension)
        return packageName?.let { defaultAppManager.getAppName(it) }
    }

    fun rememberCurrentAsBadge(context: Context, file: FileItem) {
        FileLogger.log("========================================")
        FileLogger.log("Remembering badge for: ${file.name}")

        val packageName = DefaultAppResolver.getDefaultAppPackage(context, file.path, file.mimeType)

        if (packageName != null) {
            FileLogger.log("  System default: $packageName")
            defaultAppManager.setDefaultApp(file.mimeType, file.extension, packageName)
            refreshCurrentDirectory()
        } else {
            FileLogger.log("  No system default found")
        }
    }

    fun clearBadge(file: FileItem) {
        FileLogger.log("========================================")
        FileLogger.log("Clearing badge for: ${file.name}")

        defaultAppManager.clearDefaultApp(file.mimeType, file.extension)
        refreshCurrentDirectory()
    }

    fun rememberBadgeByPackage(mimeType: String?, extension: String?, packageName: String) {
        FileLogger.log("========================================")
        FileLogger.log("Remembering badge: $packageName for $extension")

        defaultAppManager.setDefaultApp(mimeType, extension, packageName)
    }

    fun setFileWaitingForDefaultChange(file: FileItem?) {
        FileLogger.log("ViewModel: setFileWaitingForDefaultChange = ${file?.name}")
        _fileWaitingForDefaultChange.value = file
    }

    fun setFileWaitingForBadgeTracking(file: FileItem?) {
        FileLogger.log("ViewModel: setFileWaitingForBadgeTracking = ${file?.name}")
        _fileWaitingForBadgeTracking.value = file
    }

    // Storage volume management

    /** Unmounts all partitions on the same physical disk.
     *  If any partition can't be unmounted via reflection the caller must use the system
     *  notification, so needsManualEject is set and the screen expands the panel. */
    fun ejectVolume(volume: StorageVolumeInfo) {
        viewModelScope.launch(Dispatchers.IO) {
            val group = storageVolumeRepository.getVolumesOnSameDisk(volume)
            val allSucceeded = group.all { storageVolumeRepository.tryUnmount(it) }
            storageVolumeRepository.refresh()
            _ejectResultEvent.emit(EjectResult(volume.name, !allSucceeded))
        }
    }

    fun showFormatDialog(volume: StorageVolumeInfo) {
        _uiState.update { it.copy(pendingFormatVolume = volume) }
    }

    fun dismissFormatDialog() {
        _uiState.update { it.copy(pendingFormatVolume = null) }
    }

    fun confirmFormat(volume: StorageVolumeInfo, @Suppress("UNUSED_PARAMETER") formatType: FormatType) {
        _uiState.update { it.copy(pendingFormatVolume = null) }
        viewModelScope.launch { _formatVolumeEvent.emit(volume) }
    }

    fun getStorageVolumeForPath(path: String): android.os.storage.StorageVolume? =
        storageVolumeRepository.getStorageVolume(path)
}