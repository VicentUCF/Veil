package dev.vicent.veil.ui.components

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vicent.veil.config.AccentPalette
import dev.vicent.veil.launcher.model.AccentMode
import dev.vicent.veil.launcher.model.LauncherAccessState
import dev.vicent.veil.launcher.model.LauncherPreferences
import dev.vicent.veil.ui.theme.LocalVeilPalette

@Composable
fun LauncherSettingsScreen(
    preferences: LauncherPreferences,
    access: LauncherAccessState,
    systemAccent: Color?,
    onBack: () -> Unit,
    onAccentSelected: (AccentMode) -> Unit,
    onWallpaperSelected: () -> Boolean,
    onContinuitySelected: () -> Boolean,
    onCalendarSelected: () -> Boolean,
    onLocationSelected: () -> Boolean,
    onAudioVisualizerSelected: () -> Boolean,
    onFocusNotificationsSelected: () -> Boolean,
    onExactAlarmsSelected: () -> Boolean,
    onDefaultHomeSelected: () -> Boolean,
    onAndroidSettingsSelected: () -> Boolean,
    onResetAppearance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showExternalError by remember { mutableStateOf(false) }

    fun launch(action: () -> Boolean) {
        if (!action()) showExternalError = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.drawerBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        SettingsHeader(onBack = onBack)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "appearance-label") { SettingsSectionLabel("APARIENCIA") }
            item(key = "accent-intro") {
                SettingsDescription("Color de acento")
            }
            items(
                count = AccentPalette.presets.size,
                key = { "accent-${AccentPalette.presets[it].mode.persistedValue}" },
            ) { index ->
                val preset = AccentPalette.presets[index]
                AccentChoiceRow(
                    label = preset.label,
                    color = preset.color,
                    selected = preferences.accentMode == preset.mode,
                    enabled = true,
                    detail = null,
                    onClick = { onAccentSelected(preset.mode) },
                )
            }
            item(key = "accent-system") {
                val available = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && systemAccent != null
                AccentChoiceRow(
                    label = "Sistema / wallpaper",
                    color = systemAccent ?: palette.contentMuted,
                    selected = preferences.accentMode == AccentMode.SYSTEM,
                    enabled = available,
                    detail = if (available) {
                        "Color dinámico de Android"
                    } else {
                        "Disponible desde Android 12"
                    },
                    onClick = { onAccentSelected(AccentMode.SYSTEM) },
                )
            }
            item(key = "wallpaper") {
                SettingsActionRow(
                    title = "Fondo de pantalla",
                    detail = "Gestionado por Android; Veil no guarda la imagen",
                    status = "CAMBIAR",
                    onClick = { launch(onWallpaperSelected) },
                )
            }

            item(key = "access-label") { SettingsSectionLabel("ACCESOS Y PRIVACIDAD") }
            item(key = "continuity") {
                SettingsActionRow(
                    title = "Señales y Ambient Continuity",
                    detail = "Continuidad y puntos de actividad sin contenido",
                    status = access.continuityGranted.statusLabel(),
                    onClick = { launch(onContinuitySelected) },
                )
            }
            item(key = "calendar") {
                SettingsActionRow(
                    title = "Calendario",
                    detail = "Próximos eventos visibles en Android",
                    status = access.calendarGranted.statusLabel(),
                    onClick = { launch(onCalendarSelected) },
                )
            }
            item(key = "location") {
                SettingsActionRow(
                    title = "Ubicación aproximada",
                    detail = "Tiempo local mediante Open-Meteo",
                    status = access.approximateLocationGranted.statusLabel(),
                    onClick = { launch(onLocationSelected) },
                )
            }
            item(key = "audio") {
                SettingsActionRow(
                    title = "Espectro de audio",
                    detail = "FFT de baja calidad solo mientras MEDIA está visible",
                    status = access.audioVisualizerGranted.statusLabel(),
                    onClick = { launch(onAudioVisualizerSelected) },
                )
            }
            item(key = "focus-notifications") {
                SettingsActionRow(
                    title = "Notificaciones de Focus",
                    detail = "Aviso cuando termina un temporizador",
                    status = access.focusNotificationsGranted.statusLabel(),
                    onClick = { launch(onFocusNotificationsSelected) },
                )
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                item(key = "exact-alarms") {
                    SettingsActionRow(
                        title = "Alarmas exactas de Focus",
                        detail = if (access.exactAlarmsGranted) {
                            "Android permite alertas puntuales"
                        } else {
                            "Focus degradará a una alerta aproximada"
                        },
                        status = if (access.exactAlarmsGranted) "EXACTAS" else "APROXIMADAS",
                        onClick = { launch(onExactAlarmsSelected) },
                    )
                }
            }

            item(key = "system-label") { SettingsSectionLabel("SISTEMA") }
            item(key = "default-home") {
                SettingsActionRow(
                    title = "Launcher predeterminado",
                    detail = if (access.isDefaultHome) {
                        "Veil es la aplicación Home activa"
                    } else {
                        "Android aún no usa Veil como Home"
                    },
                    status = if (access.isDefaultHome) "ACTIVO" else "ELEGIR",
                    onClick = { launch(onDefaultHomeSelected) },
                )
            }
            item(key = "android-settings") {
                SettingsActionRow(
                    title = "Ajustes de Android",
                    detail = "Configuración completa del dispositivo",
                    status = "ABRIR",
                    onClick = { launch(onAndroidSettingsSelected) },
                )
            }

            item(key = "reset-label") { SettingsSectionLabel("RESTABLECER") }
            item(key = "reset") {
                SettingsActionRow(
                    title = "Restaurar apariencia de Veil",
                    detail = "Restablece únicamente el acento coral",
                    status = "RESTAURAR",
                    danger = true,
                    onClick = { showResetConfirmation = true },
                )
            }
            item(key = "bottom-space") { Spacer(modifier = Modifier.height(28.dp)) }
        }
    }

    if (showResetConfirmation) {
        RofiDialog(
            title = "restaurar apariencia",
            onDismiss = { showResetConfirmation = false },
            actions = {
                RofiAction("cancelar", { showResetConfirmation = false })
                RofiAction(
                    label = "restaurar",
                    danger = true,
                    onClick = {
                        showResetConfirmation = false
                        onResetAppearance()
                    },
                )
            },
        ) {
            RofiBody(
                "El acento volverá al coral de Veil. El fondo de pantalla y los accesos de Android no cambiarán.",
            )
        }
    }

    if (showExternalError) {
        RofiDialog(
            title = "ajuste no disponible",
            onDismiss = { showExternalError = false },
            actions = { RofiAction("cerrar", { showExternalError = false }) },
        ) {
            RofiBody("Android no ofrece una pantalla compatible para este ajuste en el dispositivo.")
        }
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
    val palette = LocalVeilPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(palette.fieldBackground.copy(alpha = 0.82f))
            .border(width = 0.dp, color = Color.Transparent),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(58.dp)
                .clickable(role = Role.Button, onClickLabel = "Volver", onClick = onBack),
        ) {
            BasicText("<", style = workspaceMonoStyle(palette.accentActive, 14))
        }
        BasicText(
            text = "> ajustes_de_veil",
            style = workspaceMonoStyle(palette.contentPrimary, 12),
        )
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    val palette = LocalVeilPalette.current
    BasicText(
        text = text,
        style = TextStyle(
            color = palette.contentMuted,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
        ),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 8.dp),
    )
}

