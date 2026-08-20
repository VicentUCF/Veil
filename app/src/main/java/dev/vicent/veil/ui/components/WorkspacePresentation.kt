package dev.vicent.veil.ui.components

import androidx.compose.ui.unit.dp

/** Product-level geometry shared by the non-CURRENT workspace dashboards. */
internal object WorkspaceLayoutTokens {
    val SECTION_SPACING = 10.dp
    val PRIMARY_TILE_HEIGHT = 220.dp
    val SECONDARY_TILE_HEIGHT = 154.dp
    val COMPACT_BREAKPOINT = 328.dp
}

internal fun formatDuration(millis: Long): String {
    val totalSeconds = (millis / 1_000).coerceAtLeast(0)
    return "%02d:%02d".format(totalSeconds / 60, totalSeconds % 60)
}
