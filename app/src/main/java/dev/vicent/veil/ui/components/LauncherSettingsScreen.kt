package dev.vicent.veil.ui.components

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicTextField
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vicent.veil.config.AccentPalette
import dev.vicent.veil.launcher.model.AccentMode
import dev.vicent.veil.launcher.model.AppCategory
import dev.vicent.veil.launcher.model.LauncherAccessState
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.LauncherPreferences
import dev.vicent.veil.launcher.model.HomeTextTone
import dev.vicent.veil.launcher.model.HomeTextWeight
import dev.vicent.veil.launcher.model.SettingsAppTarget
import dev.vicent.veil.ui.theme.LocalVeilPalette
import java.text.Normalizer
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun LauncherSettingsScreen(
    preferences: LauncherPreferences,
    access: LauncherAccessState,
    installedApps: List<LauncherApp>,
    appTarget: SettingsAppTarget?,
    showFontSettings: Boolean,
    systemAccent: Color?,
    onBack: () -> Unit,
    onOpenFontSettings: () -> Unit,
    onAccentSelected: (AccentMode) -> Unit,
    onHomeTextToneSelected: (HomeTextTone) -> Unit,
    onHomeTextWeightSelected: (HomeTextWeight) -> Unit,
    onWallpaperScrimEnabledChanged: (Boolean) -> Unit,
    onWallpaperScrimIntensityChanged: (Float) -> Unit,
    onOpenMusicProviderPicker: () -> Unit,
    onSettingsAppSelected: (String) -> Unit,
    onMusicProviderCleared: () -> Unit,
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
        CurrentHomeFontSettings(
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
private fun CurrentHomeFontSettings(
    preferences: LauncherPreferences,
    onBack: () -> Unit,
    onHomeTextToneSelected: (HomeTextTone) -> Unit,
    onHomeTextWeightSelected: (HomeTextWeight) -> Unit,
    onWallpaperScrimEnabledChanged: (Boolean) -> Unit,
    onWallpaperScrimIntensityChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.drawerBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        SettingsHeader(title = "fuente_current", onBack = onBack)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "font-intro") {
                SettingsDescription(
                    "Ajusta el texto y los iconos que CURRENT dibuja sobre el wallpaper. " +
                        "El filtro suave asociado se aplica a todas las vistas. " +
                        "Los cambios se guardan al tocarlos.",
                )
            }
            item(key = "font-color-label") { SettingsSectionLabel("COLOR") }
            item(key = "home-text-tone-light") {
                AppearanceChoiceRow(
                    label = "Claro",
                    detail = "Texto marfil · filtro negro suave",
                    selected = preferences.homeTextTone == HomeTextTone.LIGHT,
                    previewColor = Color(0xFFE8E9E7),
                    previewBackground = Color(0xFF20262A),
                    previewWeight = FontWeight.Normal,
                    onClick = { onHomeTextToneSelected(HomeTextTone.LIGHT) },
                )
            }
            item(key = "home-text-tone-dark") {
                AppearanceChoiceRow(
                    label = "Oscuro",
                    detail = "Texto carbón · filtro blanco suave",
                    selected = preferences.homeTextTone == HomeTextTone.DARK,
                    previewColor = Color(0xFF171A1C),
                    previewBackground = Color(0xFFE9E6DF),
                    previewWeight = FontWeight.Normal,
                    onClick = { onHomeTextToneSelected(HomeTextTone.DARK) },
                )
            }
            item(key = "font-weight-label") { SettingsSectionLabel("GROSOR") }
            items(
                items = listOf(
                    Triple(HomeTextWeight.LIGHT, "Fino", FontWeight.Light),
                    Triple(HomeTextWeight.REGULAR, "Normal", FontWeight.Normal),
                    Triple(HomeTextWeight.SEMIBOLD, "Seminegrita", FontWeight.SemiBold),
                ),
                key = { "home-weight-${it.first.persistedValue}" },
            ) { (mode, label, weight) ->
                AppearanceChoiceRow(
                    label = label,
                    detail = null,
                    selected = preferences.homeTextWeight == mode,
                    previewColor = Color(0xFFE8E9E7),
                    previewBackground = Color(0xFF20262A),
                    previewWeight = weight,
                    onClick = { onHomeTextWeightSelected(mode) },
                )
            }
            item(key = "filter-label") { SettingsSectionLabel("FILTRO DEL WALLPAPER") }
            item(key = "wallpaper-filter") {
                SettingsActionRow(
                    title = "Filtro suave",
                    detail = if (preferences.wallpaperScrimEnabled) {
                        "Activo en todas las vistas; el tono sigue el color elegido"
                    } else {
                        "El wallpaper se muestra sin velo adicional"
                    },
                    status = if (preferences.wallpaperScrimEnabled) "ACTIVO" else "INACTIVO",
                    onClick = {
                        onWallpaperScrimEnabledChanged(!preferences.wallpaperScrimEnabled)
                    },
                )
            }
            item(key = "wallpaper-filter-intensity") {
                WallpaperFilterIntensitySlider(
                    intensity = preferences.wallpaperScrimIntensity,
                    enabled = preferences.wallpaperScrimEnabled,
                    onChanged = onWallpaperScrimIntensityChanged,
                )
            }
            item(key = "bottom-space") { Spacer(modifier = Modifier.height(28.dp)) }
        }
    }
}

