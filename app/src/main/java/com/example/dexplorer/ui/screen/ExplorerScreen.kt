package com.example.dexplorer.ui.screen

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
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
import com.example.dexplorer.data.model.FileItem
import com.example.dexplorer.ui.components.BreadcrumbNavigation
import com.example.dexplorer.ui.components.ConflictResolutionDialog
import com.example.dexplorer.ui.screen.ConflictResolution
import com.example.dexplorer.ui.components.DeleteConfirmationDialog
import com.example.dexplorer.ui.components.ExtensionWarningDialog
import com.example.dexplorer.ui.components.FileListView
import com.example.dexplorer.ui.components.FilePropertiesDialog
import com.example.dexplorer.ui.components.KeyboardShortcutScope
import com.example.dexplorer.ui.components.KeyboardShortcuts
import com.example.dexplorer.ui.components.NewFolderDialog
import com.example.dexplorer.ui.components.RenameDialog
import com.example.dexplorer.ui.components.TopToolbar
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.key
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import android.view.ViewTreeObserver
import androidx.activity.compose.BackHandler


import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row

import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Snackbar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.platform.LocalContext
import com.example.dexplorer.domain.usecase.FolderStats
import kotlinx.coroutines.launch

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.dexplorer.core.util.AppInfo
import com.example.dexplorer.core.util.DefaultAppResolver
import com.example.dexplorer.core.util.FileLogger
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.Saver
import androidx.work.Configuration
import androidx.compose.runtime.mutableIntStateOf
import android.view.View
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.VerticalDivider
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import android.content.ActivityNotFoundException
import android.os.Build
import android.os.storage.StorageManager
import android.widget.Toast
import androidx.compose.material3.SnackbarResult
import com.example.dexplorer.data.model.FormatType
import com.example.dexplorer.ui.components.ExtractToDialog
import com.example.dexplorer.ui.components.FormatVolumeDialog
import com.example.dexplorer.ui.components.NavigationRailPanel
import com.example.dexplorer.ui.components.SidebarPanel
import android.net.Uri
import androidx.compose.foundation.border
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.isSecondaryPressed
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.DpOffset
import com.example.dexplorer.ui.components.EmptySpaceContextMenu
import com.example.dexplorer.ui.components.NewFileDialog
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.layout.onGloballyPositioned
import com.example.dexplorer.ui.components.DragSelectState
import com.example.dexplorer.ui.components.LocalDragSelectState
import com.example.dexplorer.ui.components.LocalSelectedPaths
import com.example.dexplorer.ui.components.LocalPinHandlers
import com.example.dexplorer.ui.components.PinHandlers
import com.example.dexplorer.ui.components.DEXPLORER_MOVE_MIME

