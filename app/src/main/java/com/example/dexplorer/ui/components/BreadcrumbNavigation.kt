package com.example.dexplorer.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun BreadcrumbNavigation(
    currentPath: String,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val segments = currentPath.split("/").filter { it.isNotEmpty() }

    Row(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Root segment
        Text(
            text = "/",
            style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .clickable { onNavigate("/") }
                .padding(4.dp)
        )

        // Path segments
        segments.forEachIndexed { index, segment ->
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp)
            )

            val isLast = index == segments.lastIndex
            val segmentPath = "/" + segments.subList(0, index + 1).joinToString("/")

            Text(
                text = segment,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (isLast) FontWeight.Bold else FontWeight.Medium
                ),
                color = if (isLast) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.primary
                },
                modifier = Modifier
                    .clickable(enabled = !isLast) {
                        onNavigate(segmentPath)
                    }
                    .padding(4.dp)
            )
        }
    }
}