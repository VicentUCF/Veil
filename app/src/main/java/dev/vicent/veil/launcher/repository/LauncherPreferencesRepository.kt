package dev.vicent.veil.launcher.repository

import android.content.Context
import dev.vicent.veil.launcher.model.AccentMode
import dev.vicent.veil.launcher.model.LauncherPreferences
import dev.vicent.veil.launcher.model.LauncherPreferencesPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class LauncherPreferencesRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val mutableState = MutableStateFlow(readPreferences())

    val state: StateFlow<LauncherPreferences> = mutableState.asStateFlow()

    fun setAccentMode(mode: AccentMode) {
        val next = LauncherPreferences(accentMode = mode)
        preferences.edit()
            .putString(KEY_ACCENT_MODE, LauncherPreferencesPolicy.encodeAccent(next))
            .apply()
        mutableState.value = next
    }

    fun resetAppearance() = setAccentMode(
        LauncherPreferencesPolicy.resetAppearance().accentMode,
    )

    private fun readPreferences() = LauncherPreferencesPolicy.decodeAccent(
        preferences.getString(KEY_ACCENT_MODE, null),
    )

    companion object {
        const val PREFERENCES_NAME = "veil_launcher_preferences"
        private const val KEY_ACCENT_MODE = "accent_mode"
    }
}
