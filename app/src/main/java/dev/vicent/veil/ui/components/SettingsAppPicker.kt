package dev.vicent.veil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vicent.veil.R
import dev.vicent.veil.launcher.model.AppCategory
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.SettingsAppTarget
import dev.vicent.veil.ui.theme.LocalVeilPalette
import java.text.Normalizer
import java.util.Locale

@Composable
internal fun SettingsAppPicker(
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
        SettingsAppTarget.MusicProvider -> stringResource(R.string.picker_music_title)
        is SettingsAppTarget.ContextSlot -> stringResource(
            R.string.picker_context_slot_title,
            target.kind.name.lowercase(),
            target.slotIndex + 1,
        )
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
                            stringResource(R.string.picker_search_hint),
                            style = workspaceMonoStyle(palette.contentMuted, 11),
                        )
                    }
                    inner()
                }
            },
        )
        if (target == SettingsAppTarget.MusicProvider) {
            SettingsDescription(
                stringResource(R.string.picker_music_description),
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
                            onClickLabel = stringResource(R.string.action_choose_named, app.label),
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
                    SettingsDescription(stringResource(R.string.picker_no_matches))
                }
            }
            item(key = "bottom-space") { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

@Composable
internal fun ConfiguredAppRow(
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
                onClickLabel = if (app == null) {
                    stringResource(R.string.action_choose_app)
                } else {
                    stringResource(R.string.action_change_named, app.label)
                },
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
                text = app?.label ?: stringResource(R.string.picker_no_app),
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
                        onClickLabel = stringResource(R.string.action_clear_named, slotLabel),
                        onClick = clear,
                    ),
            ) {
                BasicText("×", style = workspaceMonoStyle(palette.error, 14))
            }
        }
        BasicText(">", style = workspaceMonoStyle(palette.accentActive, 10))
    }
}

private fun String.normalizeAppSearch(): String = Normalizer
    .normalize(lowercase(Locale.getDefault()), Normalizer.Form.NFD)
    .replace("\\p{M}+".toRegex(), "")
