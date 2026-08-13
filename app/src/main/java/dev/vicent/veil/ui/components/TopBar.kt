package dev.vicent.veil.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vicent.veil.launcher.model.LauncherContext
import dev.vicent.veil.launcher.model.SystemStatus
import dev.vicent.veil.ui.theme.LocalVeilPalette
import java.util.Date
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun TopBar(
    contexts: List<LauncherContext>,
    activeContextIndex: Int,
    onContextSelected: (Int) -> Unit,
    systemStatus: SystemStatus,
    onClockOpenRequested: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    val time by rememberSystemTime()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
            .height(40.dp)
            .background(palette.barBackground.copy(alpha = .94f))
            .padding(horizontal = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(width = 38.dp, height = 40.dp)) {
                VeilMark(Modifier.size(22.dp))
            }
            RailDivider()
            Spacer(Modifier.width(5.dp))
            contexts.forEachIndexed { index, context ->
                ContextIndicator(
                    kind = context.kind,
                    label = context.label,
                    isActive = index == activeContextIndex,
                    onClick = { onContextSelected(index) },
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicText(
                text = time,
                style = TextStyle(
                    color = palette.contentPrimary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp,
                ),
                modifier = Modifier
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Abrir Reloj",
                        onClick = onClockOpenRequested,
                    )
                    .padding(horizontal = 4.dp, vertical = 10.dp),
            )
            Spacer(Modifier.width(5.dp))
            RailDivider()
            Spacer(Modifier.width(8.dp))
            ConnectionGlyph(
                connected = systemStatus.connectionLabel != "Sin conexión",
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(7.dp))
            BatteryGlyph(
                percent = systemStatus.batteryPercent,
                charging = systemStatus.isCharging,
                modifier = Modifier.size(width = 21.dp, height = 11.dp),
            )
            BasicText(
                text = systemStatus.batteryPercent?.let { "$it%" } ?: "—",
                style = TextStyle(
                    color = palette.contentSecondary,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                ),
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun RailDivider() {
    val color = LocalVeilPalette.current.divider
    Canvas(Modifier.width(1.dp).height(22.dp)) {
        drawLine(color, Offset(0f, 0f), Offset(0f, size.height), 1.dp.toPx())
    }
}

@Composable
private fun VeilMark(modifier: Modifier = Modifier) {
    val palette = LocalVeilPalette.current
    Canvas(modifier) {
        val path = Path().apply {
            moveTo(size.width * .5f, size.height * .12f)
            lineTo(size.width * .86f, size.height * .82f)
            lineTo(size.width * .16f, size.height * .82f)
            close()
        }
        drawPath(path, palette.accentActive, style = Stroke(1.25.dp.toPx()))
        drawLine(
            palette.accentActive,
            Offset(size.width * .33f, size.height * .64f),
            Offset(size.width * .68f, size.height * .64f),
            1.1.dp.toPx(),
        )
    }
}

@Composable
private fun ConnectionGlyph(connected: Boolean, modifier: Modifier = Modifier) {
    val palette = LocalVeilPalette.current
    val color = if (connected) palette.contentPrimary else palette.contentMuted
    Canvas(modifier) {
        val stroke = 1.dp.toPx()
        drawArc(color, 218f, 104f, false, topLeft = Offset(size.width * .05f, size.height * .10f), size = size.copy(width = size.width * .9f, height = size.height * .9f), style = Stroke(stroke))
        drawArc(color, 218f, 104f, false, topLeft = Offset(size.width * .25f, size.height * .34f), size = size.copy(width = size.width * .5f, height = size.height * .5f), style = Stroke(stroke))
        drawCircle(color, radius = 1.2.dp.toPx(), center = Offset(size.width / 2f, size.height * .82f))
    }
}

@Composable
private fun BatteryGlyph(percent: Int?, charging: Boolean, modifier: Modifier = Modifier) {
    val palette = LocalVeilPalette.current
    Canvas(modifier) {
        val stroke = Stroke(1.dp.toPx())
        drawRect(palette.contentSecondary, topLeft = Offset(0f, 0f), size = size.copy(width = size.width * .86f), style = stroke)
        drawRect(palette.contentSecondary, topLeft = Offset(size.width * .88f, size.height * .28f), size = size.copy(width = size.width * .1f, height = size.height * .44f))
        val fill = percent?.coerceIn(0, 100)?.div(100f) ?: 0f
        drawRect(
            if (charging) palette.accentActive else palette.contentPrimary,
            topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
            size = size.copy(
                width = (size.width * .86f - 4.dp.toPx()).coerceAtLeast(0f) * fill,
                height = (size.height - 4.dp.toPx()).coerceAtLeast(0f),
            ),
        )
    }
}

@Composable
private fun rememberSystemTime(): androidx.compose.runtime.State<String> {
    val context = LocalContext.current
    val timeFormatter = DateFormat.getTimeFormat(context)

    return produceState(initialValue = timeFormatter.format(Date()), timeFormatter) {
        while (isActive) {
            delay(15_000)
            value = timeFormatter.format(Date())
        }
    }
}
