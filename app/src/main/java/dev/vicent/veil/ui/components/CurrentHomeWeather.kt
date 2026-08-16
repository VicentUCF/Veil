package dev.vicent.veil.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vicent.veil.launcher.model.WeatherAvailability
import dev.vicent.veil.ui.theme.LocalVeilPalette
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
internal fun HomeClockAndWeather(
    state: CurrentHomeUiState,
    onLocationPermissionRequested: () -> Unit,
    onClockOpenRequested: () -> Unit,
    onCalendarOpenRequested: () -> Unit,
) {
    val appearance = LocalCurrentHomeAppearance.current
    val context = LocalContext.current
    val locale = Locale.forLanguageTag(LocalLocale.current.toLanguageTag())
    val now by rememberCurrentTime()
    val weather = state.weather

    BasicText(
        DateFormat.getTimeFormat(context).format(now),
        style = TextStyle(
            color = appearance.primary,
            fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
            fontWeight = appearance.clockWeight,
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
        SimpleDateFormat("EEEE, d MMMM yyyy", locale).format(now),
        style = TextStyle(
            color = appearance.secondary,
            fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
            fontSize = 12.sp,
            fontWeight = appearance.contentWeight,
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
            appearance.secondary,
            Offset(0f, size.height / 2f),
            Offset(size.width, size.height / 2f),
            1.dp.toPx(),
        )
    }
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(44.dp)) {
        WeatherGlyph(weather.weatherCode, Modifier.size(36.dp))
        Column(modifier = Modifier.padding(start = 2.dp)) {
            when (weather.availability) {
                WeatherAvailability.AVAILABLE -> {
                    BasicText(
                        "${weather.temperatureCelsius?.roundToInt() ?: "—"}° ${weatherDescription(weather.weatherCode)}",
                        style = TextStyle(
                            color = appearance.primary,
                            fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
                            fontSize = 15.sp,
                            fontWeight = appearance.contentWeight,
                        ),
                    )
                    if (weather.isStale) HomeWeatherLabel("DESACTUALIZADO")
                }
                WeatherAvailability.NEEDS_PERMISSION -> HomeWeatherAction(
                    "ACTIVAR TIEMPO",
                    onLocationPermissionRequested,
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
        label,
        style = homeSmallMonoStyle(LocalCurrentHomeAppearance.current.primary),
        modifier = Modifier
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 10.dp),
    )
}

@Composable
private fun HomeWeatherLabel(label: String) {
    BasicText(label, style = homeSmallMonoStyle(LocalCurrentHomeAppearance.current.secondary))
}

@Composable
internal fun WeatherGlyph(weatherCode: Int?, modifier: Modifier = Modifier) {
    val palette = LocalVeilPalette.current
    val appearance = LocalCurrentHomeAppearance.current
    Canvas(modifier) {
        val unit = size.minDimension
        val strokeWidth = (unit * .027f).coerceAtLeast(1.dp.toPx())
        val stroke = Stroke(strokeWidth, cap = StrokeCap.Round)
        val glyphColor = appearance.primary

        fun sun(at: Offset, radius: Float) {
            drawCircle(palette.accentActive, radius, at, style = stroke)
            repeat(8) { index ->
                val angle = Math.toRadians((index * 45 - 90).toDouble())
                drawLine(
                    palette.accentActive,
                    Offset(
                        at.x + cos(angle).toFloat() * radius * 1.38f,
                        at.y + sin(angle).toFloat() * radius * 1.38f,
                    ),
                    Offset(
                        at.x + cos(angle).toFloat() * radius * 1.82f,
                        at.y + sin(angle).toFloat() * radius * 1.82f,
                    ),
                    strokeWidth,
                    StrokeCap.Round,
                )
            }
        }

        fun cloud(yOffset: Float = 0f) {
            val path = Path().apply {
                moveTo(unit * .25f, unit * (.60f + yOffset))
                cubicTo(unit * .20f, unit * (.48f + yOffset), unit * .29f, unit * (.40f + yOffset), unit * .39f, unit * (.43f + yOffset))
                cubicTo(unit * .45f, unit * (.28f + yOffset), unit * .68f, unit * (.32f + yOffset), unit * .69f, unit * (.48f + yOffset))
                cubicTo(unit * .82f, unit * (.48f + yOffset), unit * .84f, unit * (.66f + yOffset), unit * .70f, unit * (.67f + yOffset))
                lineTo(unit * .34f, unit * (.67f + yOffset))
                cubicTo(unit * .27f, unit * (.67f + yOffset), unit * .23f, unit * (.64f + yOffset), unit * .25f, unit * (.60f + yOffset))
            }
            drawPath(path, glyphColor, style = stroke)
        }

        fun rain(snow: Boolean = false, drizzle: Boolean = false) {
            listOf(.36f, .52f, .68f).forEachIndexed { index, x ->
                if (snow) {
                    val at = Offset(unit * x, unit * (.77f + if (index == 1) .04f else 0f))
                    drawLine(glyphColor, at.copy(x = at.x - unit * .035f), at.copy(x = at.x + unit * .035f), strokeWidth, cap = StrokeCap.Round)
                    drawLine(glyphColor, at.copy(y = at.y - unit * .035f), at.copy(y = at.y + unit * .035f), strokeWidth, cap = StrokeCap.Round)
                } else {
                    val length = if (drizzle) .055f else .09f
                    drawLine(palette.accentActive, Offset(unit * (x + .025f), unit * .73f), Offset(unit * (x - .025f), unit * (.73f + length)), strokeWidth, cap = StrokeCap.Round)
                }
            }
        }

        when (weatherCode) {
            0 -> sun(center, unit * .16f)
            1, 2 -> { sun(Offset(unit * .38f, unit * .39f), unit * .09f); cloud(.02f) }
            3 -> cloud()
            45, 48 -> {
                cloud(-.05f)
                drawLine(glyphColor, Offset(unit * .30f, unit * .72f), Offset(unit * .70f, unit * .72f), strokeWidth, cap = StrokeCap.Round)
                drawLine(glyphColor, Offset(unit * .36f, unit * .79f), Offset(unit * .64f, unit * .79f), strokeWidth, cap = StrokeCap.Round)
            }
            in 51..57 -> { cloud(-.04f); rain(drizzle = true) }
            in 61..67, in 80..82 -> { cloud(-.04f); rain() }
            in 71..77, 85, 86 -> { cloud(-.04f); rain(snow = true) }
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
            else -> drawLine(glyphColor, Offset(unit * .34f, center.y), Offset(unit * .66f, center.y), strokeWidth, cap = StrokeCap.Round)
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

internal fun weatherDescription(code: Int?): String = when (code) {
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
