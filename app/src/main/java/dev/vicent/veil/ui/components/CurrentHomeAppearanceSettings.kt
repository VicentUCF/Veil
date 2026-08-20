package dev.vicent.veil.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vicent.veil.R
import dev.vicent.veil.launcher.model.HomeTextTone
import dev.vicent.veil.launcher.model.HomeTextWeight
import dev.vicent.veil.launcher.model.LauncherPreferences
import dev.vicent.veil.ui.theme.LocalVeilPalette
import kotlin.math.roundToInt

@Composable
internal fun CurrentHomeAppearanceSettings(
    preferences: LauncherPreferences,
    onBack: () -> Unit,
    onHomeTextToneSelected: (HomeTextTone) -> Unit,
    onHomeTextWeightSelected: (HomeTextWeight) -> Unit,
    onWallpaperScrimEnabledChanged: (Boolean) -> Unit,
    onWallpaperScrimIntensityChanged: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    val weightOptions = listOf(
        Triple(
            HomeTextWeight.LIGHT,
            stringResource(R.string.appearance_weight_light),
            FontWeight.Light,
        ),
        Triple(
            HomeTextWeight.REGULAR,
            stringResource(R.string.appearance_weight_regular),
            FontWeight.Normal,
        ),
        Triple(
            HomeTextWeight.SEMIBOLD,
            stringResource(R.string.appearance_weight_semibold),
            FontWeight.SemiBold,
        ),
    )
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(palette.drawerBackground)
            .windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        SettingsHeader(title = stringResource(R.string.appearance_header), onBack = onBack)
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "font-intro") {
                SettingsDescription(stringResource(R.string.appearance_intro))
            }
            item(key = "font-color-label") {
                SettingsSectionLabel(stringResource(R.string.appearance_section_color))
            }
            item(key = "home-text-tone-light") {
                AppearanceChoiceRow(
                    label = stringResource(R.string.appearance_tone_light),
                    detail = stringResource(R.string.appearance_tone_light_detail),
                    selected = preferences.homeTextTone == HomeTextTone.LIGHT,
                    previewColor = Color(0xFFE8E9E7),
                    previewBackground = Color(0xFF20262A),
                    previewWeight = FontWeight.Normal,
                    onClick = { onHomeTextToneSelected(HomeTextTone.LIGHT) },
                )
            }
            item(key = "home-text-tone-dark") {
                AppearanceChoiceRow(
                    label = stringResource(R.string.appearance_tone_dark),
                    detail = stringResource(R.string.appearance_tone_dark_detail),
                    selected = preferences.homeTextTone == HomeTextTone.DARK,
                    previewColor = Color(0xFF171A1C),
                    previewBackground = Color(0xFFE9E6DF),
                    previewWeight = FontWeight.Normal,
                    onClick = { onHomeTextToneSelected(HomeTextTone.DARK) },
                )
            }
            item(key = "font-weight-label") {
                SettingsSectionLabel(stringResource(R.string.appearance_section_weight))
            }
            items(
                items = weightOptions,
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
            item(key = "filter-label") {
                SettingsSectionLabel(
                    stringResource(R.string.appearance_section_wallpaper_filter),
                )
            }
            item(key = "wallpaper-filter") {
                SettingsActionRow(
                    title = stringResource(R.string.appearance_filter_title),
                    detail = if (preferences.wallpaperScrimEnabled) {
                        stringResource(R.string.appearance_filter_active_detail)
                    } else {
                        stringResource(R.string.appearance_filter_inactive_detail)
                    },
                    status = if (preferences.wallpaperScrimEnabled) {
                        stringResource(R.string.state_active)
                    } else {
                        stringResource(R.string.state_inactive)
                    },
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
                stringResource(R.string.appearance_intensity),
                style = workspaceMonoStyle(palette.contentSecondary, 8),
            )
            BasicText(
                text = stringResource(R.string.percentage_value, (normalized * 100).roundToInt()),
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
        BasicText(
            text = if (enabled) {
                stringResource(R.string.appearance_intensity_active_detail)
            } else {
                stringResource(R.string.appearance_intensity_inactive_detail)
            },
            style = workspaceMonoStyle(palette.contentMuted, 8),
        )
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
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 20.dp),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(32.dp)
                .clip(androidx.compose.foundation.shape.RoundedCornerShape(7.dp))
                .background(previewBackground)
                .border(
                    1.dp,
                    if (selected) palette.accentActive else palette.divider,
                    androidx.compose.foundation.shape.RoundedCornerShape(7.dp),
                ),
        ) {
            BasicText(
                stringResource(R.string.appearance_sample),
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
                label,
                style = TextStyle(
                    color = palette.contentPrimary,
                    fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            detail?.let { BasicText(it, style = workspaceMonoStyle(palette.contentMuted, 8)) }
        }
        if (selected) BasicText("✓", style = workspaceMonoStyle(palette.accentActive, 13))
    }
}

@Composable
internal fun HomeTextTone.label(): String = when (this) {
    HomeTextTone.LIGHT -> stringResource(R.string.appearance_tone_light)
    HomeTextTone.DARK -> stringResource(R.string.appearance_tone_dark)
}

@Composable
internal fun HomeTextTone.pluralLabel(): String = when (this) {
    HomeTextTone.LIGHT -> stringResource(R.string.appearance_tone_light_plural)
    HomeTextTone.DARK -> stringResource(R.string.appearance_tone_dark_plural)
}

@Composable
internal fun HomeTextWeight.label(): String = when (this) {
    HomeTextWeight.LIGHT -> stringResource(R.string.appearance_weight_light)
    HomeTextWeight.REGULAR -> stringResource(R.string.appearance_weight_regular)
    HomeTextWeight.SEMIBOLD -> stringResource(R.string.appearance_weight_semibold)
}
