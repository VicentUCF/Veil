package dev.vicent.veil.launcher.repository

import android.content.Context
import androidx.core.content.edit
import dev.vicent.veil.launcher.model.WeatherAvailability
import dev.vicent.veil.launcher.model.WeatherState

internal class WeatherCache(context: Context) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    fun save(value: WeatherState) {
        preferences.edit {
            putLong(KEY_OBSERVED, value.observedAtMillis ?: 0L)
            putLong(KEY_TEMP, value.temperatureCelsius?.toBits() ?: 0L)
            putLong(KEY_APPARENT, value.apparentTemperatureCelsius?.toBits() ?: 0L)
            putLong(KEY_MIN, value.minimumCelsius?.toBits() ?: 0L)
            putLong(KEY_MAX, value.maximumCelsius?.toBits() ?: 0L)
            putInt(KEY_CODE, value.weatherCode ?: -1)
        }
    }

    fun load(): WeatherState {
        val observed = preferences.getLong(KEY_OBSERVED, 0L)
        if (observed == 0L) return WeatherState()
        return WeatherState(
            availability = WeatherAvailability.AVAILABLE,
            temperatureCelsius = Double.fromBits(preferences.getLong(KEY_TEMP, 0L)),
            apparentTemperatureCelsius = Double.fromBits(preferences.getLong(KEY_APPARENT, 0L)),
            minimumCelsius = Double.fromBits(preferences.getLong(KEY_MIN, 0L)),
            maximumCelsius = Double.fromBits(preferences.getLong(KEY_MAX, 0L)),
            weatherCode = preferences.getInt(KEY_CODE, -1).takeIf { it >= 0 },
            observedAtMillis = observed,
            isStale = System.currentTimeMillis() - observed > STALE_MILLIS,
        )
    }

    private companion object {
        const val PREFERENCES = "veil_weather"
        const val KEY_OBSERVED = "observed"
        const val KEY_TEMP = "temperature"
        const val KEY_APPARENT = "apparent"
        const val KEY_MIN = "minimum"
        const val KEY_MAX = "maximum"
        const val KEY_CODE = "code"
        const val STALE_MILLIS = 2 * 60 * 60 * 1000L
    }
}
