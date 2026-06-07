package com.example.dexplorer.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp@Composable
fun BreadcrumbNavigation(
    currentPath: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Auto-scroll to end when path changes
    LaunchedEffect(currentPath) {
        scrollState.animateScrollTo(scrollState.maxValue)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)  // ADD THIS - exact same height as TextField
            .clip(MaterialTheme.shapes.medium)
            .background(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)  // Exact same as unfocused TextField
            )
            .horizontalScroll(scrollState)
            .padding(horizontal = 12.dp),  // Horizontal padding for content
        verticalAlignment = Alignment.CenterVertically
    ) {
        val pathSegments = currentPath.split("/").filter { it.isNotEmpty() }

        pathSegments.forEachIndexed { index, segment ->
            val isLast = index == pathSegments.size - 1
            val segmentPath = "/" + pathSegments.take(index + 1).joinToString("/")

            val canNavigate = when {
                segmentPath == "/storage" || segmentPath == "/storage/emulated" -> false
                else -> !isLast
            }

            TextButton(
                onClick = {
                    if (canNavigate) {
                        val targetPath = if (segmentPath == "/storage" || segmentPath == "/storage/emulated") {
                            "/storage/emulated/0"
                        } else {
                            segmentPath
                        }
                        onNavigate(targetPath)
                    }
                },
                enabled = canNavigate,
                modifier = Modifier.padding(0.dp),  // Remove default button padding
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 8.dp,
                    vertical = 4.dp
                )
            ) {
                Text(
                    text = when (segment) {
                        "storage" -> "Storage"
                        "emulated" -> "Emulated"
                        "0" -> "Internal Storage"
                        else -> segment
                    },
                    color = if (isLast) {
                        MaterialTheme.colorScheme.onSurface
                    } else if (canNavigate) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (!isLast) {
                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 2.dp)
                )
            }
        }
    }
}