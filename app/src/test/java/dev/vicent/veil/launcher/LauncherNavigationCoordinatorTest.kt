package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.SettingsAppTarget
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import org.junit.Test

class LauncherNavigationCoordinatorTest {
    @Test
    fun `music picker opened from settings returns to settings after selection`() {
        val state = MutableStateFlow(LauncherUiState(contexts = emptyList()))
        val coordinator = LauncherNavigationCoordinator(state, quickActionCount = 5)

        coordinator.openSettings()
        coordinator.openMusicProviderPicker()
        coordinator.completeAppSelection()

        assertTrue(state.value.isSettingsOpen)
        assertNull(state.value.settingsAppTarget)
        assertFalse(state.value.settingsPickerReturnsToSettings)
    }

    @Test
    fun `context picker opened from home closes settings after selection`() {
        val state = MutableStateFlow(LauncherUiState(contexts = emptyList()))
        val coordinator = LauncherNavigationCoordinator(state, quickActionCount = 5)

        coordinator.openContextSlotPicker(LauncherContextKind.WORK, slotIndex = 2)
        assertIs<SettingsAppTarget.ContextSlot>(state.value.settingsAppTarget)

        coordinator.completeAppSelection()

        assertFalse(state.value.isSettingsOpen)
        assertNull(state.value.settingsAppTarget)
    }

    @Test
    fun `invalid context slot does not change navigation`() {
        val initial = LauncherUiState(contexts = emptyList())
        val state = MutableStateFlow(initial)
        val coordinator = LauncherNavigationCoordinator(state, quickActionCount = 5)

        coordinator.openContextSlotPicker(LauncherContextKind.WORK, slotIndex = 5)

        assertEquals(initial, state.value)
    }

    @Test
    fun `closing a picker opened from settings closes only the picker`() {
        val state = MutableStateFlow(LauncherUiState(contexts = emptyList()))
        val coordinator = LauncherNavigationCoordinator(state, quickActionCount = 5)

        coordinator.openSettings()
        coordinator.openMusicProviderPicker()
        coordinator.closeSettings()

        assertTrue(state.value.isSettingsOpen)
        assertNull(state.value.settingsAppTarget)
    }
}
