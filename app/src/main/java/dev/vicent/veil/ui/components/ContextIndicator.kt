package dev.vicent.veil.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.ui.theme.LocalVeilPalette

@Composable
fun ContextIndicator(
    kind: LauncherContextKind,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(width = 32.dp, height = 32.dp)
            .semantics {
                contentDescription = label
                selected = isActive
            }
            .clip(RoundedCornerShape(3.dp))
            .background(if (isActive) palette.accentActive.copy(alpha = .88f) else androidx.compose.ui.graphics.Color.Transparent)
            .clickable(role = Role.Tab, onClick = onClick)
            .drawBehind {
                if (!isActive) {
                    drawLine(
                        color = palette.divider,
                        start = Offset(size.width, size.height * .28f),
                        end = Offset(size.width, size.height * .72f),
                        strokeWidth = 1.dp.toPx(),
                    )
                }
            },
    ) {
        ActivityGlyph(
            kind = kind.activityGlyph(),
            size = 13.dp,
            isActive = isActive,
            color = if (isActive) palette.drawerBackground else palette.contentSecondary,
        )
    }
}
