package com.example.dexplorer.ui.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.LayoutCoordinates
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class DragSelectState {
    var isDragging by mutableStateOf(false)
    var startPos by mutableStateOf(Offset.Zero)
    var currentPos by mutableStateOf(Offset.Zero)
    val itemRects = HashMap<String, Rect>()
    var containerCoordinates: LayoutCoordinates? = null

    val selectionRect: Rect?
        get() {
            if (!isDragging) return null
            val w = abs(currentPos.x - startPos.x)
            val h = abs(currentPos.y - startPos.y)
            if (w < 8f && h < 8f) return null
            return Rect(
                min(startPos.x, currentPos.x),
                min(startPos.y, currentPos.y),
                max(startPos.x, currentPos.x),
                max(startPos.y, currentPos.y)
            )
        }

    fun getSelectedPaths(): Set<String> =
        selectionRect?.let { rect ->
            itemRects.filter { (_, r) -> rect.overlaps(r) }.keys.toSet()
        } ?: emptySet()

    fun updateItemRect(path: String, itemCoords: LayoutCoordinates) {
        val containerCoords = containerCoordinates ?: return
        try {
            val localPos = containerCoords.localPositionOf(itemCoords, Offset.Zero)
            itemRects[path] = Rect(
                localPos.x, localPos.y,
                localPos.x + itemCoords.size.width,
                localPos.y + itemCoords.size.height
            )
        } catch (_: Exception) { }
    }
}

val LocalDragSelectState = staticCompositionLocalOf<DragSelectState?> { null }
