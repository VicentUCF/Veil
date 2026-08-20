package dev.vicent.veil.launcher.model

data class LauncherContext(
    val id: String,
    val kind: LauncherContextKind,
    val quickActions: List<QuickActionSpec> = emptyList(),
)

sealed interface QuickActionSpec {
    data class App(val packageCandidates: List<String>) : QuickActionSpec
    data class Setting(val id: String) : QuickActionSpec
}

/** The two deliberately small, source-configured bindings for CURRENT's action button. */
sealed interface HomeButtonActionSpec {
    data object Everything : HomeButtonActionSpec
    data object VeilSettings : HomeButtonActionSpec
    data class App(val packageCandidates: List<String>) : HomeButtonActionSpec
    data class Setting(val id: String) : HomeButtonActionSpec
}

enum class HomeButtonGesture {
    TAP,
    LONG_PRESS,
}

data class HomeButtonConfig(
    val onTap: HomeButtonActionSpec,
    val onLongPress: HomeButtonActionSpec,
)

enum class LauncherContextKind {
    CURRENT,
    WORK,
    MEDIA,
    GAME,
    TOOLS,
}
