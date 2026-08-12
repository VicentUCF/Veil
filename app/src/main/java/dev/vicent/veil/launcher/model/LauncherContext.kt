package dev.vicent.veil.launcher.model

data class LauncherContext(
    val id: String,
    val label: String,
    val apps: List<String> = emptyList(),
)