@Composable
private fun WallpaperFilterIntensitySlider(
    intensity: Float,
    enabled: Boolean,
    onChanged: (Float) -> Unit,
) {
    val palette = LocalVeilPalette.current
    val normalized = intensity.coerceIn(0f, 1f)
    val activeColor = if (enabled) palette.accentActive else palette.contentMuted
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            BasicText(
                text = "INTENSIDAD",
                style = workspaceMonoStyle(palette.contentSecondary, 8),
            )
            BasicText(
                text = "${(normalized * 100).roundToInt()}%",
                style = workspaceMonoStyle(palette.contentMuted, 8),
            )
        }
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(32.dp)
                .semantics {
                    progressBarRangeInfo = ProgressBarRangeInfo(normalized, 0f..1f)
                    setProgress { target ->
                        onChanged(target.coerceIn(0f, 1f))
                        true
                    }
                }
                .pointerInput(Unit) {
                    detectTapGestures { position ->
                        onChanged((position.x / size.width).coerceIn(0f, 1f))
                    }
                }
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragStart = { position ->
                            onChanged((position.x / size.width).coerceIn(0f, 1f))
                        },
                        onHorizontalDrag = { change, _ ->
                            change.consume()
                            onChanged((change.position.x / size.width).coerceIn(0f, 1f))
                        },
                    )
                },
        ) {
            val y = size.height / 2f
            val progressX = size.width * normalized
            drawLine(palette.divider, Offset(0f, y), Offset(size.width, y), 3.dp.toPx())
            drawLine(activeColor, Offset(0f, y), Offset(progressX, y), 3.dp.toPx())
            drawCircle(palette.contentPrimary, 4.dp.toPx(), Offset(progressX, y))
        }
        if (!enabled) {
            BasicText(
                text = "Se conservará para cuando actives el filtro",
                style = workspaceMonoStyle(palette.contentMuted, 8),
            )
        } else {
            BasicText(
                text = "La intensidad aumenta progresivamente en el tramo alto",
                style = workspaceMonoStyle(palette.contentMuted, 8),
            )
        }
    }
}

