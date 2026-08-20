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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vicent.veil.R
import dev.vicent.veil.launcher.ResolvedQuickAction
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.SettingsShortcut
import dev.vicent.veil.ui.theme.LocalVeilPalette

/** A stable, context-owned application dock. CURRENT intentionally does not render it. */
@Composable
fun ContextDock(
    actions: List<ResolvedQuickAction>,
    notificationIndicatorPackages: Set<String>,
    settingsShortcuts: List<SettingsShortcut>,
    onAppSelected: (LauncherApp) -> Unit,
    onAppLongPressed: (LauncherApp) -> Unit,
    onSettingSelected: (SettingsShortcut) -> Unit,
    onEmptySlotSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    val emptyLabel = stringResource(R.string.launcher_empty_slot)
    val notificationState = stringResource(R.string.state_has_notifications)
    Row(
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Top,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(palette.tileBackground.copy(alpha = 0.84f))
            .border(1.dp, palette.divider, RoundedCornerShape(16.dp))
            .padding(horizontal = 7.dp, vertical = 8.dp),
    ) {
        actions.take(5).forEach { action ->
            val app = (action as? ResolvedQuickAction.App)?.app
            val setting = (action as? ResolvedQuickAction.Setting)?.let { resolved ->
                settingsShortcuts.firstOrNull { it.id == resolved.id }
            }
            val emptySlot = (action as? ResolvedQuickAction.Empty)?.slotIndex
            val label = app?.label ?: setting?.label ?: emptyLabel
            val openLabel = stringResource(R.string.action_open_named, label)
            val optionsLabel = app?.let { stringResource(R.string.action_options_named, label) }
            val hasNotification = app != null && app.packageName in notificationIndicatorPackages
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .weight(1f)
                    .combinedClickable(
                        role = Role.Button,
                        onClickLabel = openLabel,
                        onLongClickLabel = optionsLabel,
                        onClick = {
                            if (app != null) onAppSelected(app)
                            else if (setting != null) onSettingSelected(setting)
                            else if (emptySlot != null) onEmptySlotSelected(emptySlot)
                        },
                        onLongClick = app?.let { selected -> { onAppLongPressed(selected) } },
                    )
                    .then(
                        if (hasNotification) {
                            Modifier.semantics {
                                stateDescription = notificationState
                            }
                        } else {
                            Modifier
                        },
                    )
                    .padding(vertical = 2.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(palette.subtleFill),
                ) {
                    if (app != null) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(39.dp)) {
                            LauncherAppIcon(app = app, size = 35.dp)
                            AppNotificationIndicator(
                                visible = hasNotification,
                                modifier = Modifier.align(Alignment.TopEnd),
                            )
                        }
                    } else {
                        BasicText(
                            text = "+",
                            style = TextStyle(
                                color = palette.contentMuted,
                                fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.system,
                                fontSize = 22.sp,
                            ),
                        )
                    }
                }
                BasicText(
                    text = label,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = TextStyle(
                        color = palette.contentSecondary,
                        fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
                        fontSize = 9.sp,
                    ),
                    modifier = Modifier.padding(top = 3.dp),
                )
            }
        }
    }
}
