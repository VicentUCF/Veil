package dev.vicent.veil.launcher.model

data class LauncherContext(
    val id: String,
    val kind: LauncherContextKind,
    val titleResource: Int,
    val descriptionResource: Int,
    val capabilities: Set<WorkspaceCapability> = emptySet(),
    val availability: WorkspaceAvailability = WorkspaceAvailability.AVAILABLE,
    val quickActions: List<QuickActionSpec> = emptyList(),
)

enum class WorkspaceCapability {
    CALENDAR,
    WEATHER,
    CONTINUITY,
    WORK_PROGRESS,
    AUDIO,
    STEAM,
    SYSTEM_STATUS,
}

enum class WorkspaceAvailability {
    AVAILABLE,
    RETIRING,
}

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
    FOCUS,
    MEDIA,
    GAME,
    TOOLS,
    ON_THE_GO,
}