@Composable
private fun SettingsAppPicker(
    target: SettingsAppTarget,
    installedApps: List<LauncherApp>,
    onBack: () -> Unit,
    onSelected: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    var query by remember(target) { mutableStateOf("") }
    val normalizedQuery = remember(query) { query.normalizeAppSearch() }
    val apps = remember(installedApps, normalizedQuery, target) {
        installedApps.asSequence()
            .filter { app ->
                normalizedQuery.isBlank() ||
                    "${app.label} ${app.packageName}".normalizeAppSearch().contains(normalizedQuery)
            }
            .sortedWith(
                compareBy<LauncherApp> {
                    if (target == SettingsAppTarget.MusicProvider && it.category == AppCategory.MEDIA) 0 else 1
                }.thenBy { it.label.lowercase(Locale.getDefault()) },
            )
            .toList()
    }
    val title = when (target) {
        SettingsAppTarget.MusicProvider -> "elegir_proveedor_de_musica"
        is SettingsAppTarget.ContextSlot ->
            "${target.kind.name.lowercase()}_slot_${target.slotIndex + 1}"
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.drawerBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding(),
    ) {
        SettingsHeader(title = title, onBack = onBack)
        BasicTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            textStyle = workspaceMonoStyle(palette.contentPrimary, 11),
            cursorBrush = SolidColor(palette.accentActive),
            decorationBox = { inner ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(7.dp))
                        .background(palette.fieldBackground)
                        .border(
                            1.dp,
                            palette.divider,
                            androidx.compose.foundation.shape.RoundedCornerShape(7.dp),
                        )
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                ) {
                    if (query.isBlank()) {
                        BasicText(
                            "Buscar aplicación",
                            style = workspaceMonoStyle(palette.contentMuted, 11),
                        )
                    }
                    inner()
                }
            },
        )
        if (target == SettingsAppTarget.MusicProvider) {
            SettingsDescription(
                "Las apps de audio y vídeo aparecen primero. La reproducción activa seguirá cualquier MediaSession compatible.",
            )
        }
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(apps, key = LauncherApp::packageName) { app ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            role = Role.Button,
                            onClickLabel = "Elegir ${app.label}",
                        ) { onSelected(app.packageName) }
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                ) {
                    LauncherAppIcon(app = app, size = 38.dp)
                    Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
                        BasicText(
                            app.label,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = TextStyle(
                                color = palette.contentPrimary,
                                fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Medium,
                            ),
                        )
                        BasicText(
                            app.packageName,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            style = workspaceMonoStyle(palette.contentMuted, 8),
                        )
                    }
                    BasicText(">", style = workspaceMonoStyle(palette.accentActive, 11))
                }
            }
            if (apps.isEmpty()) {
                item(key = "empty") {
                    SettingsDescription("No hay aplicaciones que coincidan con la búsqueda.")
                }
            }
            item(key = "bottom-space") { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
private fun ConfiguredAppRow(
    slotLabel: String,
    app: LauncherApp?,
    emptyDetail: String,
    onClick: () -> Unit,
    onClear: (() -> Unit)?,
) {
    val palette = LocalVeilPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = if (app == null) "Elegir aplicación" else "Cambiar ${app.label}",
                onClick = onClick,
            )
            .padding(start = 20.dp, end = 10.dp, top = 9.dp, bottom = 9.dp),
    ) {
        if (app != null) {
            LauncherAppIcon(app = app, size = 38.dp)
        } else {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(38.dp)
                    .border(
                        1.dp,
                        palette.divider,
                        androidx.compose.foundation.shape.RoundedCornerShape(9.dp),
                    ),
            ) {
                BasicText("+", style = workspaceMonoStyle(palette.contentMuted, 14))
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            BasicText(slotLabel, style = workspaceMonoStyle(palette.contentMuted, 8))
            BasicText(
                text = app?.label ?: "Sin aplicación",
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = palette.contentPrimary,
                    fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            BasicText(
                text = app?.packageName ?: emptyDetail,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = workspaceMonoStyle(palette.contentMuted, 8),
            )
        }
        onClear?.let { clear ->
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        role = Role.Button,
                        onClickLabel = "Vaciar $slotLabel",
                        onClick = clear,
                    ),
            ) {
                BasicText("×", style = workspaceMonoStyle(palette.error, 14))
            }
        }
        BasicText(">", style = workspaceMonoStyle(palette.accentActive, 10))
    }
}

@Composable
private fun SettingsHeader(
    title: String = "ajustes_de_veil",
    onBack: () -> Unit,
) {
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
            text = "> $title",
            style = workspaceMonoStyle(palette.contentPrimary, 12),
        )
    }
}

private fun String.normalizeAppSearch(): String = Normalizer
    .normalize(lowercase(Locale.getDefault()), Normalizer.Form.NFD)
    .replace(Regex("\\p{Mn}+"), "")

@Composable
private fun SettingsSectionLabel(text: String) {
    val palette = LocalVeilPalette.current
    BasicText(
        text = text,
        style = TextStyle(
            color = palette.contentMuted,
            fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.system,
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
                    fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
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
private fun AppearanceChoiceRow(
    label: String,
    detail: String?,
    selected: Boolean,
    previewColor: Color,
    previewBackground: Color,
    previewWeight: FontWeight,
    onClick: () -> Unit,
) {
    val palette = LocalVeilPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (detail == null) 52.dp else 60.dp)
            .semantics { this.selected = selected }
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(7.dp))
                .background(previewBackground)
                .border(
                    width = 1.dp,
                    color = if (selected) palette.accentActive else palette.divider,
                    shape = androidx.compose.foundation.shape.RoundedCornerShape(7.dp),
                ),
        ) {
            BasicText(
                text = "Aa",
                style = TextStyle(
                    color = previewColor,
                    fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
                    fontSize = 12.sp,
                    fontWeight = previewWeight,
                ),
            )
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            BasicText(
                text = label,
                style = TextStyle(
                    color = palette.contentPrimary,
                    fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
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
                    fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
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

private fun HomeTextTone.label(): String = when (this) {
    HomeTextTone.LIGHT -> "Claro"
    HomeTextTone.DARK -> "Oscuro"
}

private fun HomeTextWeight.label(): String = when (this) {
    HomeTextWeight.LIGHT -> "Fino"
    HomeTextWeight.REGULAR -> "Normal"
    HomeTextWeight.SEMIBOLD -> "Seminegrita"
}
