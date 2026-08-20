package dev.vicent.veil.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import dev.vicent.veil.launcher.model.AudioChannel
import dev.vicent.veil.launcher.model.ContinuityAction

@Composable
internal fun MediaWorkspace(
    state: MediaWorkspaceUiState,
    compact: Boolean,
    onContinuityAccessRequested: () -> Unit,
    onContinuityAction: (String, ContinuityAction, Long?) -> Unit,
    onAudioVisualizerPermissionRequested: () -> Unit,
    onAudioVolumeChanged: (AudioChannel, Float) -> Unit,
    onSettingsSelected: (String) -> Unit,
    onAppSelected: (dev.vicent.veil.launcher.model.LauncherApp) -> Unit,
    onMusicProviderSelectionRequested: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(WorkspaceLayoutTokens.SECTION_SPACING)) {
        val media = state.mediaContinuity
        if (media != null) {
            MediaPlayerTile(media, onContinuityAction)
        } else {
            val musicProvider = state.musicProvider
            EmptyMediaTile(
                continuityEnabled = state.continuityAccessGranted,
                onContinuityAccessRequested = onContinuityAccessRequested,
                musicProviderLabel = musicProvider?.label,
                onOpenMusicProvider = musicProvider?.let { app -> { onAppSelected(app) } },
                onChooseMusicProvider = onMusicProviderSelectionRequested,
            )
        }
        AudioMixerTile(
            state = state.audioMixer,
            compact = compact,
            isPlaying = media?.isPlaying == true,
            onVisualizerPermissionRequested = onAudioVisualizerPermissionRequested,
            onVolumeChanged = onAudioVolumeChanged,
            onOpenSoundSettings = { onSettingsSelected("sound") },
        )
    }
}
