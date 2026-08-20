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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vicent.veil.R
import dev.vicent.veil.launcher.model.ConnectionType
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
    val openClockLabel = stringResource(R.string.action_open_clock)

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
                VeilWordmark(
                    color = palette.accentActive,
                    modifier = Modifier.size(width = 29.dp, height = 6.dp),
                )
            }
            RailDivider()
            Spacer(Modifier.width(5.dp))
            contexts.forEachIndexed { index, context ->
                ContextIndicator(
                    kind = context.kind,
                    label = launcherContextLabel(context.kind),
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
                    fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.system,
                    fontSize = 11.sp,
                    letterSpacing = 0.6.sp,
                ),
                modifier = Modifier
                    .clickable(
                        role = Role.Button,
                        onClickLabel = openClockLabel,
                        onClick = onClockOpenRequested,
                    )
                    .padding(horizontal = 4.dp, vertical = 10.dp),
            )
            Spacer(Modifier.width(5.dp))
            RailDivider()
            Spacer(Modifier.width(8.dp))
            ConnectionGlyph(
                type = systemStatus.connectionType,
                signalLevel = systemStatus.connectionSignalLevel,
                modifier = Modifier.size(15.dp),
            )
            Spacer(Modifier.width(7.dp))
            BatteryGlyph(
                percent = systemStatus.batteryPercent,
                charging = systemStatus.isCharging,
                modifier = Modifier.size(width = 21.dp, height = 11.dp),
            )
            BasicText(
                text = systemStatus.batteryPercent?.let {
                    stringResource(R.string.tools_battery_percent, it)
                } ?: "—",
                style = TextStyle(
                    color = if (systemStatus.isCharging) palette.accentActive else palette.contentSecondary,
                    fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.system,
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
private fun ConnectionGlyph(
    type: ConnectionType,
    signalLevel: Int?,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    val level = signalLevel?.coerceIn(0, 4)
    val description = when (type) {
        ConnectionType.NONE -> stringResource(R.string.connection_none)
        ConnectionType.WIFI -> level?.let {
            stringResource(R.string.connection_wifi_signal, it)
        } ?: stringResource(R.string.connection_wifi_signal_unknown)
        ConnectionType.CELLULAR -> level?.let {
            stringResource(R.string.connection_cellular_signal, it)
        } ?: stringResource(R.string.connection_cellular_signal_unknown)
        ConnectionType.ETHERNET -> stringResource(R.string.connection_ethernet_connected)
        ConnectionType.OTHER -> stringResource(R.string.connection_network_connected)
    }
    Canvas(modifier.semantics { contentDescription = description }) {
        val strokeWidth = 1.dp.toPx()
        val stroke = Stroke(strokeWidth, cap = StrokeCap.Round)
        val inactive = palette.contentMuted.copy(alpha = .42f)

        fun levelColor(requiredLevel: Int) = when {
            level == null -> palette.contentMuted
            level >= requiredLevel -> palette.contentPrimary
            else -> inactive
        }

        when (type) {
            ConnectionType.WIFI -> {
                drawArc(
                    levelColor(4),
                    218f,
                    104f,
                    false,
                    topLeft = Offset(size.width * .02f, size.height * .05f),
                    size = Size(size.width * .96f, size.height * .92f),
                    style = stroke,
                )
                drawArc(
                    levelColor(3),
                    218f,
                    104f,
                    false,
                    topLeft = Offset(size.width * .19f, size.height * .29f),
                    size = Size(size.width * .62f, size.height * .62f),
                    style = stroke,
                )
                drawArc(
                    levelColor(2),
                    218f,
                    104f,
                    false,
                    topLeft = Offset(size.width * .35f, size.height * .52f),
                    size = Size(size.width * .30f, size.height * .30f),
                    style = stroke,
                )
                drawCircle(
                    levelColor(1),
                    radius = size.minDimension * .075f,
                    center = Offset(size.width * .50f, size.height * .84f),
                )
            }
            ConnectionType.CELLULAR -> {
                repeat(4) { index ->
                    val requiredLevel = index + 1
                    val barHeight = size.height * (.22f + index * .18f)
                    drawRoundRect(
                        color = levelColor(requiredLevel),
                        topLeft = Offset(size.width * (.06f + index * .235f), size.height * .90f - barHeight),
                        size = Size(size.width * .14f, barHeight),
                        cornerRadius = CornerRadius(strokeWidth * .5f),
                    )
                }
            }
            ConnectionType.ETHERNET -> {
                drawRoundRect(
                    palette.contentPrimary,
                    topLeft = Offset(size.width * .15f, size.height * .18f),
                    size = Size(size.width * .70f, size.height * .50f),
                    cornerRadius = CornerRadius(strokeWidth),
                    style = stroke,
                )
                drawLine(palette.contentPrimary, Offset(size.width * .38f, size.height * .68f), Offset(size.width * .38f, size.height * .84f), strokeWidth)
                drawLine(palette.contentPrimary, Offset(size.width * .62f, size.height * .68f), Offset(size.width * .62f, size.height * .84f), strokeWidth)
            }
            ConnectionType.OTHER -> {
                drawCircle(palette.contentSecondary, size.minDimension * .16f, Offset(size.width * .28f, size.height * .50f), style = stroke)
                drawCircle(palette.contentSecondary, size.minDimension * .16f, Offset(size.width * .72f, size.height * .50f), style = stroke)
                drawLine(palette.contentSecondary, Offset(size.width * .43f, size.height * .50f), Offset(size.width * .57f, size.height * .50f), strokeWidth)
            }
            ConnectionType.NONE -> {
                drawCircle(palette.contentMuted, size.minDimension * .29f, center, style = stroke)
                drawLine(
                    palette.contentMuted,
                    Offset(size.width * .26f, size.height * .74f),
                    Offset(size.width * .74f, size.height * .26f),
                    strokeWidth,
                    StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun BatteryGlyph(percent: Int?, charging: Boolean, modifier: Modifier = Modifier) {
    val palette = LocalVeilPalette.current
    val description = when {
        percent == null && charging -> stringResource(R.string.battery_charging_unknown)
        percent == null -> stringResource(R.string.battery_level_unknown)
        charging -> pluralStringResource(
            R.plurals.battery_charging_percent,
            percent,
            percent,
        )
        else -> pluralStringResource(R.plurals.battery_level_percent, percent, percent)
    }
    Canvas(modifier.semantics { contentDescription = description }) {
        val stroke = Stroke(1.dp.toPx())
        val frameColor = if (charging) palette.accentActive else palette.contentSecondary
        drawRect(frameColor, topLeft = Offset(0f, 0f), size = size.copy(width = size.width * .86f), style = stroke)
        drawRect(frameColor, topLeft = Offset(size.width * .88f, size.height * .28f), size = size.copy(width = size.width * .1f, height = size.height * .44f))
        val fill = percent?.coerceIn(0, 100)?.div(100f) ?: 0f
        drawRect(
            if (charging) palette.accentActive else palette.contentPrimary,
            topLeft = Offset(2.dp.toPx(), 2.dp.toPx()),
            size = size.copy(
                width = (size.width * .86f - 4.dp.toPx()).coerceAtLeast(0f) * fill,
                height = (size.height - 4.dp.toPx()).coerceAtLeast(0f),
            ),
        )
        if (charging) {
            val bolt = Path().apply {
                moveTo(size.width * .52f, size.height * .08f)
                lineTo(size.width * .33f, size.height * .52f)
                lineTo(size.width * .47f, size.height * .52f)
                lineTo(size.width * .40f, size.height * .92f)
                lineTo(size.width * .66f, size.height * .42f)
                lineTo(size.width * .51f, size.height * .42f)
                close()
            }
            drawPath(bolt, palette.contentPrimary)
        }
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
