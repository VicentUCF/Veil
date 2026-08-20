package dev.vicent.veil.launcher.model

import kotlin.math.pow

enum class AccentMode(val persistedValue: String) {
    VEIL("veil"),
    AMBER("amber"),
    SAGE("sage"),
    SKY("sky"),
    LILAC("lilac"),
    SYSTEM("system");

    companion object {
        fun fromPersistedValue(value: String?): AccentMode =
            entries.firstOrNull { it.persistedValue == value } ?: VEIL
    }
}

enum class HomeTextTone(val persistedValue: String) {
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromPersistedValue(value: String?): HomeTextTone =
            entries.firstOrNull { it.persistedValue == value } ?: LIGHT
    }
}

enum class HomeTextWeight(val persistedValue: String) {
    LIGHT("light"),
    REGULAR("regular"),
    SEMIBOLD("semibold");

    companion object {
        fun fromPersistedValue(value: String?): HomeTextWeight =
            entries.firstOrNull { it.persistedValue == value } ?: LIGHT
    }
}

object WallpaperScrimPolicy {
    private const val CURVE_EXPONENT = 2.585f

    fun alpha(tone: HomeTextTone, intensity: Float): Float {
        val maximumAlpha = when (tone) {
            HomeTextTone.LIGHT -> 0.72f
            HomeTextTone.DARK -> 0.60f
        }
        return maximumAlpha * intensity.coerceIn(0f, 1f).pow(CURVE_EXPONENT)
    }
}

data class LauncherPreferences(
    val accentMode: AccentMode = AccentMode.VEIL,
    val homeTextTone: HomeTextTone = HomeTextTone.LIGHT,
    val homeTextWeight: HomeTextWeight = HomeTextWeight.LIGHT,
    val wallpaperScrimEnabled: Boolean = true,
    val wallpaperScrimIntensity: Float = 0.5f,
    val musicProviderPackage: String? = null,
    val contextAppOverrides: Map<LauncherContextKind, List<String?>> = emptyMap(),
    val selectedWorkspaceKinds: List<LauncherContextKind> =
        WorkspaceSelectionPolicy.RECOMMENDED_SELECTION,
    val workspaceSetupCompleted: Boolean = false,
    val homeButtonConfig: HomeButtonConfig = HomeButtonConfig(
        onTap = HomeButtonActionSpec.Everything,
        onLongPress = HomeButtonActionSpec.Everything,
    ),
)

object WorkspaceSelectionPolicy {
    const val SELECTED_COUNT = 4

    val RECOMMENDED_SELECTION = listOf(
        LauncherContextKind.WORK,
        LauncherContextKind.FOCUS,
        LauncherContextKind.MEDIA,
        LauncherContextKind.TOOLS,
    )

    val LEGACY_SELECTION = listOf(
        LauncherContextKind.WORK,
        LauncherContextKind.MEDIA,
        LauncherContextKind.GAME,
        LauncherContextKind.TOOLS,
    )

    fun decode(
        persistedValues: List<String>?,
        availableKinds: Set<LauncherContextKind> = LauncherContextKind.entries
            .filterNot { it == LauncherContextKind.CURRENT }
            .toSet(),
        fallback: List<LauncherContextKind> = RECOMMENDED_SELECTION,
    ): List<LauncherContextKind> {
        val decoded = persistedValues.orEmpty().mapNotNull { value ->
            LauncherContextKind.entries.firstOrNull {
                it.name.equals(value, ignoreCase = true)
            }
        }
        return normalize(decoded, availableKinds, fallback)
    }

    fun normalize(
        selected: List<LauncherContextKind>,
        availableKinds: Set<LauncherContextKind> = LauncherContextKind.entries
            .filterNot { it == LauncherContextKind.CURRENT }
            .toSet(),
        fallback: List<LauncherContextKind> = RECOMMENDED_SELECTION,
    ): List<LauncherContextKind> = buildList {
        (selected + fallback + availableKinds.sortedBy(LauncherContextKind::ordinal))
            .forEach { kind ->
                if (size < SELECTED_COUNT &&
                    kind != LauncherContextKind.CURRENT &&
                    kind in availableKinds &&
                    kind !in this
                ) {
                    add(kind)
                }
            }
    }

