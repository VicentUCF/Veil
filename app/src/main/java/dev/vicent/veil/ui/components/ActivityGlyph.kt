package dev.vicent.veil.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import dev.vicent.veil.launcher.model.AppCategory
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.ui.theme.LocalVeilPalette

enum class ActivityGlyphKind {
    CURRENT,
    WORK,
    MEDIA,
    SOCIAL,
    TOOLS,
    PHONE,
    MESSAGE,
    BROWSER,
    CAMERA,
    NAVIGATION,
    PROGRESS,
    APP,
}

fun LauncherApp.activityGlyph(): ActivityGlyphKind {
    val searchable = "$packageName $label".lowercase()
    return when {
        listOf("dialer", "phone", "telefono", "contacts").any(searchable::contains) ->
            ActivityGlyphKind.PHONE
        listOf("message", "messaging", "sms", "signal", "whatsapp", "telegram").any(searchable::contains) ->
            ActivityGlyphKind.MESSAGE
        listOf("browser", "chrome", "firefox", "brave", "edge").any(searchable::contains) ->
            ActivityGlyphKind.BROWSER
        listOf("camera", "camara").any(searchable::contains) -> ActivityGlyphKind.CAMERA
        category == AppCategory.WORK -> ActivityGlyphKind.WORK
        category == AppCategory.MEDIA -> ActivityGlyphKind.MEDIA
        category == AppCategory.SOCIAL -> ActivityGlyphKind.SOCIAL
        else -> ActivityGlyphKind.APP
    }
}

fun LauncherContextKind.activityGlyph(): ActivityGlyphKind = when (this) {
    LauncherContextKind.CURRENT -> ActivityGlyphKind.CURRENT
    LauncherContextKind.WORK -> ActivityGlyphKind.WORK
    LauncherContextKind.MEDIA -> ActivityGlyphKind.MEDIA
    LauncherContextKind.SOCIAL -> ActivityGlyphKind.SOCIAL
    LauncherContextKind.TOOLS -> ActivityGlyphKind.TOOLS
}