@Composable
private fun SettingsDescription(text: String) {
    BasicText(
        text = text,
        style = workspaceMonoStyle(LocalVeilPalette.current.contentSecondary, 10),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

@Composable
private fun AccentChoiceRow(
    label: String,
    color: Color,
    selected: Boolean,
    enabled: Boolean,
    detail: String?,
    onClick: () -> Unit,
) {
    val palette = LocalVeilPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (detail == null) 52.dp else 62.dp)
            .semantics { this.selected = selected }
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp),
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            drawCircle(color = color, radius = 9.dp.toPx())
            if (selected) {
                drawCircle(
                    color = palette.contentPrimary,
                    radius = 13.dp.toPx(),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            BasicText(
                text = label,
                style = TextStyle(
                    color = if (enabled) palette.contentPrimary else palette.contentMuted,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            detail?.let {
                BasicText(it, style = workspaceMonoStyle(palette.contentMuted, 8))
            }
        }
        if (selected) {
            BasicText("✓", style = workspaceMonoStyle(palette.accentActive, 13))
        }
    }
}

@Composable
private fun SettingsActionRow(
    title: String,
    detail: String,
    status: String,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    val palette = LocalVeilPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClickLabel = "$status: $title", onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 13.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = if (danger) palette.error else palette.contentPrimary,
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            BasicText(
                text = detail,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = workspaceMonoStyle(palette.contentMuted, 8),
            )
        }
        BasicText(
            text = "[ ${status.lowercase()} ]",
            style = workspaceMonoStyle(if (danger) palette.error else palette.accentActive, 9),
        )
    }
}

private fun Boolean.statusLabel(): String = if (this) "REVISAR" else "ACTIVAR"
