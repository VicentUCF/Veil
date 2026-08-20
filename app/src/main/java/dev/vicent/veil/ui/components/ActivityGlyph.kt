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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
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
    FOCUS,
    MEDIA,
    GAME,
    TOOLS,
    PHONE,
    MESSAGE,
    BROWSER,
    BRAVE,
    CAMERA,
    WHATSAPP,
    NAVIGATION,
    PROGRESS,
    APP,
}

fun LauncherApp.activityGlyph(): ActivityGlyphKind {
    return activityGlyphFor(packageName, label, category)
}

internal fun activityGlyphFor(
    packageName: String,
    label: String,
    category: AppCategory,
): ActivityGlyphKind {
    val searchable = "$packageName $label".lowercase()
    return when {
        "brave" in searchable -> ActivityGlyphKind.BRAVE
        "whatsapp" in searchable -> ActivityGlyphKind.WHATSAPP
        listOf("dialer", "phone", "telefono", "contacts").any(searchable::contains) ->
            ActivityGlyphKind.PHONE
        listOf("message", "messaging", "sms", "signal", "telegram").any(searchable::contains) ->
            ActivityGlyphKind.MESSAGE
        listOf("browser", "chrome", "firefox", "edge").any(searchable::contains) ->
            ActivityGlyphKind.BROWSER
        listOf("camera", "camara").any(searchable::contains) -> ActivityGlyphKind.CAMERA
        category == AppCategory.WORK -> ActivityGlyphKind.WORK
        category == AppCategory.MEDIA -> ActivityGlyphKind.MEDIA
        category == AppCategory.GAME -> ActivityGlyphKind.GAME
        else -> ActivityGlyphKind.APP
    }
}