@Composable
fun ActivityGlyph(
    kind: ActivityGlyphKind,
    size: Dp,
    modifier: Modifier = Modifier,
    isActive: Boolean = false,
    color: Color? = null,
) {
    val palette = LocalVeilPalette.current
    val glyphColor = color ?: if (isActive) palette.accentActive else palette.contentSecondary
    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(width = 1.15.dp.toPx())
        val w = this.size.width
        val h = this.size.height
        val center = Offset(w / 2f, h / 2f)
        val inset = w * 0.2f
        when (kind) {
            ActivityGlyphKind.CURRENT -> {
                drawCircle(
                    color = glyphColor,
                    radius = w * 0.27f,
                    center = center,
                    style = if (isActive) androidx.compose.ui.graphics.drawscope.Fill else stroke,
                )
                if (!isActive) drawCircle(glyphColor, w * 0.07f, center)
            }
            ActivityGlyphKind.WORK -> {
                drawRect(
                    glyphColor,
                    Offset(inset, h * .34f),
                    Size(w - inset * 2, h * .42f),
                    style = if (isActive) androidx.compose.ui.graphics.drawscope.Fill else stroke,
                )
                drawLine(glyphColor, Offset(w * .39f, h * .34f), Offset(w * .39f, h * .24f), stroke.width)
                drawLine(glyphColor, Offset(w * .39f, h * .24f), Offset(w * .61f, h * .24f), stroke.width)
                drawLine(glyphColor, Offset(w * .61f, h * .24f), Offset(w * .61f, h * .34f), stroke.width)
            }
            ActivityGlyphKind.MEDIA -> {
                val triangle = Path().apply {
                    moveTo(w * .38f, h * .27f)
                    lineTo(w * .72f, h * .5f)
                    lineTo(w * .38f, h * .73f)
                    close()
                }
                drawPath(
                    path = triangle,
                    color = glyphColor,
                    style = if (isActive) androidx.compose.ui.graphics.drawscope.Fill else stroke,
                )
            }
            ActivityGlyphKind.SOCIAL, ActivityGlyphKind.MESSAGE -> {
                drawRoundRect(
                    glyphColor,
                    Offset(w * .2f, h * .24f),
                    Size(w * .6f, h * .46f),
                    CornerRadius(w * .08f),
                    style = if (isActive) androidx.compose.ui.graphics.drawscope.Fill else stroke,
                )
                drawLine(glyphColor, Offset(w * .32f, h * .7f), Offset(w * .25f, h * .82f), stroke.width)
                drawLine(glyphColor, Offset(w * .32f, h * .7f), Offset(w * .45f, h * .7f), stroke.width)
            }
            ActivityGlyphKind.TOOLS -> {
                drawCircle(
                    glyphColor,
                    w * .29f,
                    center,
                    style = if (isActive) androidx.compose.ui.graphics.drawscope.Fill else stroke,
                )
                drawCircle(glyphColor, w * .08f, center)
                repeat(4) { index ->
                    val horizontal = index % 2 == 0
                    val sign = if (index < 2) -1 else 1
                    val start = if (horizontal) Offset(center.x + sign * w * .29f, center.y) else Offset(center.x, center.y + sign * h * .29f)
                    val end = if (horizontal) Offset(center.x + sign * w * .41f, center.y) else Offset(center.x, center.y + sign * h * .41f)
                    drawLine(glyphColor, start, end, stroke.width)
                }
            }
            ActivityGlyphKind.PHONE -> {
                val path = Path().apply {
                    moveTo(w * .3f, h * .22f)
                    cubicTo(w * .18f, h * .34f, w * .52f, h * .82f, w * .7f, h * .74f)
                    lineTo(w * .78f, h * .61f)
                    lineTo(w * .61f, h * .52f)
                    lineTo(w * .52f, h * .61f)
                    cubicTo(w * .43f, h * .55f, w * .38f, h * .48f, w * .34f, h * .4f)
                    lineTo(w * .43f, h * .31f)
                    close()
                }
                drawPath(path, glyphColor, style = stroke)
            }
            ActivityGlyphKind.BROWSER -> {
                drawCircle(glyphColor, w * .31f, center, style = stroke)
                drawLine(glyphColor, Offset(w * .28f, h * .72f), Offset(w * .72f, h * .28f), stroke.width)
            }
            ActivityGlyphKind.CAMERA -> {
                drawRoundRect(glyphColor, Offset(w * .18f, h * .3f), Size(w * .64f, h * .46f), CornerRadius(w * .06f), style = stroke)
                drawCircle(glyphColor, w * .13f, center, style = stroke)
                drawLine(glyphColor, Offset(w * .34f, h * .3f), Offset(w * .4f, h * .22f), stroke.width)
                drawLine(glyphColor, Offset(w * .4f, h * .22f), Offset(w * .58f, h * .22f), stroke.width)
            }
            ActivityGlyphKind.NAVIGATION -> {
                val path = Path().apply {
                    moveTo(w * .5f, h * .16f)
                    lineTo(w * .76f, h * .78f)
                    lineTo(w * .5f, h * .65f)
                    lineTo(w * .24f, h * .78f)
                    close()
                }
                drawPath(path, glyphColor, style = stroke)
            }
            ActivityGlyphKind.PROGRESS -> {
                drawArc(
                    color = glyphColor,
                    startAngle = -90f,
                    sweepAngle = 275f,
                    useCenter = false,
                    topLeft = Offset(inset, inset),
                    size = Size(w - inset * 2, h - inset * 2),
                    style = stroke,
                )
            }
            ActivityGlyphKind.APP -> {
                drawRect(glyphColor, Offset(w * .25f, h * .25f), Size(w * .5f, h * .5f), style = stroke)
                drawCircle(glyphColor, w * .055f, center)
            }
        }
    }
}
