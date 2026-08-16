package dev.vicent.veil.launcher.model

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
