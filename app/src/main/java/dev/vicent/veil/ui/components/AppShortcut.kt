package dev.vicent.veil.ui.components

import android.graphics.Canvas
import android.graphics.drawable.Drawable
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.core.graphics.createBitmap
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.ui.theme.LocalVeilPalette
import kotlin.math.min

@Composable
fun LauncherAppIcon(
    app: LauncherApp,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    val density = LocalDensity.current
    val targetSize = with(density) { size.roundToPx() }
    val icon = remember(app.componentName, app.icon, targetSize) {
        app.icon?.toRenderedIcon(targetSize)
    }

    if (icon != null) {
        Image(
            bitmap = icon.bitmap,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = modifier.size(size),
        )
    } else {
        ComposeCanvas(modifier = modifier.size(size)) {
            drawCircle(
                color = palette.contentSecondary,
                style = Stroke(width = 1.dp.toPx()),
            )
            drawCircle(
                color = palette.contentSecondary,
                radius = 1.5.dp.toPx(),
            )
        }
    }
}

private data class RenderedAppIcon(
    val bitmap: ImageBitmap,
)

private fun Drawable.toRenderedIcon(targetSize: Int): RenderedAppIcon {
    val bitmap = createBitmap(targetSize, targetSize)
    val canvas = Canvas(bitmap)
    val drawable = constantState?.newDrawable()?.mutate() ?: mutate()
    val intrinsicWidth = drawable.intrinsicWidth.coerceAtLeast(1)
    val intrinsicHeight = drawable.intrinsicHeight.coerceAtLeast(1)
    val scale = min(targetSize.toFloat() / intrinsicWidth, targetSize.toFloat() / intrinsicHeight)
    val width = (intrinsicWidth * scale).toInt().coerceAtLeast(1)
    val height = (intrinsicHeight * scale).toInt().coerceAtLeast(1)
    val left = (targetSize - width) / 2
    val top = (targetSize - height) / 2

    drawable.setBounds(left, top, left + width, top + height)
    drawable.draw(canvas)
    return RenderedAppIcon(
        bitmap = bitmap.asImageBitmap(),
    )
}