fun LauncherContextKind.activityGlyph(): ActivityGlyphKind = when (this) {
    LauncherContextKind.CURRENT -> ActivityGlyphKind.CURRENT
    LauncherContextKind.WORK -> ActivityGlyphKind.WORK
    LauncherContextKind.FOCUS -> ActivityGlyphKind.FOCUS
    LauncherContextKind.MEDIA -> ActivityGlyphKind.MEDIA
    LauncherContextKind.GAME -> ActivityGlyphKind.GAME
    LauncherContextKind.TOOLS -> ActivityGlyphKind.TOOLS
    LauncherContextKind.ON_THE_GO -> ActivityGlyphKind.NAVIGATION
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
        val stroke = Stroke(
            width = 1.15.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
        )
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
            ActivityGlyphKind.FOCUS -> {
                drawCircle(
                    color = glyphColor,
                    radius = w * .28f,
                    center = center.copy(y = h * .54f),
                    style = stroke,
                )
                drawLine(
                    glyphColor,
                    Offset(center.x, h * .18f),
                    Offset(center.x, h * .26f),
                    stroke.width,
                    StrokeCap.Round,
                )
                drawLine(
                    glyphColor,
                    center.copy(y = h * .54f),
                    Offset(w * .62f, h * .39f),
                    stroke.width,
                    StrokeCap.Round,
                )
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
            ActivityGlyphKind.GAME -> {
                drawRoundRect(
                    glyphColor,
                    Offset(w * .16f, h * .31f),
                    Size(w * .68f, h * .42f),
                    CornerRadius(w * .16f),
                    style = if (isActive) androidx.compose.ui.graphics.drawscope.Fill else stroke,
                )
                val detailColor = if (isActive) palette.tileBackground else glyphColor
                drawLine(detailColor, Offset(w * .29f, h * .52f), Offset(w * .45f, h * .52f), stroke.width)
                drawLine(detailColor, Offset(w * .37f, h * .44f), Offset(w * .37f, h * .60f), stroke.width)
                drawCircle(detailColor, w * .035f, Offset(w * .65f, h * .47f))
                drawCircle(detailColor, w * .035f, Offset(w * .72f, h * .57f))
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
                    moveTo(w * .28f, h * .19f)
                    cubicTo(w * .20f, h * .27f, w * .23f, h * .43f, w * .34f, h * .57f)
                    cubicTo(w * .45f, h * .71f, w * .61f, h * .80f, w * .72f, h * .75f)
                    lineTo(w * .80f, h * .61f)
                    lineTo(w * .62f, h * .51f)
                    lineTo(w * .53f, h * .62f)
                    cubicTo(w * .45f, h * .57f, w * .39f, h * .50f, w * .35f, h * .41f)
                    lineTo(w * .45f, h * .31f)
                    close()
                }
                drawPath(path, glyphColor, style = stroke)
            }
            ActivityGlyphKind.MESSAGE -> {
                drawRoundRect(
                    color = glyphColor,
                    topLeft = Offset(w * .16f, h * .20f),
                    size = Size(w * .68f, h * .52f),
                    cornerRadius = CornerRadius(w * .13f),
                    style = stroke,
                )
                drawLine(glyphColor, Offset(w * .32f, h * .72f), Offset(w * .24f, h * .83f), stroke.width, StrokeCap.Round)
                drawLine(glyphColor, Offset(w * .32f, h * .72f), Offset(w * .45f, h * .72f), stroke.width, StrokeCap.Round)
                drawLine(glyphColor, Offset(w * .30f, h * .39f), Offset(w * .70f, h * .39f), stroke.width, StrokeCap.Round)
                drawLine(glyphColor, Offset(w * .30f, h * .53f), Offset(w * .57f, h * .53f), stroke.width, StrokeCap.Round)
            }
            ActivityGlyphKind.BROWSER -> {
                drawCircle(glyphColor, w * .31f, center, style = stroke)
                drawLine(glyphColor, Offset(w * .28f, h * .72f), Offset(w * .72f, h * .28f), stroke.width)
            }
            ActivityGlyphKind.BRAVE -> {
                val shield = Path().apply {
                    moveTo(w * .50f, h * .13f)
                    lineTo(w * .78f, h * .25f)
                    lineTo(w * .74f, h * .59f)
                    cubicTo(w * .71f, h * .72f, w * .60f, h * .81f, w * .50f, h * .87f)
                    cubicTo(w * .40f, h * .81f, w * .29f, h * .72f, w * .26f, h * .59f)
                    lineTo(w * .22f, h * .25f)
                    close()
                }
                drawPath(shield, glyphColor, style = stroke)
                drawLine(glyphColor, Offset(w * .32f, h * .36f), Offset(w * .42f, h * .32f), stroke.width, StrokeCap.Round)
                drawLine(glyphColor, Offset(w * .58f, h * .32f), Offset(w * .68f, h * .36f), stroke.width, StrokeCap.Round)
                val face = Path().apply {
                    moveTo(w * .35f, h * .45f)
                    lineTo(w * .41f, h * .65f)
                    lineTo(w * .50f, h * .72f)
                    lineTo(w * .59f, h * .65f)
                    lineTo(w * .65f, h * .45f)
                    lineTo(w * .50f, h * .55f)
                    close()
                }
                drawPath(face, glyphColor, style = stroke)
            }
            ActivityGlyphKind.CAMERA -> {
                val body = Path().apply {
                    moveTo(w * .17f, h * .33f)
                    lineTo(w * .34f, h * .33f)
                    lineTo(w * .40f, h * .23f)
                    lineTo(w * .60f, h * .23f)
                    lineTo(w * .66f, h * .33f)
                    lineTo(w * .83f, h * .33f)
                    lineTo(w * .83f, h * .76f)
                    lineTo(w * .17f, h * .76f)
                    close()
                }
                drawPath(body, glyphColor, style = stroke)
                drawCircle(glyphColor, w * .15f, Offset(w * .50f, h * .54f), style = stroke)
                drawCircle(glyphColor, w * .035f, Offset(w * .50f, h * .54f))
            }
            ActivityGlyphKind.WHATSAPP -> {
                drawArc(
                    color = glyphColor,
                    startAngle = 132f,
                    sweepAngle = 330f,
                    useCenter = false,
                    topLeft = Offset(w * .20f, h * .17f),
                    size = Size(w * .60f, h * .60f),
                    style = stroke,
                )
                val bubbleTail = Path().apply {
                    moveTo(w * .30f, h * .69f)
                    lineTo(w * .23f, h * .82f)
                    lineTo(w * .44f, h * .76f)
                }
                drawPath(bubbleTail, glyphColor, style = stroke)
                val handset = Path().apply {
                    moveTo(w * .39f, h * .32f)
                    cubicTo(w * .34f, h * .39f, w * .47f, h * .62f, w * .61f, h * .64f)
                    lineTo(w * .67f, h * .55f)
                    lineTo(w * .56f, h * .49f)
                    lineTo(w * .50f, h * .55f)
                    cubicTo(w * .45f, h * .51f, w * .42f, h * .46f, w * .41f, h * .41f)
                    lineTo(w * .47f, h * .36f)
                    close()
                }
                drawPath(handset, glyphColor, style = stroke)
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
