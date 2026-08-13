package dev.vicent.veil.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vicent.veil.launcher.LauncherUiState
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.ContinuityItem
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.WeatherAvailability
import dev.vicent.veil.ui.theme.LocalVeilPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun CurrentHome(
    state: LauncherUiState,
    apps: List<LauncherApp>,
    onAppSelected: (LauncherApp) -> Unit,
    onAppLongPressed: (LauncherApp) -> Unit,
    onLocationPermissionRequested: () -> Unit,
    onClockOpenRequested: () -> Unit,
    onCalendarOpenRequested: () -> Unit,
    onContinuityAction: (String, ContinuityAction, Long?) -> Unit,
    onMediaDismissed: (String) -> Unit,
    onQuickButtonTap: () -> Unit,
    onQuickButtonLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val playingMedia = state.mediaContinuity?.takeIf { it.isPlaying }
    var dismissedMediaId by rememberSaveable { mutableStateOf<String?>(null) }

    LaunchedEffect(playingMedia?.id) {
        when {
            playingMedia == null -> dismissedMediaId = null
            dismissedMediaId != null && dismissedMediaId != playingMedia.id -> dismissedMediaId = null
        }
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val upperBreathingRoom = (maxHeight * 0.32f).coerceIn(130.dp, 280.dp)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
        ) {
            Spacer(Modifier.height(upperBreathingRoom))
            HomeClockAndWeather(
                state = state,
                onLocationPermissionRequested = onLocationPermissionRequested,
                onClockOpenRequested = onClockOpenRequested,
                onCalendarOpenRequested = onCalendarOpenRequested,
            )

            if (playingMedia != null && dismissedMediaId != playingMedia.id) {
                CompactMediaPlayer(
                    media = playingMedia,
                    onAction = onContinuityAction,
                    onDismiss = {
                        dismissedMediaId = playingMedia.id
                        onMediaDismissed(playingMedia.id)
                    },
                    modifier = Modifier.padding(top = 12.dp),
                )
            } else {
                Spacer(Modifier.height(8.dp))
            }

            Column(
                verticalArrangement = Arrangement.spacedBy(1.dp),
                modifier = Modifier.padding(top = 10.dp).widthIn(max = 278.dp),
            ) {
                apps.take(5).forEach { app ->
                    HomeAppRow(
                        app = app,
                        hasNotification = app.packageName in state.notificationIndicatorPackages,
                        onClick = { onAppSelected(app) },
                        onLongClick = { onAppLongPressed(app) },
                    )
                }
            }
        }

        HomeQuickButton(
            onClick = onQuickButtonTap,
            onLongClick = onQuickButtonLongPress,
            modifier = Modifier.align(Alignment.BottomEnd).padding(end = 2.dp, bottom = 12.dp),
        )
    }
}

