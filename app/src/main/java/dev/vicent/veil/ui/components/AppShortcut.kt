package dev.vicent.veil.ui.components

import android.graphics.Canvas
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import androidx.compose.foundation.Canvas as ComposeCanvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.createBitmap
import dev.vicent.veil.launcher.ResolvedLauncherContext
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.ui.theme.LocalVeilPalette
import kotlin.math.min

@Composable
fun AppCluster(
    context: ResolvedLauncherContext,
    onAppSelected: (LauncherApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current

    Row(
        modifier = modifier.height(IntrinsicSize.Min),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .drawBehind { drawRect(palette.accentActive) },
        )

        Column(modifier = Modifier.padding(start = 18.dp)) {
            if (context.apps.isEmpty()) {
                BasicText(
                    text = "NO APPS",
                    style = appLabelStyle(palette.contentMuted),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp),
                )
            } else {
                context.apps.forEach { app ->
                    AppShortcut(app = app, onClick = { onAppSelected(app) })
                }
            }
        }
    }
}

@Composable
private fun AppShortcut(
    app: LauncherApp,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    val locale = LocalLocale.current.platformLocale
    val icon = remember(app.icon) { app.icon?.toRenderedIcon() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .widthIn(min = 220.dp, max = 292.dp)
            .height(54.dp)
            .clickable(
                role = Role.Button,
                onClickLabel = "Open ${app.label}",
                onClick = onClick,
            )
            .semantics { stateDescription = app.label }
            .padding(horizontal = 12.dp),
    ) {
        if (icon != null) {
            Image(
                bitmap = icon.bitmap,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                colorFilter = if (icon.shouldTint) {
                    ColorFilter.tint(
                        color = palette.contentPrimary,
                        blendMode = BlendMode.SrcIn,
                    )
                } else {
                    null
                },
                modifier = Modifier.size(21.dp),
            )
        } else {
            ComposeCanvas(modifier = Modifier.size(21.dp)) {
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

        BasicText(
            text = app.label.uppercase(locale),
            style = appLabelStyle(palette.contentPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(start = 28.dp),
        )
    }
}

private fun appLabelStyle(color: Color) = TextStyle(
    color = color,
    fontFamily = FontFamily.SansSerif,
    fontSize = 14.sp,
    fontWeight = FontWeight.Normal,
    letterSpacing = 2.8.sp,
    shadow = Shadow(
        color = Color.Black.copy(alpha = 0.82f),
        offset = Offset(0f, 1f),
        blurRadius = 5f,
    ),
)

private data class RenderedAppIcon(
    val bitmap: ImageBitmap,
    val shouldTint: Boolean,
)

private fun Drawable.toRenderedIcon(): RenderedAppIcon {
    val source = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this is AdaptiveIconDrawable) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            monochrome ?: foreground
        } else {
            foreground
        }
    } else {
        this
    }
    val targetSize = 72
    val bitmap = createBitmap(targetSize, targetSize)
    val canvas = Canvas(bitmap)
    val drawable = source.mutate()
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
        shouldTint = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && this is AdaptiveIconDrawable,
    )
}
