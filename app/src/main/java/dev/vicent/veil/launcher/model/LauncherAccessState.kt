package dev.vicent.veil.launcher.model

data class LauncherAccessState(
    val isDefaultHome: Boolean = false,
    val continuityGranted: Boolean = false,
    val calendarGranted: Boolean = false,
    val approximateLocationGranted: Boolean = false,
    val audioVisualizerGranted: Boolean = false,
    val focusNotificationsGranted: Boolean = false,
    val exactAlarmsGranted: Boolean = false,
)