val fileItemSaver = run {
    val pathKey = "path"
    val nameKey = "name"
    val mimeTypeKey = "mimeType"
    val extensionKey = "extension"

    Saver<FileItem?, Map<String, Any?>>(
        save = { fileItem ->
            if (fileItem == null) {
                emptyMap()
            } else {
                mapOf(
                    pathKey to fileItem.path,
                    nameKey to fileItem.name,
                    mimeTypeKey to fileItem.mimeType,
                    extensionKey to fileItem.extension
                )
            }
        },
        restore = { map ->
            if (map.isEmpty()) {
                null
            } else {
                FileItem(
                    name = map[nameKey] as String,
                    path = map[pathKey] as String,
                    size = 0,
                    lastModified = 0,
                    isDirectory = false,
                    mimeType = map[mimeTypeKey] as? String,
                    extension = map[extensionKey] as? String ?: "",
                    permissions = null,
                    thumbnailPath = null,
                    defaultAppIcon = null
                )
            }
        }
    )
}
@SuppressLint("LocalContextConfigurationRead", "LocalContextResourcesRead", "WrongConstant")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3WindowSizeClassApi::class)
@Composable
fun ExplorerScreen(
    viewModel: ExplorerViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    val snackbarHostState = remember { SnackbarHostState() }
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val windowSizeClass = calculateWindowSizeClass(context as Activity)
    val isExpandedWidth = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Expanded
    val isMediumWidth = windowSizeClass.widthSizeClass == WindowWidthSizeClass.Medium
    val folderStatsMap by viewModel.folderStats.collectAsState()
    val calculatingFolderPaths by viewModel.calculatingFolderPaths.collectAsState()
    var defaultAppNameForDialog by remember { mutableStateOf<String?>(null) }
    val fileWaitingForDefaultChange by viewModel.fileWaitingForDefaultChange.collectAsState()
    val fileWaitingForBadgeTracking by viewModel.fileWaitingForBadgeTracking.collectAsState()
    var lifecycleEventCount by remember { mutableIntStateOf(0) }
    var lastEventTimestamp by remember { mutableLongStateOf(0L) }

    LaunchedEffect(uiState.selectedItems) {
        FileLogger.log("SCREEN: selectedItems=${uiState.selectedItems.map { it.substringAfterLast('/') }}")
    }

    val isAtRoot = uiState.currentPath == "/storage/emulated/0" || uiState.currentPath.isEmpty()
    BackHandler(enabled = !isAtRoot) {
        viewModel.navigateUp()
    }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showExtensionWarning by remember { mutableStateOf(false) }
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showPropertiesDialog by remember { mutableStateOf(false) }
    var showOpenWithDialog by remember { mutableStateOf(false) }
    var openWithFile by remember { mutableStateOf<FileItem?>(null) }
    var availableApps by remember { mutableStateOf<List<AppInfo>>(emptyList()) }
    var pendingRename by remember { mutableStateOf<Pair<String, String>?>(null) }
    var propertiesFile by remember { mutableStateOf<FileItem?>(null) }
    var extractToFile by remember { mutableStateOf<FileItem?>(null) }
    var showEmptySpaceMenu by remember { mutableStateOf(false) }
    var emptySpaceMenuOffset by remember { mutableStateOf(DpOffset.Zero) }
    var newFileDefaultName by remember { mutableStateOf<String?>(null) }
    val density = LocalDensity.current

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

    LaunchedEffect(Unit) {
        viewModel.ejectResultEvent.collect { result ->
            if (result.needsManualEject) {
                // Programmatic unmount failed — guide the user to the system notification.
                // Toast stays visible over the open panel so they know which device to tap
                // when multiple drives are connected.
                Toast.makeText(
                    context,
                    "Tap \"${result.volumeName}\" in the notification panel to safely eject",
                    Toast.LENGTH_LONG
                ).show()
                try {
                    val statusBar = context.getSystemService("statusbar")
                    statusBar?.javaClass?.getMethod("expandNotificationsPanel")?.invoke(statusBar)
                } catch (_: Exception) { }
            }
            // If all partitions were unmounted via reflection, StorageVolumeCallback already
            // updated the sidebar — nothing else needed.
        }
    }

    LaunchedEffect(Unit) {
        viewModel.formatVolumeEvent.collect { volume ->
            val sv = viewModel.getStorageVolumeForPath(volume.path)
            try {
                val formatIntent = Intent("android.os.storage.action.FORMAT_AS_PORTABLE")
                formatIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (sv != null) {
                    formatIntent.putExtra("android.os.storage.extra.STORAGE_VOLUME", sv)
                }
                context.startActivity(formatIntent)
            } catch (_: ActivityNotFoundException) {
                val action = snackbarHostState.showSnackbar(
                    message = "Format not supported on this device",
                    actionLabel = "Settings"
                )
                if (action == SnackbarResult.ActionPerformed) {
                    val settingsIntent = Intent(StorageManager.ACTION_MANAGE_STORAGE)
                    settingsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(settingsIntent)
                }
            }
        }
    }
    val view = LocalView.current
    DisposableEffect(view) {
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            val rect = android.graphics.Rect()
            view.getWindowVisibleDisplayFrame(rect)
            val screenHeight = view.rootView.height
            val keypadHeight = screenHeight - rect.bottom

            // If keyboard is hidden (keypad height is less than 15% of screen)
            if (keypadHeight < screenHeight * 0.15) {
                // Keyboard is closed - clear focus
                focusManager.clearFocus()
            }
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(listener)

        onDispose {
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }
    DisposableEffect(uiState.searchQuery) {
        if (uiState.searchQuery.isEmpty()) {
            focusManager.clearFocus()
        }
        onDispose { }
    }


    // Add tracking variables


    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            val now = System.currentTimeMillis()
            val timeSinceLastEvent = now - lastEventTimestamp
            lifecycleEventCount++
            lastEventTimestamp = now

            FileLogger.log("========================================")
            FileLogger.log("=== LIFECYCLE EVENT #$lifecycleEventCount ===")
            FileLogger.log("Event: $event")
            FileLogger.log("Time since last event: ${timeSinceLastEvent}ms")
            FileLogger.log("Timestamp: $now")

            // Samsung DeX specific detection
            val isDeXMode = try {
                val semDesktopModeState = context.resources.configuration.javaClass
                    .getField("SEM_DESKTOP_MODE_ENABLED")
                    .getInt(context.resources.configuration)
                val desktopModeEnabled = context.resources.configuration.javaClass
                    .getField("semDesktopModeEnabled")
                    .getInt(context.resources.configuration)
                desktopModeEnabled == semDesktopModeState
            } catch (e: Exception) {
                // Fallback: check screen size
                val metrics = context.resources.displayMetrics
                (metrics.widthPixels >= 1920 && metrics.heightPixels >= 1080)
            }
            FileLogger.log("DeX Mode: $isDeXMode")

            // Log window state
            val activity = context as? android.app.Activity
            if (activity != null) {
                FileLogger.log("Activity hasWindowFocus: ${activity.hasWindowFocus()}")
                FileLogger.log("Activity isInMultiWindowMode: ${activity.isInMultiWindowMode}")
                FileLogger.log("Activity isInPictureInPictureMode: ${activity.isInPictureInPictureMode}")
                FileLogger.log("Activity isFinishing: ${activity.isFinishing}")
                FileLogger.log("Activity isDestroyed: ${activity.isDestroyed}")
            }

            // Log our tracking state
            FileLogger.log("fileWaitingForDefaultChange: ${fileWaitingForDefaultChange?.name}")
            FileLogger.log("fileWaitingForBadgeTracking: ${fileWaitingForBadgeTracking?.name}")

            when (event) {
                Lifecycle.Event.ON_CREATE -> {
                    FileLogger.log(">>> ON_CREATE - Activity being created")
                }
                Lifecycle.Event.ON_START -> {
                    FileLogger.log(">>> ON_START - Activity becoming visible")
                }
                Lifecycle.Event.ON_RESUME -> {
                    FileLogger.log(">>> ON_RESUME - Activity gaining focus")

                    // Check what apps are currently running
                    val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
                    val runningApps = activityManager.runningAppProcesses
                    FileLogger.log("Currently running processes:")
                    runningApps?.take(10)?.forEach { processInfo ->
                        FileLogger.log("  - ${processInfo.processName} (importance: ${processInfo.importance})")
                    }

                    // Your existing stage 1/2 logic here...
                    if (fileWaitingForBadgeTracking != null) {
                        // Stage 2 logic...
                    } else if (fileWaitingForDefaultChange != null) {
                        // Stage 1 logic...
                    } else {
                        FileLogger.log(">>> No files waiting (normal resume)")
                    }
                }
                Lifecycle.Event.ON_PAUSE -> {
                    FileLogger.log(">>> ON_PAUSE - Activity losing focus")

                    // Log what's happening
                    if (activity != null && activity.isChangingConfigurations) {
                        FileLogger.log("  Reason: Configuration change")
                    } else if (activity != null && activity.isFinishing) {
                        FileLogger.log("  Reason: Activity finishing")
                    } else {
                        FileLogger.log("  Reason: Another app/window taking focus")
                    }
                }
                Lifecycle.Event.ON_STOP -> {
                    FileLogger.log(">>> ON_STOP - Activity no longer visible")
                }
                Lifecycle.Event.ON_DESTROY -> {
                    FileLogger.log(">>> ON_DESTROY - Activity being destroyed")
                }
                else -> {
                    FileLogger.log(">>> Other event: $event")
                }
            }

            FileLogger.log("========================================")
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            FileLogger.log("Lifecycle observer disposed")
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }


    DisposableEffect(view) {
        val focusListener = View.OnFocusChangeListener { v, hasFocus ->
            FileLogger.log("========================================")
            FileLogger.log("=== WINDOW FOCUS CHANGE ===")
            FileLogger.log("Has focus: $hasFocus")
            FileLogger.log("Timestamp: ${System.currentTimeMillis()}")
            FileLogger.log("View: ${v.javaClass.simpleName}")

            if (hasFocus) {
                FileLogger.log("DExplorer gained window focus")

                // Check what changed while we were away
                FileLogger.log("Checking state after gaining focus...")
                FileLogger.log("fileWaitingForDefaultChange: ${fileWaitingForDefaultChange?.name}")
                FileLogger.log("fileWaitingForBadgeTracking: ${fileWaitingForBadgeTracking?.name}")
            } else {
                FileLogger.log("DExplorer lost window focus")
            }

            FileLogger.log("========================================")
        }

        view.onFocusChangeListener = focusListener

        onDispose {
            view.onFocusChangeListener = null
        }
    }

    // Keyboard shortcuts
    val shortcuts = KeyboardShortcuts(
        onCopy = {
            if (uiState.selectedItems.isNotEmpty()) {
                viewModel.copyToClipboard()
            }
        },
        onCut = {
            if (uiState.selectedItems.isNotEmpty()) {
                viewModel.cutToClipboard()
            }
        },
        onPaste = {
            if (uiState.clipboard.operation != ClipboardOperation.NONE) {
                viewModel.paste()
            }
        },
        onDelete = {
            if (uiState.selectedItems.isNotEmpty()) {
                showDeleteDialog = true
            }
        },
        onRename = {
            if (uiState.selectedItems.size == 1) {
                showRenameDialog = true
            }
        },
        onSelectAll = { viewModel.selectAll() },
        onRefresh = { viewModel.refreshCurrentDirectory() },
        onNewFolder = { showNewFolderDialog = true },
        onBack = { viewModel.navigateUp() }
    )

    KeyboardShortcutScope(shortcuts = shortcuts) { keyModifier ->
        Scaffold(
            modifier = keyModifier.focusable(),
            topBar = {
                TopAppBar(
                    title = { Text(if (uiState.isSearching) "Search Results" else viewModel.getCurrentFolderName()) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                        titleContentColor = MaterialTheme.colorScheme.onSurface
                    ),navigationIcon = {
                        Row(
                            modifier = if (isExpandedWidth) Modifier.width(220.dp) else Modifier,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(
                                onClick = { viewModel.navigateBack() },
                                enabled = uiState.canNavigateBack
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Navigate back",
                                    tint = if (uiState.canNavigateBack) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    }
                                )
                            }
                            IconButton(
                                onClick = { viewModel.navigateForward() },
                                enabled = uiState.canNavigateForward
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Navigate forward",
                                    tint = if (uiState.canNavigateForward) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    }
                                )
                            }
                            IconButton(
                                onClick = { viewModel.navigateUp() },
                                enabled = !isAtRoot
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ArrowUpward,
                                    contentDescription = "Navigate to parent directory",
                                    tint = if (!isAtRoot) {
                                        MaterialTheme.colorScheme.onSurface
                                    } else {
                                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                                    }
                                )
                            }
                            IconButton(onClick = { viewModel.refreshCurrentDirectory() }) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Refresh"
                                )
                            }
                        }
                    }
                )
            },
            snackbarHost = {
                SnackbarHost(
                    hostState = snackbarHostState,
                    snackbar = { data ->
                        Snackbar(
                            snackbarData = data,
                            containerColor = MaterialTheme.colorScheme.inverseSurface,
                            contentColor = MaterialTheme.colorScheme.inverseOnSurface
                        )
                    }
                )
            }
        ) { paddingValues ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                when {
                    isExpandedWidth -> {
                        SidebarPanel(
                            currentPath = uiState.currentPath,
                            pinnedFolders = uiState.pinnedFolders,
                            storageVolumes = uiState.storageVolumes,
                            onNavigate = { viewModel.navigateToDirectory(it) },
                            onUnpin = { viewModel.unpinFolder(it) },
                            isProtected = { viewModel.isFolderProtected(it) },
                            onVolumeClick = { viewModel.navigateToDirectory(it.path) },
                            onEjectVolume = { viewModel.ejectVolume(it) },
                            onFormatVolume = { viewModel.showFormatDialog(it) }
                        )
                        VerticalDivider()
                    }
                    isMediumWidth -> {
                        NavigationRailPanel(
                            currentPath = uiState.currentPath,
                            pinnedFolders = uiState.pinnedFolders,
                            storageVolumes = uiState.storageVolumes,
                            onNavigate = { viewModel.navigateToDirectory(it) },
                            onUnpin = { viewModel.unpinFolder(it) },
                            isProtected = { viewModel.isFolderProtected(it) },
                            onVolumeClick = { viewModel.navigateToDirectory(it.path) },
                            onEjectVolume = { viewModel.ejectVolume(it) },
                            onFormatVolume = { viewModel.showFormatDialog(it) }
                        )
                        VerticalDivider()
                    }
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            focusManager.clearFocus()
                        }
                ) {
                HorizontalDivider()
                // Combined Breadcrumb + Search Bar Row - MOVE THIS FIRST
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Breadcrumb Navigation - FILL AVAILABLE SPACE
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 8.dp)  // ADD THIS - 8dp gap before search bar
                    ) {
                        BreadcrumbNavigation(
                            currentPath = uiState.currentPath,
                            onNavigate = { path -> viewModel.navigateToDirectory(path) }
                        )
                    }

                    // Remove the Spacer - we're using padding instead
                    // Spacer(modifier = Modifier.width(16.dp))  // DELETE THIS

                    // Search TextField (fixed width)

                    key("search_${uiState.currentPath}") {
                        TextField(
                            value = uiState.searchQuery,
                            onValueChange = { query ->
                                viewModel.search(query)
                            },
                            placeholder = { Text("Search...") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Search",
                                    modifier = Modifier.size(20.dp)
                                )
                            },
                            trailingIcon = {
                                if (uiState.searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { viewModel.clearSearch() }) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Clear search",
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier
                                .width(250.dp)
                                .height(56.dp),  // ADD THIS - standard Material 3 TextField height
                            textStyle = MaterialTheme.typography.bodyMedium
                        )
                    }
                }

                // Search hint — shown 2s after typing fewer than 3 characters
                if (uiState.searchTooShort) {
                    Text(
                        text = "Type at least 3 characters to search",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.End,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 16.dp, bottom = 4.dp)
                    )
                }

                // Search scope toggle — visible only while search bar has text
                if (uiState.searchQuery.isNotEmpty()) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 2.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Search in:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        FilterChip(
                            selected = uiState.searchScope == SearchScope.CURRENT_FOLDER,
                            onClick = { viewModel.setSearchScope(SearchScope.CURRENT_FOLDER) },
                            label = { Text("This folder") }
                        )
                        FilterChip(
                            selected = uiState.searchScope == SearchScope.ENTIRE_DEVICE,
                            onClick = { viewModel.setSearchScope(SearchScope.ENTIRE_DEVICE) },
                            label = { Text("Entire device") }
                        )
                    }
                }

                // Persistent Toolbar - MOVE THIS SECOND
                TopToolbar(
                    hasSelection = uiState.selectedItems.isNotEmpty(),
                    hasSingleSelection = uiState.selectedItems.size == 1,
                    hasClipboard = uiState.clipboard.operation != ClipboardOperation.NONE,
                    selectedCount = uiState.selectedItems.size,
                    currentViewMode = uiState.viewMode,  // ADD THIS
                    onCopy = { viewModel.copyToClipboard() },
                    onCut = { viewModel.cutToClipboard() },
                    onPaste = { viewModel.paste() },
                    onDelete = { showDeleteDialog = true },
                    onRename = { showRenameDialog = true },
                    onNewFolder = { showNewFolderDialog = true },
                    onViewModeChange = { mode -> viewModel.setViewMode(mode) }  // ADD THIS
                )


                when {
                    uiState.isLoading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                color = MaterialTheme.colorScheme.primary
                            )
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
                        val dragSelectState = remember { DragSelectState() }
                        val isDragOverState = remember { mutableStateOf(false) }
                        val isDragOver by isDragOverState
                        CompositionLocalProvider(LocalDragSelectState provides dragSelectState) {
                        val dropTarget = remember {
                            object : DragAndDropTarget {
                                override fun onDrop(event: DragAndDropEvent): Boolean {
                                    isDragOverState.value = false
                                    val androidEvent = event.toAndroidDragEvent()
                                    val clipData = androidEvent.clipData ?: return false
                                    val clipDesc = androidEvent.clipDescription
                                    if (clipDesc?.hasMimeType(DEXPLORER_MOVE_MIME) == true) {
                                        val pathsText = clipData.getItemAt(0)?.text?.toString()
                                            ?: return false
                                        val sourcePaths = pathsText.split("\n").filter { it.isNotEmpty() }
                                        val destDir = viewModel.uiState.value.currentPath
                                        val toMove = sourcePaths.filter { java.io.File(it).parent != destDir }
                                        if (toMove.isNotEmpty()) {
                                            viewModel.moveFilesToDirectory(toMove, destDir)
                                        }
                                        return true
                                    }
                                    val uris = (0 until clipData.itemCount)
                                        .mapNotNull { clipData.getItemAt(it).uri }
                                    if (uris.isEmpty()) return false
                                    viewModel.handleDroppedFiles(uris, context)
                                    return true
                                }
                                override fun onEntered(event: DragAndDropEvent) {
                                    isDragOverState.value = true
                                }
                                override fun onExited(event: DragAndDropEvent) {
                                    isDragOverState.value = false
                                }
                                override fun onEnded(event: DragAndDropEvent) {
                                    isDragOverState.value = false
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .then(
                                    if (isDragOver) Modifier.border(
                                        2.dp,
                                        MaterialTheme.colorScheme.primary
                                    ) else Modifier
                                )
                                .dragAndDropTarget(
                                    shouldStartDragAndDrop = { event ->
                                        // Only accept external drops; internal moves go to folderDropTarget
                                        event.toAndroidDragEvent().clipDescription
                                            ?.hasMimeType(DEXPLORER_MOVE_MIME) != true
                                    },
                                    target = dropTarget
                                )
                                .onGloballyPositioned { coords ->
                                    dragSelectState.containerCoordinates = coords
                                }
                                .pointerInput(Unit) {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            if (event.type == PointerEventType.Press &&
                                                event.buttons.isSecondaryPressed &&
                                                event.changes.none { it.isConsumed }
                                            ) {
                                                val pos = event.changes.first().position
                                                emptySpaceMenuOffset = with(density) {
                                                    DpOffset(pos.x.toDp(), pos.y.toDp())
                                                }
                                                showEmptySpaceMenu = true
                                            }
                                        }
                                    }
                                }
                                .pointerInput("dragSelect") {
                                    awaitPointerEventScope {
                                        while (true) {
                                            val pressEvent = awaitPointerEvent()
                                            if (pressEvent.type != PointerEventType.Press) continue
                                            if (pressEvent.buttons.isSecondaryPressed) continue
                                            val firstChange = pressEvent.changes.firstOrNull() ?: continue
                                            if (firstChange.isConsumed) continue
                                            if (firstChange.type != PointerType.Mouse) continue
                                            // Don't start rubber-band when clicking directly on a file item
                                            val clickPos = firstChange.position
                                            if (dragSelectState.itemRects.values.any { it.contains(clickPos) }) continue
                                            dragSelectState.startPos = clickPos
                                            dragSelectState.currentPos = clickPos
                                            dragSelectState.isDragging = true
                                            while (dragSelectState.isDragging) {
                                                val event = awaitPointerEvent()
                                                when (event.type) {
                                                    PointerEventType.Move -> {
                                                        event.changes.firstOrNull()?.let { change ->
                                                            dragSelectState.currentPos = change.position
                                                            // Only update selection when rect is large enough to be intentional
                                                            if (dragSelectState.selectionRect != null) {
                                                                viewModel.setSelection(dragSelectState.getSelectedPaths())
                                                            }
                                                        }
                                                    }
                                                    PointerEventType.Release -> {
                                                        if (dragSelectState.selectionRect == null) {
                                                            viewModel.clearSelection()
                                                        } else {
                                                            viewModel.setSelection(dragSelectState.getSelectedPaths())
                                                        }
                                                        dragSelectState.isDragging = false
                                                    }
                                                    else -> {}
                                                }
                                            }
                                        }
                                    }
                                }
                        ) {
                        CompositionLocalProvider(
                            LocalSelectedPaths provides uiState.selectedItems,
                            LocalPinHandlers provides PinHandlers(
                                isPinned = { path -> viewModel.isFolderPinned(path) },
                                onToggle = { path ->
                                    if (viewModel.isFolderPinned(path)) viewModel.unpinFolder(path)
                                    else viewModel.pinFolder(path)
                                },
                                isProtected = { path -> viewModel.isFolderProtected(path) }
                            )
                        ) {
                        FileListView(
                            files = uiState.items,
                            selectedItems = uiState.selectedItems,
                            viewMode = uiState.viewMode,
                            isSearching = uiState.isSearching,
                            onItemClick = { file ->
                                viewModel.toggleItemSelection(file.path)
                            },
                            onItemCtrlClick = { file ->
                                viewModel.toggleItemSelectionMulti(file.path)
                            },
                            onItemDoubleClick = { file ->
                                if (file.isDirectory) {
                                    viewModel.navigateToDirectory(file.path)
                                }
                            },
                            onOpen = { file ->
                                viewModel.openFile(file)
                            },
                            onOpenWith = { file ->
                                FileLogger.log("========================================")
                                FileLogger.log("Open with: ${file.name}")

                                // Remember this file for auto-tracking (use ViewModel)
                                viewModel.setFileWaitingForDefaultChange(file)

                                // Open file with Android's system chooser (shows "Always" and "Just once")
                                viewModel.openFile(file)
                            },
                            onCopy = { file ->
                                if (file.path !in uiState.selectedItems) {
                                    viewModel.toggleItemSelection(file.path)
                                }
                                viewModel.copyToClipboard()
                            },
                            onCut = { file ->
                                if (file.path !in uiState.selectedItems) {
                                    viewModel.toggleItemSelection(file.path)
                                }
                                viewModel.cutToClipboard()
                            },
                            onRename = { file ->
                                if (file.path !in uiState.selectedItems) {
                                    viewModel.clearSelection()
                                    viewModel.toggleItemSelection(file.path)
                                }
                                showRenameDialog = true
                            },
                            onDelete = { file ->
                                if (file.path !in uiState.selectedItems) {
                                    viewModel.toggleItemSelection(file.path)
                                }
                                showDeleteDialog = true
                            },
                            onProperties = { file ->
                                propertiesFile = file
                                showPropertiesDialog = true
                            },
                            onCompress = { file -> viewModel.compress(file) },
                            onExtract = { file -> viewModel.extractArchive(file) },
                            onExtractTo = { file -> extractToFile = file },
                            onMoveToFolder = { destFolder, sourcePaths ->
                                viewModel.moveFilesToDirectory(sourcePaths, destFolder.path)
                            }
                        )
                        } // close CompositionLocalProvider(LocalSelectedPaths + LocalPinHandlers)

                        EmptySpaceContextMenu(
                            expanded = showEmptySpaceMenu,
                            offset = emptySpaceMenuOffset,
                            hasPasteItems = uiState.clipboard.items.isNotEmpty(),
                            isCurrentFolderPinned = viewModel.isFolderPinned(uiState.currentPath),
                            isCurrentFolderProtected = viewModel.isFolderProtected(uiState.currentPath),
                            onDismiss = { showEmptySpaceMenu = false },
                            onNewFolder = {
                                showEmptySpaceMenu = false
                                showNewFolderDialog = true
                            },
                            onNewFile = { defaultName ->
                                newFileDefaultName = defaultName
                            },
                            onSelectAll = { viewModel.selectAll() },
                            onPaste = { viewModel.paste() },
                            onRefresh = { viewModel.refreshCurrentDirectory() },
                            onTogglePinCurrentFolder = {
                                val path = uiState.currentPath
                                if (viewModel.isFolderPinned(path)) viewModel.unpinFolder(path)
                                else viewModel.pinFolder(path)
                            }
                        )
                        val selRect = dragSelectState.selectionRect
                        if (selRect != null) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                drawRect(
                                    color = androidx.compose.ui.graphics.Color(0xFF2196F3).copy(alpha = 0.15f),
                                    topLeft = Offset(selRect.left, selRect.top),
                                    size = Size(selRect.width, selRect.height)
                                )
                                drawRect(
                                    color = androidx.compose.ui.graphics.Color(0xFF2196F3).copy(alpha = 0.6f),
                                    topLeft = Offset(selRect.left, selRect.top),
                                    size = Size(selRect.width, selRect.height),
                                    style = Stroke(width = 1.dp.toPx())
                                )
                            }
                        }
                        } // close Box (drop target)
                        } // close CompositionLocalProvider
                    }
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
                        showRenameDialog = false
                        viewModel.renameFile(selectedFile.path, newName)
                    },
                    onDismiss = { showRenameDialog = false },
                    onExtensionWarning = { newName ->
                        pendingRename = selectedFile.path to newName
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

        newFileDefaultName?.let { defaultName ->
            NewFileDialog(
                defaultName = defaultName,
                onConfirm = { fileName ->
                    viewModel.createEmptyFile(fileName)
                    newFileDefaultName = null
                },
                onDismiss = { newFileDefaultName = null }
            )
        }

        uiState.pasteConflict?.let { conflictState ->
            ConflictResolutionDialog(
                conflict = conflictState.current,
                conflictIndex = conflictState.currentIndex + 1,
                totalConflicts = conflictState.conflicts.size,
                onReplace  = { applyToAll -> viewModel.resolveCurrentConflict(ConflictResolution.REPLACE,   applyToAll) },
                onKeepBoth = { applyToAll -> viewModel.resolveCurrentConflict(ConflictResolution.KEEP_BOTH, applyToAll) },
                onSkip     = { applyToAll -> viewModel.resolveCurrentConflict(ConflictResolution.SKIP,      applyToAll) },
                onCancel   = { viewModel.cancelPasteConflict() }
            )
        }

        // Properties dialog
        // Track which file is waiting for default change
        var fileWaitingForDefaultChange by remember { mutableStateOf<FileItem?>(null) }

// Properties dialog
        if (showPropertiesDialog && propertiesFile != null) {
            val context = LocalContext.current
            val currentFile = propertiesFile!!
            val folderStatsForDialog = folderStatsMap[currentFile.path]
            val isFolderSizeCalculating = currentFile.path in calculatingFolderPaths

            LaunchedEffect(currentFile.path) {
                if (currentFile.isDirectory) {
                    viewModel.calculateFolderSize(currentFile.path)
                } else {
                    defaultAppNameForDialog = viewModel.getSystemDefaultAppName(
                        context,
                        currentFile.path,
                        currentFile.mimeType
                    )
                }
            }

            FilePropertiesDialog(
                file = currentFile,
                folderStats = folderStatsForDialog,
                isCalculating = isFolderSizeCalculating,
                defaultAppName = defaultAppNameForDialog,
                onDismiss = {
                    if (currentFile.isDirectory) {
                        viewModel.cancelFolderSizeCalculation(currentFile.path)
                    }
                    showPropertiesDialog = false
                    propertiesFile = null
                    defaultAppNameForDialog = null
                },
                onChangeDefaultApp = if (propertiesFile?.isDirectory == false) {
                    {
                        FileLogger.log("========================================")
                        FileLogger.log("=== CHANGE DEFAULT APP BUTTON CLICKED ===")
                        FileLogger.log("Timestamp: ${System.currentTimeMillis()}")
                        FileLogger.log("File: ${propertiesFile!!.name}")
                        FileLogger.log("Has system default: ${defaultAppNameForDialog != null}")
                        FileLogger.log("System default name: $defaultAppNameForDialog")

                        val file = propertiesFile!!

                        // Samsung DeX specific detection
                        val isDeXMode = try {
                            val semDesktopModeState = context.resources.configuration.javaClass
                                .getField("SEM_DESKTOP_MODE_ENABLED")
                                .getInt(context.resources.configuration)
                            val desktopModeEnabled = context.resources.configuration.javaClass
                                .getField("semDesktopModeEnabled")
                                .getInt(context.resources.configuration)
                            desktopModeEnabled == semDesktopModeState
                        } catch (e: Exception) {
                            // Fallback: check screen size
                            val metrics = context.resources.displayMetrics
                            (metrics.widthPixels >= 1920 && metrics.heightPixels >= 1080)
                        }
                        FileLogger.log("DeX Mode: $isDeXMode")

                        if (defaultAppNameForDialog != null) {
                            val packageName = DefaultAppResolver.getDefaultAppPackage(
                                context, file.path, file.mimeType
                            )
                            FileLogger.log("Opening Settings for: $packageName")

                            // Your existing settings opening code...
                        } else {
                            FileLogger.log("No default set - will open file to show chooser")
                            viewModel.openFile(file)
                        }

                        showPropertiesDialog = false
                        propertiesFile = null
                        defaultAppNameForDialog = null

                        FileLogger.log("=== END CHANGE DEFAULT APP ===")
                        FileLogger.log("========================================")
                    }
                } else null,
            )
        }


        uiState.pendingFormatVolume?.let { volume ->
            FormatVolumeDialog(
                volume = volume,
                onConfirm = { formatType -> viewModel.confirmFormat(volume, formatType) },
                onDismiss = { viewModel.dismissFormatDialog() }
            )
        }

        extractToFile?.let { file ->
            ExtractToDialog(
                archivePath = file.path,
                currentPath = uiState.currentPath,
                onConfirm = { destination ->
                    viewModel.extractArchiveTo(file, destination)
                    extractToFile = null
                },
                onDismiss = { extractToFile = null }
            )
        }

    }
}