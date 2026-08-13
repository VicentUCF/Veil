package dev.vicent.veil.launcher.repository

import android.content.Context
import dev.vicent.veil.launcher.model.AccentMode
import dev.vicent.veil.launcher.model.LauncherPreferences
import dev.vicent.veil.launcher.model.LauncherPreferencesPolicy
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.ContextAppPreferencesPolicy
import dev.vicent.veil.launcher.model.HomeTextTone
import dev.vicent.veil.launcher.model.HomeTextWeight
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
        val next = mutableState.value.copy(accentMode = mode)
        preferences.edit()
            .putString(KEY_ACCENT_MODE, LauncherPreferencesPolicy.encodeAccent(next))
            .apply()
        mutableState.value = next
    }

    fun resetAppearance() {
        val next = LauncherPreferencesPolicy.resetAppearance(mutableState.value)
        preferences.edit()
            .putString(KEY_ACCENT_MODE, next.accentMode.persistedValue)
            .putString(KEY_HOME_TEXT_TONE, next.homeTextTone.persistedValue)
            .putString(KEY_HOME_TEXT_WEIGHT, next.homeTextWeight.persistedValue)
            .putBoolean(KEY_WALLPAPER_SCRIM_ENABLED, next.wallpaperScrimEnabled)
            .apply()
        mutableState.value = next
    }

    fun setHomeTextTone(mode: HomeTextTone) {
        val next = mutableState.value.copy(homeTextTone = mode)
        preferences.edit().putString(KEY_HOME_TEXT_TONE, mode.persistedValue).apply()
        mutableState.value = next
    }

    fun setHomeTextWeight(mode: HomeTextWeight) {
        val next = mutableState.value.copy(homeTextWeight = mode)
        preferences.edit().putString(KEY_HOME_TEXT_WEIGHT, mode.persistedValue).apply()
        mutableState.value = next
    }

    fun setWallpaperScrimEnabled(enabled: Boolean) {
        val next = mutableState.value.copy(wallpaperScrimEnabled = enabled)
        preferences.edit().putBoolean(KEY_WALLPAPER_SCRIM_ENABLED, enabled).apply()
        mutableState.value = next
    }

    fun setMusicProvider(packageName: String?) {
        val next = mutableState.value.copy(musicProviderPackage = packageName)
        preferences.edit().apply {
            if (packageName == null) remove(KEY_MUSIC_PROVIDER)
            else putString(KEY_MUSIC_PROVIDER, packageName)
        }.apply()
        mutableState.value = next
    }

    fun setContextSlot(
        kind: LauncherContextKind,
        slotIndex: Int,
        packageName: String?,
        currentSlots: List<String?>,
    ) {
        if (slotIndex !in 0 until CONTEXT_SLOT_COUNT) return
        val slots = ContextAppPreferencesPolicy.update(
            currentSlots = currentSlots,
            slotIndex = slotIndex,
            packageName = packageName,
            count = CONTEXT_SLOT_COUNT,
        )
        writeContextOverride(kind, slots)
    }

    fun resetContext(kind: LauncherContextKind) {
        preferences.edit().apply {
            remove(contextConfiguredKey(kind))
            repeat(CONTEXT_SLOT_COUNT) { remove(contextSlotKey(kind, it)) }
        }.apply()
        mutableState.value = mutableState.value.copy(
            contextAppOverrides = mutableState.value.contextAppOverrides - kind,
        )
    }

    private fun writeContextOverride(kind: LauncherContextKind, slots: List<String?>) {
        val normalized = ContextAppPreferencesPolicy.normalize(slots, CONTEXT_SLOT_COUNT)
        preferences.edit().apply {
            putBoolean(contextConfiguredKey(kind), true)
            normalized.forEachIndexed { index, packageName ->
                if (packageName == null) remove(contextSlotKey(kind, index))
                else putString(contextSlotKey(kind, index), packageName)
            }
        }.apply()
        mutableState.value = mutableState.value.copy(
            contextAppOverrides = mutableState.value.contextAppOverrides + (kind to normalized),
        )
    }

    private fun readPreferences(): LauncherPreferences {
        val base = LauncherPreferencesPolicy.decodeAppearance(
            accent = preferences.getString(KEY_ACCENT_MODE, null),
            homeTextTone = preferences.getString(KEY_HOME_TEXT_TONE, null),
            homeTextWeight = preferences.getString(KEY_HOME_TEXT_WEIGHT, null),
            wallpaperScrimEnabled = preferences.getBoolean(
                KEY_WALLPAPER_SCRIM_ENABLED,
                true,
            ),
        )
        val overrides = LauncherContextKind.entries.mapNotNull { kind ->
            if (!preferences.getBoolean(contextConfiguredKey(kind), false)) return@mapNotNull null
            kind to List(CONTEXT_SLOT_COUNT) { index ->
                preferences.getString(contextSlotKey(kind, index), null)
            }
        }.toMap()
        return base.copy(
            musicProviderPackage = preferences.getString(KEY_MUSIC_PROVIDER, null),
            contextAppOverrides = overrides,
        )
    }

    private fun contextConfiguredKey(kind: LauncherContextKind) =
        "context_${kind.name.lowercase()}_configured"

    private fun contextSlotKey(kind: LauncherContextKind, index: Int) =
        "context_${kind.name.lowercase()}_slot_$index"

    companion object {
        const val PREFERENCES_NAME = "veil_launcher_preferences"
        const val CONTEXT_SLOT_COUNT = 5
        private const val KEY_ACCENT_MODE = "accent_mode"
        private const val KEY_HOME_TEXT_TONE = "home_text_tone"
        private const val KEY_HOME_TEXT_WEIGHT = "home_text_weight"
        private const val KEY_WALLPAPER_SCRIM_ENABLED = "wallpaper_scrim_enabled"
        private const val KEY_MUSIC_PROVIDER = "music_provider_package"
    }
}
