package dev.vicent.veil.launcher.repository

import android.content.Context
import dev.vicent.veil.launcher.model.AccentMode
import dev.vicent.veil.launcher.model.LauncherPreferences
import dev.vicent.veil.launcher.model.LauncherPreferencesPolicy
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.ContextAppPreferencesPolicy
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

    fun resetAppearance() = setAccentMode(
        LauncherPreferencesPolicy.resetAppearance(mutableState.value).accentMode,
    )

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
        val base = LauncherPreferencesPolicy.decodeAccent(
            preferences.getString(KEY_ACCENT_MODE, null),
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
        private const val KEY_MUSIC_PROVIDER = "music_provider_package"
    }
}
