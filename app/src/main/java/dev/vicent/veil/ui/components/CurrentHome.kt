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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.semantics.Role
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
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun CurrentHome(
    state: LauncherUiState,
    apps: List<LauncherApp>,
    onAppSelected: (LauncherApp) -> Unit,
    onAppLongPressed: (LauncherApp) -> Unit,
    onLocationPermissionRequested: () -> Unit,
    onContinuityAction: (String, ContinuityAction, Long?) -> Unit,
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
                .padding(start = 22.dp, end = 78.dp),
        ) {
            Spacer(Modifier.height(upperBreathingRoom))
            HomeClockAndWeather(
                state = state,
                onLocationPermissionRequested = onLocationPermissionRequested,
            )

            if (playingMedia != null && dismissedMediaId != playingMedia.id) {
                CompactMediaPlayer(
                    media = playingMedia,
                    onAction = onContinuityAction,
                    onDismiss = { dismissedMediaId = playingMedia.id },
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
) {
    val palette = LocalVeilPalette.current
    val context = LocalContext.current
    val locale = Locale.forLanguageTag(LocalLocale.current.toLanguageTag())
    val now by rememberCurrentTime()
    val weather = state.weather
    val uriHandler = LocalUriHandler.current

    BasicText(
        text = DateFormat.getTimeFormat(context).format(now),
        style = TextStyle(
            color = palette.contentPrimary,
            fontFamily = FontFamily.SansSerif,
            fontWeight = FontWeight.ExtraLight,
            fontSize = 59.sp,
            letterSpacing = 1.5.sp,
        ),
    )
    BasicText(
        text = SimpleDateFormat("EEEE, d MMMM", locale).format(now),
        style = TextStyle(
            color = palette.contentSecondary,
            fontFamily = FontFamily.SansSerif,
            fontSize = 12.sp,
            letterSpacing = 0.7.sp,
        ),
        modifier = Modifier.padding(top = 1.dp),
    )
    Canvas(Modifier.padding(top = 13.dp).width(210.dp).height(1.dp)) {
        drawLine(
            palette.contentSecondary,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 1.dp.toPx(),
        )
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 12.dp).height(42.dp),
    ) {
        WeatherGlyph(
            weatherCode = weather.weatherCode,
            modifier = Modifier.size(34.dp),
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
                    BasicText(
                        text = (if (weather.isStale) "DESACTUALIZADO · " else "") + "OPEN-METEO",
                        style = TextStyle(
                            color = palette.contentMuted,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 8.sp,
                            letterSpacing = 0.8.sp,
                        ),
                        modifier = Modifier
                            .clickable { uriHandler.openUri("https://open-meteo.com/") }
                            .padding(top = 3.dp, bottom = 3.dp),
                    )
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
private fun WeatherGlyph(weatherCode: Int?, modifier: Modifier = Modifier) {
    val color = LocalVeilPalette.current.contentPrimary
    Canvas(modifier) {
        val stroke = Stroke(1.1.dp.toPx())
        val rainy = weatherCode in 51..99
        drawCircle(color, radius = size.minDimension * .18f, center = center.copy(x = size.width * .38f), style = stroke)
        drawCircle(color, radius = size.minDimension * .23f, center = center.copy(x = size.width * .57f, y = size.height * .55f), style = stroke)
        drawLine(color, start = center.copy(x = size.width * .22f, y = size.height * .72f), end = center.copy(x = size.width * .78f, y = size.height * .72f), strokeWidth = stroke.width)
        if (rainy) {
            drawLine(color, start = center.copy(x = size.width * .40f, y = size.height * .81f), end = center.copy(x = size.width * .34f, y = size.height * .93f), strokeWidth = stroke.width)
            drawLine(color, start = center.copy(x = size.width * .62f, y = size.height * .81f), end = center.copy(x = size.width * .56f, y = size.height * .93f), strokeWidth = stroke.width)
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
            .clickable(enabled = ContinuityAction.OPEN in media.supportedActions) {
                onAction(media.id, ContinuityAction.OPEN, null)
            }
            .padding(horizontal = 8.dp),
    ) {
        if (media.artwork != null) {
            Image(
                bitmap = remember(media.artwork) { media.artwork.asImageBitmap() },
                contentDescription = null,
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(7.dp)),
            )
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(7.dp))
                    .background(Color.White.copy(alpha = .055f)),
            ) {
                ActivityGlyph(ActivityGlyphKind.MEDIA, size = 22.dp, isActive = true)
            }
        }
        Column(modifier = Modifier.weight(1f).padding(horizontal = 10.dp)) {
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
        if (ContinuityAction.TOGGLE_PLAYBACK in media.supportedActions) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(42.dp)
                    .clickable(role = Role.Button, onClickLabel = "Pausar") {
                        onAction(media.id, ContinuityAction.TOGGLE_PLAYBACK, null)
                    },
            ) {
                Canvas(Modifier.size(14.dp)) {
                    drawRect(palette.contentPrimary, topLeft = center.copy(x = size.width * .24f, y = 0f), size = size.copy(width = 2.dp.toPx()))
                    drawRect(palette.contentPrimary, topLeft = center.copy(x = size.width * .66f, y = 0f), size = size.copy(width = 2.dp.toPx()))
                }
            }
        }
    }
}

@Composable
private fun HomeAppRow(
    app: LauncherApp,
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
            ),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.width(47.dp)) {
            ActivityGlyph(kind = app.activityGlyph(), size = 27.dp)
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
    in 71..77 -> "Nieve"
    in 80..82 -> "Chubascos"
    in 95..99 -> "Tormenta"
    else -> "Variable"
}
