package dev.vicent.veil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vicent.veil.launcher.ResolvedQuickAction
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.SettingsShortcut
import dev.vicent.veil.ui.theme.LocalVeilPalette

/** A stable, context-owned application dock. CURRENT intentionally does not render it. */
@Composable
fun ContextDock(
    actions: List<ResolvedQuickAction>,
    settingsShortcuts: List<SettingsShortcut>,
    onAppSelected: (LauncherApp) -> Unit,
    onAppLongPressed: (LauncherApp) -> Unit,
    onSettingSelected: (SettingsShortcut) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFF101418).copy(alpha = 0.84f))
            .border(1.dp, palette.divider, RoundedCornerShape(16.dp))
            .padding(horizontal = 7.dp, vertical = 8.dp),
    ) {
        actions.take(5).forEach { action ->
            val app = (action as? ResolvedQuickAction.App)?.app
            val setting = (action as? ResolvedQuickAction.Setting)?.let { resolved ->
                settingsShortcuts.firstOrNull { it.id == resolved.id }
            }
            val label = app?.label ?: setting?.label ?: return@forEach
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        role = Role.Button,
                        onClickLabel = "Abrir $label",
                        onLongClickLabel = app?.let { "Opciones de $label" },
                        onClick = {
                            if (app != null) onAppSelected(app)
                            else if (setting != null) onSettingSelected(setting)
                        },
                        onLongClick = app?.let { selected -> { onAppLongPressed(selected) } },
                    )
                    .padding(vertical = 2.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White.copy(alpha = 0.055f)),
                ) {
                    if (app != null) {
                        LauncherAppIcon(app = app, size = 35.dp)
                    } else {
                        ActivityGlyph(ActivityGlyphKind.TOOLS, size = 24.dp)
                    }
                }
                BasicText(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = palette.contentSecondary,
                        fontFamily = FontFamily.SansSerif,
                        fontSize = 9.sp,
                    ),
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}
