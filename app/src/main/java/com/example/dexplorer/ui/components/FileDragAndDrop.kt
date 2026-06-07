package com.example.dexplorer.ui.components

import android.content.ClipData
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Point
import android.graphics.Rect
import android.view.View
import androidx.compose.foundation.draganddrop.dragAndDropTarget
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draganddrop.DragAndDropEvent
import androidx.compose.ui.draganddrop.DragAndDropTarget
import androidx.compose.ui.draganddrop.toAndroidDragEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.content.FileProvider
import com.example.dexplorer.core.util.FileLogger
import com.example.dexplorer.data.model.FileItem
import java.io.File

const val DEXPLORER_MOVE_MIME = "application/x-dexplorer-move"

/** Provides the current selection set to drag sources deeper in the tree. */
val LocalSelectedPaths = compositionLocalOf<Set<String>> { emptySet() }

/** Provides pin/unpin handlers to file context menus without parameter threading. */
data class PinHandlers(
    val isPinned: (path: String) -> Boolean,
    val onToggle: (path: String) -> Unit,
    val isProtected: (path: String) -> Boolean = { false }
)
val LocalPinHandlers = compositionLocalOf<PinHandlers?> { null }

fun Modifier.fileItemDragSource(file: FileItem): Modifier = this.composed {
    val context = LocalContext.current
    val view = LocalView.current
    // Always read the latest selection without restarting the pointerInput coroutine.
    val selectedPathsRef = rememberUpdatedState(LocalSelectedPaths.current)

    val startDrag = remember(file) {
        {
            val currentSelected = selectedPathsRef.value
            val pathsToDrag: List<String> =
                if (file.path in currentSelected && currentSelected.size > 1)
                    currentSelected.toList()
                else
                    listOf(file.path)

            val firstFile = File(pathsToDrag.first())
            if (!firstFile.exists()) {
                FileLogger.log("DRAG: source missing '${file.name}'")
            } else {
                val label = if (pathsToDrag.size == 1) file.name else "${pathsToDrag.size} items"
                val clip = ClipData(
                    label,
                    arrayOf(DEXPLORER_MOVE_MIME),
                    ClipData.Item(pathsToDrag.joinToString("\n"))
                )
                if (!file.isDirectory && pathsToDrag.size == 1) {
                    runCatching {
                        val uri = FileProvider.getUriForFile(
                            context, "${context.packageName}.fileprovider", firstFile
                        )
                        clip.addItem(ClipData.Item(uri))
                    }
                }
                val ok = view.startDragAndDrop(
                    clip,
                    FileDragShadowBuilder(file, pathsToDrag.size),
                    null,
                    View.DRAG_FLAG_GLOBAL or View.DRAG_FLAG_GLOBAL_URI_READ
                )
                FileLogger.log("DRAG: '${file.name}' count=${pathsToDrag.size} ok=$ok")
            }
        }
    }

    pointerInput(file) {
        awaitEachGesture {
            val down = awaitFirstDown(requireUnconsumed = false)
            if (down.type != PointerType.Mouse) return@awaitEachGesture
            val slop = viewConfiguration.touchSlop
            while (true) {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val change = event.changes.firstOrNull { it.id == down.id } ?: break
                if (!change.pressed) break
                val dx = change.position.x - down.position.x
                val dy = change.position.y - down.position.y
                if (dx * dx + dy * dy > slop * slop) {
                    change.consume()
                    startDrag()
                    break
                }
            }
        }
    }
}

private class FileDragShadowBuilder(
    private val file: FileItem,
    private val count: Int = 1
) : View.DragShadowBuilder() {
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 44f
        color = android.graphics.Color.WHITE
    }
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.parseColor("#CC1976D2")
        style = Paint.Style.FILL
    }
    private val bounds = Rect()
    private val pad = 24

    override fun onProvideShadowMetrics(size: Point, touch: Point) {
        val label = if (count > 1) "$count items" else file.name.take(30)
        textPaint.getTextBounds(label, 0, label.length, bounds)
        val w = (bounds.width() + pad * 2).coerceAtLeast(200)
        val h = (bounds.height() + pad * 2).coerceAtLeast(60)
        size.set(w, h)
        touch.set(w / 2, h / 2)
    }

    override fun onDrawShadow(canvas: Canvas) {
        val w = canvas.width.toFloat()
        val h = canvas.height.toFloat()
        canvas.drawRoundRect(0f, 0f, w, h, 16f, 16f, bgPaint)
        val label = if (count > 1) "$count items" else file.name.take(30)
        textPaint.getTextBounds(label, 0, label.length, bounds)
        canvas.drawText(label, pad.toFloat(), h / 2f + bounds.height() / 2f, textPaint)
    }
}

fun Modifier.folderDropTarget(
    enabled: Boolean,
    tag: String = "",
    onHoverChange: (Boolean) -> Unit,
    onDrop: (List<String>) -> Unit
): Modifier {
    if (!enabled) return this
    // composed{} gives a per-node scope so remember/rememberUpdatedState work.
    // The DragAndDropTarget MUST be the same object across recompositions: a new object
    // triggers update() on the internal node, which resets session state and breaks in-flight drops.
    return this.composed {
        val onHoverRef = rememberUpdatedState(onHoverChange)
        val onDropRef  = rememberUpdatedState(onDrop)
        val tagRef     = rememberUpdatedState(tag)

        val stableTarget = remember {
            object : DragAndDropTarget {
                override fun onDrop(event: DragAndDropEvent): Boolean {
                    onHoverRef.value(false)
                    val pathsText = event.toAndroidDragEvent().clipData
                        ?.getItemAt(0)?.text?.toString()
                        ?: run { FileLogger.log("FOLDER_DROP: '${tagRef.value}' clipData null"); return false }
                    val paths = pathsText.split("\n").filter { it.isNotEmpty() }
                    FileLogger.log("FOLDER_DROP: '${tagRef.value}' paths=$paths")
                    onDropRef.value(paths)
                    return true
                }
                override fun onEntered(event: DragAndDropEvent) {
                    FileLogger.log("HOVER_ENTER: '${tagRef.value}'")
                    onHoverRef.value(true)
                }
                override fun onExited(event: DragAndDropEvent) {
                    FileLogger.log("HOVER_EXIT: '${tagRef.value}'")
                    onHoverRef.value(false)
                }
                override fun onEnded(event: DragAndDropEvent) {
                    FileLogger.log("DRAG_ENDED: '${tagRef.value}'")
                    onHoverRef.value(false)
                }
            }
        }

        val stableShouldAccept: (DragAndDropEvent) -> Boolean = remember {
            { event ->
                val accepts = event.toAndroidDragEvent().clipDescription
                    ?.hasMimeType(DEXPLORER_MOVE_MIME) == true
                FileLogger.log("SHOULD_ACCEPT: '${tagRef.value}' accepts=$accepts")
                accepts
            }
        }

        dragAndDropTarget(shouldStartDragAndDrop = stableShouldAccept, target = stableTarget)
    }
}
