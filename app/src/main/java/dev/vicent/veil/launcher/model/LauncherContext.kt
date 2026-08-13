package dev.vicent.veil.launcher.model

data class LauncherContext(
    val id: String,
    val label: String,
    val kind: LauncherContextKind,
    val apps: List<String> = emptyList(),
)

enum class LauncherContextKind {
    CURRENT,
    WORK,
    MEDIA,
    SOCIAL,
    TOOLS,
}
