package dev.vicent.veil.launcher.model

data class CalendarEventSummary(
    val id: Long,
    val title: String,
    val startMillis: Long,
    val endMillis: Long,
)

enum class WeatherAvailability { NEEDS_PERMISSION, LOADING, AVAILABLE, UNAVAILABLE }

data class WeatherState(
    val availability: WeatherAvailability = WeatherAvailability.NEEDS_PERMISSION,
    val temperatureCelsius: Double? = null,
    val apparentTemperatureCelsius: Double? = null,
    val minimumCelsius: Double? = null,
    val maximumCelsius: Double? = null,
    val weatherCode: Int? = null,
    val observedAtMillis: Long? = null,
    val isStale: Boolean = false,
)

enum class FocusTimerStatus { IDLE, RUNNING, PAUSED, COMPLETED }

data class FocusTimerState(
    val status: FocusTimerStatus = FocusTimerStatus.IDLE,
    val durationMillis: Long = 25 * 60 * 1000L,
    val remainingMillis: Long = 25 * 60 * 1000L,
    val exactAlarmAvailable: Boolean = true,
    val notificationsAvailable: Boolean = true,
)

data class SystemStatus(
    val batteryPercent: Int = 0,
    val isCharging: Boolean = false,
    val storageAvailableBytes: Long = 0,
    val storageTotalBytes: Long = 0,
    val connectionLabel: String = "Sin conexión",
)
