package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.SettingsAppTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

internal class LauncherNavigationCoordinator(
    private val state: MutableStateFlow<LauncherUiState>,
    private val quickActionCount: Int,
) {
    fun openDrawer() = state.update { current ->
        current.copy(navigation = current.navigation.openEverything())
    }

    fun closeDrawer() = state.update { current ->
        current.copy(navigation = current.navigation.closeToHome())
    }

    fun handleHomePressed() = state.update { current ->
        current.copy(navigation = current.navigation.handleHomePressed())
    }

    fun openSettings() = state.update { current ->
        current.copy(
            navigation = if (current.isSettingsOpen) {
                current.navigation
            } else {
                current.navigation.openSettings()
            },
            settingsAppTarget = null,
            settingsPickerReturnsToSettings = false,
        )
    }

    fun closeSettings() = state.update { current ->
        when {
            current.settingsAppTarget == null ->
                current.copy(navigation = current.navigation.closeSettings())
            current.settingsPickerReturnsToSettings -> current.copy(
                settingsAppTarget = null,
                settingsPickerReturnsToSettings = false,
            )
            else -> current.copy(
                navigation = current.navigation.closeSettings(),
                settingsAppTarget = null,
                settingsPickerReturnsToSettings = false,
            )
        }
    }

    fun openMusicProviderPicker() = state.update { current ->
        current.copy(
            navigation = if (current.isSettingsOpen) {
                current.navigation
            } else {
                current.navigation.openSettings()
            },
            settingsAppTarget = SettingsAppTarget.MusicProvider,
            settingsPickerReturnsToSettings = current.isSettingsOpen,
        )
    }

    fun openContextSlotPicker(kind: LauncherContextKind, slotIndex: Int) {
        if (slotIndex !in 0 until quickActionCount) return
        state.update { current ->
            current.copy(
                navigation = current.navigation.openSettings(),
                settingsAppTarget = SettingsAppTarget.ContextSlot(kind, slotIndex),
                settingsPickerReturnsToSettings = false,
            )
        }
    }

    fun completeAppSelection() = state.update { current ->
        if (current.settingsPickerReturnsToSettings) {
            current.copy(
                settingsAppTarget = null,
                settingsPickerReturnsToSettings = false,
            )
        } else {
            current.copy(
                navigation = current.navigation.closeSettings(),
                settingsAppTarget = null,
                settingsPickerReturnsToSettings = false,
            )
        }
    }
}
