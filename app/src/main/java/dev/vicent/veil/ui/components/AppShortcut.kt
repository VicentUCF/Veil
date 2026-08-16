package dev.vicent.veil.ui.components

import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.ui.theme.LocalVeilPalette

@Composable
fun LauncherAppIcon(
    app: LauncherApp,
    size: Dp,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    val icon = app.icon

    if (icon != null) {
        Image(
            bitmap = icon.bitmap.asImageBitmap(),
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
