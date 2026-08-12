package dev.vicent.veil.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawStyle
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.vicent.veil.ui.theme.LocalVeilPalette

@Composable
fun ContextIndicator(
    index: Int,
    label: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    val color = if (isActive) palette.accentActive else palette.contentMuted

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(width = 36.dp, height = 40.dp)
            .semantics {
                contentDescription = label
                selected = isActive
            }
            .clickable(role = Role.Tab, onClick = onClick),
    ) {
        Canvas(modifier = Modifier.size(width = 16.dp, height = 20.dp)) {
            val stroke = Stroke(width = 1.2.dp.toPx())
            val style: DrawStyle = if (isActive) Fill else stroke
            val center = Offset(size.width / 2f, size.height * 0.38f)
            val radius = 4.5.dp.toPx()

            when (index % 5) {
                0 -> drawDiamond(center, radius, color, style)
                1 -> drawCircle(color = color, radius = radius, center = center, style = style)
                2 -> drawCircle(color = color, radius = radius, center = center, style = style)
                3 -> drawTriangle(center, radius, color, style)
                else -> drawRect(
                    color = color,
                    topLeft = Offset(center.x - radius, center.y - radius),
                    size = Size(radius * 2f, radius * 2f),
                    style = style,
                )
            }

            if (index % 5 == 2 && !isActive) {
                drawCircle(
                    color = color,
                    radius = 1.dp.toPx(),
                    center = center,
                )
            }

            if (isActive) {
                drawLine(
                    color = color,
                    start = Offset(center.x - radius, size.height),
                    end = Offset(center.x + radius, size.height),
                    strokeWidth = 1.2.dp.toPx(),
                )
            }
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawDiamond(
    center: Offset,
    radius: Float,
    color: androidx.compose.ui.graphics.Color,
    style: DrawStyle,
) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        lineTo(center.x + radius, center.y)
        lineTo(center.x, center.y + radius)
        lineTo(center.x - radius, center.y)
        close()
    }
    drawPath(path = path, color = color, style = style)
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawTriangle(
    center: Offset,
    radius: Float,
    color: androidx.compose.ui.graphics.Color,
    style: DrawStyle,
) {
    val bounds = Rect(
        left = center.x - radius,
        top = center.y - radius,
        right = center.x + radius,
        bottom = center.y + radius,
    )
    val path = Path().apply {
        moveTo(bounds.center.x, bounds.top)
        lineTo(bounds.right, bounds.bottom)
        lineTo(bounds.left, bounds.bottom)
        close()
    }
    drawPath(path = path, color = color, style = style)
}
