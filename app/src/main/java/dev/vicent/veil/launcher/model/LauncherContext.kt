package dev.vicent.veil.launcher.model

data class LauncherContext(
    val id: String,
    val label: String,
    val kind: LauncherContextKind,
    val quickActions: List<QuickActionSpec> = emptyList(),
)

sealed interface QuickActionSpec {
    data class App(val packageName: String) : QuickActionSpec
    data class Setting(val id: String) : QuickActionSpec
}

enum class LauncherContextKind {
    CURRENT,
    WORK,
    MEDIA,
    SOCIAL,
    TOOLS,
}