    fun replace(
        current: List<LauncherContextKind>,
        position: Int,
        replacement: LauncherContextKind,
        availableKinds: Set<LauncherContextKind>,
    ): List<LauncherContextKind> {
        val normalized = normalize(current, availableKinds)
        if (position !in normalized.indices ||
            replacement == LauncherContextKind.CURRENT ||
            replacement !in availableKinds ||
            replacement in normalized
        ) {
            return normalized
        }
        return normalized.toMutableList().apply { this[position] = replacement }
    }

    fun move(
        current: List<LauncherContextKind>,
        from: Int,
        to: Int,
        availableKinds: Set<LauncherContextKind>,
    ): List<LauncherContextKind> {
        val normalized = normalize(current, availableKinds)
        if (from !in normalized.indices || to !in normalized.indices || from == to) {
            return normalized
        }
        return normalized.toMutableList().apply {
            add(to, removeAt(from))
        }
    }
}

object HomeButtonActionPreferencesPolicy {
    private const val EVERYTHING = "everything"
    private const val VEIL_SETTINGS = "veil_settings"
    private const val APP_PREFIX = "app:"
    private const val SETTING_PREFIX = "setting:"

    fun encode(action: HomeButtonActionSpec): String = when (action) {
        HomeButtonActionSpec.Everything -> EVERYTHING
        HomeButtonActionSpec.VeilSettings -> VEIL_SETTINGS
        is HomeButtonActionSpec.App -> APP_PREFIX + action.packageCandidates.firstOrNull().orEmpty()
        is HomeButtonActionSpec.Setting -> SETTING_PREFIX + action.id
    }

    fun decode(value: String?, fallback: HomeButtonActionSpec): HomeButtonActionSpec = when {
        value == EVERYTHING -> HomeButtonActionSpec.Everything
        value == VEIL_SETTINGS -> HomeButtonActionSpec.VeilSettings
        value?.startsWith(APP_PREFIX) == true -> value.removePrefix(APP_PREFIX)
            .takeIf(String::isNotBlank)
            ?.let { HomeButtonActionSpec.App(listOf(it)) }
            ?: fallback
        value?.startsWith(SETTING_PREFIX) == true -> value.removePrefix(SETTING_PREFIX)
            .takeIf(String::isNotBlank)
            ?.let(HomeButtonActionSpec::Setting)
            ?: fallback
        else -> fallback
    }
}

object LauncherPreferencesPolicy {
    fun decodeAppearance(
        accent: String?,
        homeTextTone: String? = null,
        homeTextWeight: String? = null,
        wallpaperScrimEnabled: Boolean = true,
        wallpaperScrimIntensity: Float = 0.5f,
    ): LauncherPreferences = LauncherPreferences(
        accentMode = AccentMode.fromPersistedValue(accent),
        homeTextTone = HomeTextTone.fromPersistedValue(homeTextTone),
        homeTextWeight = HomeTextWeight.fromPersistedValue(homeTextWeight),
        wallpaperScrimEnabled = wallpaperScrimEnabled,
        wallpaperScrimIntensity = wallpaperScrimIntensity.coerceIn(0f, 1f),
    )

    fun encodeAccent(preferences: LauncherPreferences): String =
        preferences.accentMode.persistedValue

    fun resetAppearance(current: LauncherPreferences): LauncherPreferences =
        current.copy(
            accentMode = AccentMode.VEIL,
            homeTextTone = HomeTextTone.LIGHT,
            homeTextWeight = HomeTextWeight.LIGHT,
            wallpaperScrimEnabled = true,
            wallpaperScrimIntensity = 0.5f,
        )
}

object ContextAppPreferencesPolicy {
    fun normalize(slots: List<String?>, count: Int = 5): List<String?> =
        (slots.take(count) + List(count) { null }).take(count)

    fun update(
        currentSlots: List<String?>,
        slotIndex: Int,
        packageName: String?,
        count: Int = 5,
    ): List<String?> {
        if (slotIndex !in 0 until count) return normalize(currentSlots, count)
        return normalize(currentSlots, count).toMutableList().apply {
            this[slotIndex] = packageName
        }
    }
}
