package dev.vicent.veil.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.vicent.veil.launcher.LauncherUiState
import dev.vicent.veil.launcher.ResolvedLauncherContext
import dev.vicent.veil.launcher.GameLibraryPolicy
import dev.vicent.veil.launcher.model.AudioChannel
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.QuickNoteChecklistItem
import dev.vicent.veil.launcher.model.QuickNoteType
import dev.vicent.veil.launcher.model.SettingsShortcut
import dev.vicent.veil.ui.theme.LocalVeilPalette

@Composable
fun WorkspaceDashboard(
    state: LauncherUiState,
    context: ResolvedLauncherContext,
    settingsShortcuts: List<SettingsShortcut>,
    onCalendarPermissionRequested: () -> Unit,
    onLocationPermissionRequested: () -> Unit,
    onClockOpenRequested: () -> Unit,
    onContinuityAccessRequested: () -> Unit,
    onCalendarEventSelected: (Long) -> Unit,
    onCalendarEventCreateRequested: () -> Unit,
    onCalendarOpenRequested: () -> Unit,
    onGoogleCalendarConfigureRequested: () -> Unit,
    onContinuityAction: (String, ContinuityAction, Long?) -> Unit,
    onHomeMediaDismissed: (String) -> Unit,
    onAudioVisualizerPermissionRequested: () -> Unit,
    onAudioVolumeChanged: (AudioChannel, Float) -> Unit,
    onSettingsSelected: (SettingsShortcut) -> Unit,
    onVeilSettingsSelected: () -> Unit,
    onMusicProviderSelectionRequested: () -> Unit,
    onFocusStart: (Int) -> Unit,
    onFocusPause: () -> Unit,
    onFocusResume: () -> Unit,
    onFocusFinish: () -> Unit,
    onQuickNoteAdded: (String, QuickNoteType, String, List<QuickNoteChecklistItem>) -> Unit,
    onQuickNoteUpdated: (Long, String, QuickNoteType, String, List<QuickNoteChecklistItem>) -> Unit,
    onQuickNoteDeleted: (Long) -> Unit,
    onExternalLinkSelected: (String) -> Unit,
    onAppSelected: (dev.vicent.veil.launcher.model.LauncherApp) -> Unit,
    onAppLongPressed: (dev.vicent.veil.launcher.model.LauncherApp) -> Unit,
    onEmptyContextSlotSelected: (LauncherContextKind, Int) -> Unit,
    onHomeButtonTap: () -> Unit,
    onHomeButtonLongPress: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(modifier = modifier) {
        // The outer 16 dp gutters are already consumed by LauncherScreen. A 328 dp
        // content width corresponds to the product's 360 dp screen breakpoint.
        val compact = maxWidth < WorkspaceLayoutTokens.COMPACT_BREAKPOINT
        when (context.definition.kind) {
            LauncherContextKind.CURRENT -> CurrentHome(
                state = state.currentHomeState(),
                actions = context.quickActions,
                onAppSelected = onAppSelected,
                onAppLongPressed = onAppLongPressed,
                onEmptySlotSelected = { slotIndex ->
                    onEmptyContextSlotSelected(context.definition.kind, slotIndex)
                },
                onLocationPermissionRequested = onLocationPermissionRequested,
                onClockOpenRequested = onClockOpenRequested,
                onCalendarOpenRequested = onCalendarOpenRequested,
                onContinuityAction = onContinuityAction,
                onMediaDismissed = onHomeMediaDismissed,
                onQuickButtonTap = onHomeButtonTap,
                onQuickButtonLongPress = onHomeButtonLongPress,
                modifier = Modifier.fillMaxSize(),
            )
            LauncherContextKind.WORK -> WorkWorkspace(
                state.workWorkspaceState(),
                compact,
                onCalendarPermissionRequested,
                onCalendarEventSelected,
                onCalendarEventCreateRequested,
                onCalendarOpenRequested,
                onGoogleCalendarConfigureRequested,
                onContinuityAction,
                onFocusStart,
                onFocusPause,
                onFocusResume,
                onFocusFinish,
                onQuickNoteAdded,
                onQuickNoteUpdated,
                onQuickNoteDeleted,
            )
            LauncherContextKind.MEDIA -> MediaWorkspace(
                state.mediaWorkspaceState(),
                compact,
                onContinuityAccessRequested,
                onContinuityAction,
                onAudioVisualizerPermissionRequested,
                onAudioVolumeChanged,
                onSettingsSelected = { id -> settingsShortcuts.find { it.id == id }?.let(onSettingsSelected) },
                onAppSelected = onAppSelected,
                onMusicProviderSelectionRequested = onMusicProviderSelectionRequested,
            )
            LauncherContextKind.GAME -> GameWorkspace(
                feed = state.gameFeed,
                library = GameLibraryPolicy.gameLibrary(state.installedApps, context.apps),
                compact = compact,
                onExternalLinkSelected = onExternalLinkSelected,
                onAppSelected = onAppSelected,
                onAppLongPressed = onAppLongPressed,
            )
            LauncherContextKind.TOOLS -> ToolsWorkspace(
                state.toolsWorkspaceState(),
                compact,
                onVeilSettingsSelected,
                onSettingsSelected = { id -> settingsShortcuts.find { it.id == id }?.let(onSettingsSelected) },
            )
        }
    }
}










@Composable
internal fun ResponsivePair(
    compact: Boolean,
    left: @Composable () -> Unit,
    right: @Composable () -> Unit,
) {
    if (compact) {
        Column(verticalArrangement = Arrangement.spacedBy(WorkspaceLayoutTokens.SECTION_SPACING)) {
            left()
            right()
        }
    } else {
        Row(horizontalArrangement = Arrangement.spacedBy(WorkspaceLayoutTokens.SECTION_SPACING)) {
            Column(modifier = Modifier.weight(1f)) { left() }
            Column(modifier = Modifier.weight(1f)) { right() }
        }
    }
}





@Composable internal fun TileTitle(text: String, prominent: Boolean = false) = BasicText(
    text = text,
    maxLines = 2,
    overflow = TextOverflow.Ellipsis,
    style = workspaceTitleStyle(LocalVeilPalette.current.contentPrimary, prominent),
)

@Composable internal fun TileBody(text: String) = BasicText(
    text = text,
    maxLines = 3,
    overflow = TextOverflow.Ellipsis,
    style = workspaceBodyStyle(LocalVeilPalette.current.contentSecondary),
    modifier = Modifier.padding(top = 5.dp),
)

@Composable internal fun TileAction(label: String, onClick: () -> Unit) = BasicText(
    text = label.uppercase(),
    style = workspaceMonoStyle(LocalVeilPalette.current.contentPrimary, 10),
    modifier = Modifier
        .clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
        .padding(vertical = 10.dp, horizontal = 2.dp),
)

@Composable
internal fun SimpleProgress(progress: Float) {
    val palette = LocalVeilPalette.current
    Canvas(modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(2.dp)) {
        drawRect(palette.divider)
        drawRect(palette.accentActive, size = size.copy(width = size.width * progress.coerceIn(0f, 1f)))
    }
}