@Composable
private fun HomeClockAndWeather(
    state: LauncherUiState,
    onLocationPermissionRequested: () -> Unit,
    onClockOpenRequested: () -> Unit,
    onCalendarOpenRequested: () -> Unit,
) {
    val palette = LocalVeilPalette.current
    val context = LocalContext.current
    val locale = Locale.forLanguageTag(LocalLocale.current.toLanguageTag())
    val now by rememberCurrentTime()
    val weather = state.weather

    BasicText(
        text = DateFormat.getTimeFormat(context).format(now),
        style = TextStyle(
            color = palette.contentPrimary,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.ExtraLight,
            fontSize = 59.sp,
            letterSpacing = 1.5.sp,
        ),
        modifier = Modifier.clickable(
            role = Role.Button,
            onClickLabel = "Abrir Reloj",
            onClick = onClockOpenRequested,
        ),
    )
    BasicText(
        text = SimpleDateFormat("EEEE, d MMMM yyyy", locale).format(now),
        style = TextStyle(
            color = palette.contentSecondary,
            fontFamily = FontFamily.SansSerif,
            fontSize = 12.sp,
            letterSpacing = 0.7.sp,
        ),
        modifier = Modifier
            .clickable(
                role = Role.Button,
                onClickLabel = "Abrir Calendario",
                onClick = onCalendarOpenRequested,
            )
            .padding(top = 1.dp, bottom = 4.dp),
    )
    Canvas(Modifier.padding(top = 4.dp).width(210.dp).height(1.dp)) {
        drawLine(
            palette.contentSecondary,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 1.dp.toPx(),
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.height(44.dp),
    ) {
        WeatherGlyph(
            weatherCode = weather.weatherCode,
            modifier = Modifier.offset(x = (-8).dp).size(40.dp),
        )
        Column(modifier = Modifier.padding(start = 11.dp)) {
            when (weather.availability) {
                WeatherAvailability.AVAILABLE -> {
                    BasicText(
                        text = "${weather.temperatureCelsius?.roundToInt() ?: "—"}°  ${homeWeatherDescription(weather.weatherCode)}",
                        style = TextStyle(
                            color = palette.contentPrimary,
                            fontFamily = FontFamily.SansSerif,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Light,
                        ),
                    )
                    if (weather.isStale) HomeWeatherLabel("DESACTUALIZADO")
                }
                WeatherAvailability.NEEDS_PERMISSION -> HomeWeatherAction(
                    label = "ACTIVAR TIEMPO",
                    onClick = onLocationPermissionRequested,
                )
                WeatherAvailability.LOADING -> HomeWeatherLabel("ACTUALIZANDO…")
                WeatherAvailability.UNAVAILABLE -> HomeWeatherLabel("TIEMPO NO DISPONIBLE")
            }
        }
    }
}

@Composable
private fun HomeWeatherAction(label: String, onClick: () -> Unit) {
    BasicText(
        text = label,
        style = homeSmallMonoStyle(LocalVeilPalette.current.contentPrimary),
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
    )
}

@Composable
private fun HomeWeatherLabel(label: String) {
    BasicText(text = label, style = homeSmallMonoStyle(LocalVeilPalette.current.contentSecondary))
}

@Composable
internal fun WeatherGlyph(weatherCode: Int?, modifier: Modifier = Modifier) {
    val palette = LocalVeilPalette.current
    Canvas(modifier) {
        val unit = size.minDimension
        val strokeWidth = (unit * .027f).coerceAtLeast(1.dp.toPx())
        val stroke = Stroke(strokeWidth, cap = StrokeCap.Round)
        val glyphColor = palette.contentPrimary

        fun sun(at: Offset, radius: Float) {
            drawCircle(palette.accentActive, radius, at, style = stroke)
            repeat(8) { index ->
                val angle = Math.toRadians((index * 45 - 90).toDouble())
                drawLine(
                    color = palette.accentActive,
                    start = Offset(
                        at.x + cos(angle).toFloat() * radius * 1.38f,
                        at.y + sin(angle).toFloat() * radius * 1.38f,
                    ),
                    end = Offset(
                        at.x + cos(angle).toFloat() * radius * 1.82f,
                        at.y + sin(angle).toFloat() * radius * 1.82f,
                    ),
                    strokeWidth = strokeWidth,
                    cap = StrokeCap.Round,
                )
            }
        }

        fun cloud(yOffset: Float = 0f) {
            val path = Path().apply {
                moveTo(unit * .25f, unit * (.60f + yOffset))
                cubicTo(
                    unit * .20f, unit * (.48f + yOffset),
                    unit * .29f, unit * (.40f + yOffset),
                    unit * .39f, unit * (.43f + yOffset),
                )
                cubicTo(
                    unit * .45f, unit * (.28f + yOffset),
                    unit * .68f, unit * (.32f + yOffset),
                    unit * .69f, unit * (.48f + yOffset),
                )
                cubicTo(
                    unit * .82f, unit * (.48f + yOffset),
                    unit * .84f, unit * (.66f + yOffset),
                    unit * .70f, unit * (.67f + yOffset),
                )
                lineTo(unit * .34f, unit * (.67f + yOffset))
                cubicTo(
                    unit * .27f, unit * (.67f + yOffset),
                    unit * .23f, unit * (.64f + yOffset),
                    unit * .25f, unit * (.60f + yOffset),
                )
            }
            drawPath(path, glyphColor, style = stroke)
        }

        fun rain(snow: Boolean = false, drizzle: Boolean = false) {
            val drops = listOf(.36f, .52f, .68f)
            drops.forEachIndexed { index, x ->
                if (snow) {
                    val at = Offset(unit * x, unit * (.77f + if (index == 1) .04f else 0f))
                    drawLine(
                        glyphColor,
                        at.copy(x = at.x - unit * .035f),
                        at.copy(x = at.x + unit * .035f),
                        strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    drawLine(
                        glyphColor,
                        at.copy(y = at.y - unit * .035f),
                        at.copy(y = at.y + unit * .035f),
                        strokeWidth,
                        cap = StrokeCap.Round,
                    )
                } else {
                    val length = if (drizzle) .055f else .09f
                    drawLine(
                        color = palette.accentActive,
                        start = Offset(unit * (x + .025f), unit * .73f),
                        end = Offset(unit * (x - .025f), unit * (.73f + length)),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }
        }

        when (weatherCode) {
            0 -> sun(center, unit * .16f)
            1, 2 -> {
                sun(Offset(unit * .38f, unit * .39f), unit * .09f)
                cloud(.02f)
            }
            3 -> cloud()
            45, 48 -> {
                cloud(-.05f)
                drawLine(glyphColor, Offset(unit * .30f, unit * .72f), Offset(unit * .70f, unit * .72f), strokeWidth, cap = StrokeCap.Round)
                drawLine(glyphColor, Offset(unit * .36f, unit * .79f), Offset(unit * .64f, unit * .79f), strokeWidth, cap = StrokeCap.Round)
            }
            in 51..57 -> {
                cloud(-.04f)
                rain(drizzle = true)
            }
            in 61..67, in 80..82 -> {
                cloud(-.04f)
                rain()
            }
            in 71..77, 85, 86 -> {
                cloud(-.04f)
                rain(snow = true)
            }
            in 95..99 -> {
                cloud(-.06f)
                val bolt = Path().apply {
                    moveTo(unit * .55f, unit * .69f)
                    lineTo(unit * .45f, unit * .82f)
                    lineTo(unit * .54f, unit * .81f)
                    lineTo(unit * .47f, unit * .91f)
                }
                drawPath(bolt, palette.accentActive, style = Stroke(strokeWidth * 1.25f, cap = StrokeCap.Round))
            }
            else -> {
                drawLine(glyphColor, Offset(unit * .34f, center.y), Offset(unit * .66f, center.y), strokeWidth, cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun CompactMediaPlayer(
    media: ContinuityItem.Media,
    onAction: (String, ContinuityAction, Long?) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    var horizontalOffset by remember(media.id) { mutableFloatStateOf(0f) }
    val shape = RoundedCornerShape(10.dp)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .offset { IntOffset(horizontalOffset.roundToInt(), 0) }
            .widthIn(max = 302.dp)
            .fillMaxWidth()
            .height(62.dp)
            .clip(shape)
            .background(Color(0xFF0D1114).copy(alpha = 0.82f))
            .border(1.dp, palette.divider, shape)
            .pointerInput(media.id) {
                detectHorizontalDragGestures(
                    onHorizontalDrag = { change, amount ->
                        change.consume()
                        horizontalOffset += amount
                    },
                    onDragEnd = {
                        if (abs(horizontalOffset) > size.width * .24f) onDismiss()
                        else horizontalOffset = 0f
                    },
                    onDragCancel = { horizontalOffset = 0f },
                )
            }
            .padding(start = 8.dp, end = 4.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .clickable(
                    enabled = ContinuityAction.OPEN in media.supportedActions,
                    role = Role.Button,
                    onClickLabel = "Abrir ${media.appLabel}",
                ) {
                    onAction(media.id, ContinuityAction.OPEN, null)
                },
        ) {
            if (media.artwork != null) {
                Image(
                    bitmap = remember(media.artwork) { media.artwork.asImageBitmap() },
                    contentDescription = null,
                    modifier = Modifier.size(42.dp).clip(RoundedCornerShape(7.dp)),
                )
            } else {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(Color.White.copy(alpha = .055f)),
                ) {
                    ActivityGlyph(ActivityGlyphKind.MEDIA, size = 22.dp, isActive = true)
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 9.dp, end = 4.dp)) {
                BasicText(
                    text = media.title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = palette.contentPrimary,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                BasicText(
                    text = media.subtitle ?: media.appLabel,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = palette.contentMuted,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 9.sp,
                    ),
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (ContinuityAction.SKIP_PREVIOUS in media.supportedActions) {
                CompactMediaControl(
                    kind = CompactMediaControlKind.PREVIOUS,
                    label = "Canción anterior",
                    onClick = { onAction(media.id, ContinuityAction.SKIP_PREVIOUS, null) },
                )
            }
            if (ContinuityAction.TOGGLE_PLAYBACK in media.supportedActions) {
                CompactMediaControl(
                    kind = if (media.isPlaying) {
                        CompactMediaControlKind.PAUSE
                    } else {
                        CompactMediaControlKind.PLAY
                    },
                    label = if (media.isPlaying) "Pausar" else "Reproducir",
                    onClick = { onAction(media.id, ContinuityAction.TOGGLE_PLAYBACK, null) },
                )
            }
            if (ContinuityAction.SKIP_NEXT in media.supportedActions) {
                CompactMediaControl(
                    kind = CompactMediaControlKind.NEXT,
                    label = "Canción siguiente",
                    onClick = { onAction(media.id, ContinuityAction.SKIP_NEXT, null) },
                )
            }
        }
    }
}

private enum class CompactMediaControlKind { PREVIOUS, PLAY, PAUSE, NEXT }

@Composable
private fun CompactMediaControl(
    kind: CompactMediaControlKind,
    label: String,
    onClick: () -> Unit,
) {
    val color = LocalVeilPalette.current.contentPrimary
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(38.dp)
            .clickable(role = Role.Button, onClickLabel = label, onClick = onClick),
    ) {
        Canvas(Modifier.size(16.dp)) {
            val strokeWidth = 1.6.dp.toPx()
            when (kind) {
                CompactMediaControlKind.PREVIOUS,
                CompactMediaControlKind.NEXT,
                -> {
                    val pointsLeft = kind == CompactMediaControlKind.PREVIOUS
                    val barX = if (pointsLeft) size.width * .18f else size.width * .82f
                    drawLine(
                        color = color,
                        start = Offset(barX, size.height * .18f),
                        end = Offset(barX, size.height * .82f),
                        strokeWidth = strokeWidth,
                        cap = StrokeCap.Round,
                    )
                    val path = Path().apply {
                        if (pointsLeft) {
                            moveTo(size.width * .72f, size.height * .17f)
                            lineTo(size.width * .28f, size.height * .5f)
                            lineTo(size.width * .72f, size.height * .83f)
                        } else {
                            moveTo(size.width * .28f, size.height * .17f)
                            lineTo(size.width * .72f, size.height * .5f)
                            lineTo(size.width * .28f, size.height * .83f)
                        }
                        close()
                    }
                    drawPath(path, color)
                }

                CompactMediaControlKind.PLAY -> {
                    val path = Path().apply {
                        moveTo(size.width * .28f, size.height * .14f)
                        lineTo(size.width * .78f, size.height * .5f)
                        lineTo(size.width * .28f, size.height * .86f)
                        close()
                    }
                    drawPath(path, color)
                }

                CompactMediaControlKind.PAUSE -> {
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(size.width * .23f, size.height * .15f),
                        size = androidx.compose.ui.geometry.Size(size.width * .18f, size.height * .7f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(strokeWidth / 2f),
                    )
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(size.width * .59f, size.height * .15f),
                        size = androidx.compose.ui.geometry.Size(size.width * .18f, size.height * .7f),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(strokeWidth / 2f),
                    )
                }
            }
        }
    }
}

@Composable
private fun HomeAppRow(
    app: LauncherApp,
    hasNotification: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val palette = LocalVeilPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(49.dp)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = "Abrir ${app.label}",
                onLongClickLabel = "Opciones de ${app.label}",
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .then(
                if (hasNotification) {
                    Modifier.semantics {
                        stateDescription = "Con notificaciones"
                    }
                } else {
                    Modifier
                },
            ),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.width(47.dp)) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(31.dp)) {
                ActivityGlyph(kind = app.activityGlyph(), size = 27.dp)
                AppNotificationIndicator(
                    visible = hasNotification,
                    modifier = Modifier.align(Alignment.TopEnd),
                )
            }
        }
        BasicText(
            text = app.label.uppercase(),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = TextStyle(
                color = palette.contentPrimary,
                fontFamily = FontFamily.SansSerif,
                fontWeight = FontWeight.Light,
                fontSize = 13.sp,
                letterSpacing = 4.2.sp,
            ),
            modifier = Modifier.padding(start = 9.dp),
        )
    }
}

@Composable
private fun HomeQuickButton(
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(Color(0xFF0C1013).copy(alpha = .78f))
            .border(1.dp, palette.contentSecondary, CircleShape)
            .combinedClickable(
                role = Role.Button,
                onClickLabel = "Acción rápida",
                onLongClickLabel = "Acción rápida secundaria",
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        Canvas(Modifier.size(18.dp)) {
            val stroke = 1.1.dp.toPx()
            drawLine(palette.contentPrimary, start = center.copy(y = 0f), end = center.copy(y = size.height), strokeWidth = stroke)
            drawLine(palette.contentPrimary, start = center.copy(x = 0f), end = center.copy(x = size.width), strokeWidth = stroke)
        }
    }
}

@Composable
private fun rememberCurrentTime(): androidx.compose.runtime.State<Date> =
    produceState(initialValue = Date()) {
        while (isActive) {
            delay(15_000)
            value = Date()
        }
    }

private fun homeSmallMonoStyle(color: Color) = TextStyle(
    color = color,
    fontFamily = FontFamily.Monospace,
    fontSize = 10.sp,
    letterSpacing = 1.sp,
)

private fun homeWeatherDescription(code: Int?): String = when (code) {
    0 -> "Despejado"
    1, 2 -> "Algo nublado"
    3 -> "Cubierto"
    45, 48 -> "Niebla"
    in 51..57 -> "Llovizna"
    in 61..67 -> "Lluvia"
    in 71..77, 85, 86 -> "Nieve"
    in 80..82 -> "Chubascos"
    in 95..99 -> "Tormenta"
    else -> "Variable"
}
