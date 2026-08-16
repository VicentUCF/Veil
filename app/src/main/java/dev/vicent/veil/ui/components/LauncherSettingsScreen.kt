package dev.vicent.veil.ui.components

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.unit.dp
import dev.vicent.veil.config.AccentPalette
import dev.vicent.veil.BuildConfig
import dev.vicent.veil.launcher.model.AccentMode
import dev.vicent.veil.launcher.model.LauncherAccessState
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.LauncherPreferences
import dev.vicent.veil.launcher.model.HomeTextTone
import dev.vicent.veil.launcher.model.HomeTextWeight
import dev.vicent.veil.launcher.model.SettingsAppTarget
import dev.vicent.veil.ui.theme.LocalVeilPalette

@Composable
fun LauncherSettingsScreen(
    state: LauncherSettingsUiState,
    navigationActions: SettingsNavigationActions,
    appearanceActions: SettingsAppearanceActions,
    appActions: SettingsAppActions,
    accessActions: SettingsAccessActions,
    modifier: Modifier = Modifier,
) {
    val (preferences, access, installedApps, appTarget, showFontSettings, systemAccent) = state
    val (onBack, onOpenFontSettings) = navigationActions
    val (
        onAccentSelected,
        onHomeTextToneSelected,
        onHomeTextWeightSelected,
        onWallpaperScrimEnabledChanged,
        onWallpaperScrimIntensityChanged,
        onWallpaperSelected,
        onResetAppearance,
    ) = appearanceActions
    val (
        onOpenMusicProviderPicker,
        onSettingsAppSelected,
        onMusicProviderCleared,
    ) = appActions
    val (
        onContinuitySelected,
        onCalendarSelected,
        onLocationSelected,
        onAudioVisualizerSelected,
        onPrivacyPolicySelected,
        onFocusNotificationsSelected,
        onExactAlarmsSelected,
        onDefaultHomeSelected,
        onAndroidSettingsSelected,
    ) = accessActions
    val palette = LocalVeilPalette.current
    var showResetConfirmation by remember { mutableStateOf(false) }
    var showExternalError by remember { mutableStateOf(false) }
    var showPrivacyPolicy by remember { mutableStateOf(false) }

    fun launch(action: () -> Boolean) {
        if (!action()) showExternalError = true
    }

    if (appTarget != null) {
        SettingsAppPicker(
            target = appTarget,
            installedApps = installedApps,
            onBack = onBack,
            onSelected = onSettingsAppSelected,
            modifier = modifier,
        )
        return
    }

    if (showFontSettings) {
        CurrentHomeAppearanceSettings(
            preferences = preferences,
            onBack = onBack,
            onHomeTextToneSelected = onHomeTextToneSelected,
            onHomeTextWeightSelected = onHomeTextWeightSelected,
            onWallpaperScrimEnabledChanged = onWallpaperScrimEnabledChanged,
            onWallpaperScrimIntensityChanged = onWallpaperScrimIntensityChanged,
            modifier = modifier,
        )
        return
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
            item(key = "home-font") {
                SettingsActionRow(
                    title = "Fuente de CURRENT",
                    detail = "Texto e iconos ${preferences.homeTextTone.label().lowercase()}s · " +
                        preferences.homeTextWeight.label(),
                    status = "ABRIR",
                    onClick = onOpenFontSettings,
                )
            }

            item(key = "apps-label") { SettingsSectionLabel("APLICACIONES") }
            item(key = "music-provider") {
                val provider = preferences.musicProviderPackage?.let { packageName ->
                    installedApps.firstOrNull { it.packageName == packageName }
                }
                ConfiguredAppRow(
                    slotLabel = "Proveedor de música",
                    app = provider,
                    emptyDetail = "Elige qué app abre el estado vacío de MEDIA",
                    onClick = onOpenMusicProviderPicker,
                    onClear = if (preferences.musicProviderPackage != null) {
                        onMusicProviderCleared
                    } else null,
                )
            }
            item(key = "context-edit-hint") {
                SettingsDescription(
                    "Para cambiar las apps de una página, mantén pulsada su app en el workspace. Los huecos vacíos muestran +.",
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
            item(key = "privacy-policy") {
                SettingsActionRow(
                    title = "Política de privacidad",
                    detail = "Qué procesa Veil, dónde y durante cuánto tiempo",
                    status = "LEER",
                    onClick = { showPrivacyPolicy = true },
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
                    detail = "Restaura acento y legibilidad de CURRENT",
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
                "El acento volverá al coral; el texto y los iconos de CURRENT, a claros y finos. " +
                    "El filtro suave se activará. El wallpaper y los accesos de Android no cambiarán.",
            )
        }
    }

    if (showPrivacyPolicy) {
        RofiDialog(
            title = "política de privacidad",
            onDismiss = { showPrivacyPolicy = false },
            actions = {
                if (BuildConfig.PRIVACY_POLICY_URL.isNotBlank()) {
                    RofiAction("abrir web", { launch(onPrivacyPolicySelected) })
                }
                RofiAction("cerrar", { showPrivacyPolicy = false })
            },
        ) {
            RofiBody(
                "Veil no tiene cuentas, publicidad, analítica ni backend propio. " +
                    "Las apps instaladas, calendario, preferencias y notas se procesan en el dispositivo.\n\n" +
                    "Con permiso opcional, las señales de notificación se mantienen solo en memoria y " +
                    "sin contenido; el espectro de audio es transitorio y solo funciona con Veil visible, " +
                    "MEDIA visible y audio reproduciéndose.\n\n" +
                    "El tiempo envía ubicación aproximada a Open-Meteo por HTTPS. GAME consulta datos " +
                    "públicos de Steam por HTTPS. Estos proveedores reciben la IP necesaria para la conexión.\n\n" +
                    "Puedes revocar los accesos desde Android. Las notas rápidas se excluyen de copias y " +
                    "transferencias del dispositivo. La política pública y el contacto del editor se " +
                    "publican también en la ficha de distribución." +
                    if (BuildConfig.PRIVACY_CONTACT.isNotBlank()) {
                        "\n\nContacto: ${BuildConfig.PRIVACY_CONTACT}"
                    } else {
                        ""
                    },
            )
            if (BuildConfig.PRIVACY_POLICY_URL.isNotBlank()) {
                RofiBody(BuildConfig.PRIVACY_POLICY_URL)
            }
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

private fun Boolean.statusLabel(): String = if (this) "REVISAR" else "ACTIVAR"
