package dev.vicent.veil.launcher.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import dev.vicent.veil.launcher.model.WeatherAvailability
import dev.vicent.veil.launcher.model.WeatherState
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

class WeatherRepository(context: Context) {
    private val appContext = context.applicationContext
    private val locationManager = appContext.getSystemService(LocationManager::class.java)
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val mutableState = MutableStateFlow(loadCached())
    val state: StateFlow<WeatherState> = mutableState.asStateFlow()

    suspend fun refresh(accessGranted: Boolean, force: Boolean = false) {
        if (!accessGranted) {
            mutableState.value = mutableState.value.copy(availability = WeatherAvailability.NEEDS_PERMISSION)
            return
        }
        val cached = mutableState.value
        val age = cached.observedAtMillis?.let { System.currentTimeMillis() - it } ?: Long.MAX_VALUE
        if (!force && cached.availability == WeatherAvailability.AVAILABLE && age < REFRESH_MILLIS) return
        mutableState.value = cached.copy(availability = WeatherAvailability.LOADING, isStale = age > STALE_MILLIS)
        val location = withTimeoutOrNull(6_000) { currentLocation() }
        if (location == null) {
            mutableState.value = cached.copy(
                availability = if (cached.temperatureCelsius != null) WeatherAvailability.AVAILABLE else WeatherAvailability.UNAVAILABLE,
                isStale = age > STALE_MILLIS,
            )
            return
        }
        val result = runCatching { fetch(location.latitude, location.longitude) }.getOrNull()
        mutableState.value = if (result != null) {
            save(result)
            result
        } else {
            cached.copy(
                availability = if (cached.temperatureCelsius != null) WeatherAvailability.AVAILABLE else WeatherAvailability.UNAVAILABLE,
                isStale = age > STALE_MILLIS,
            )
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun currentLocation(): Location? {
        val providers = locationManager.getProviders(true)
        val cached = providers.mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull(Location::getTime)
        if (cached != null && System.currentTimeMillis() - cached.time < LOCATION_MAX_AGE_MILLIS) return cached
        val provider = when {
            LocationManager.NETWORK_PROVIDER in providers -> LocationManager.NETWORK_PROVIDER
            LocationManager.GPS_PROVIDER in providers -> LocationManager.GPS_PROVIDER
            else -> return cached
        }
        return suspendCancellableCoroutine { continuation ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                locationManager.getCurrentLocation(provider, null, appContext.mainExecutor) {
                    if (continuation.isActive) continuation.resume(it)
                }
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        locationManager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(location)
                    }
                    @Deprecated("Legacy callback") override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) = Unit
                    override fun onProviderEnabled(provider: String) = Unit
                    override fun onProviderDisabled(provider: String) {
                        locationManager.removeUpdates(this)
                        if (continuation.isActive) continuation.resume(cached)
                    }
                }
                locationManager.requestSingleUpdate(provider, listener, appContext.mainLooper)
                continuation.invokeOnCancellation { locationManager.removeUpdates(listener) }
            }
        }
    }

    private suspend fun fetch(latitude: Double, longitude: Double): WeatherState = withContext(Dispatchers.IO) {
        val endpoint = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,apparent_temperature,weather_code" +
                "&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1",
        )
        val connection = endpoint.openConnection() as HttpsURLConnection
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        connection.setRequestProperty("User-Agent", "Veil/0.1")
        try {
            check(connection.responseCode in 200..299)
            parseWeather(connection.inputStream.bufferedReader().use { it.readText() }, System.currentTimeMillis())
        } finally {
            connection.disconnect()
        }
    }

    private fun save(value: WeatherState) {
        preferences.edit()
            .putLong(KEY_OBSERVED, value.observedAtMillis ?: 0L)
            .putLong(KEY_TEMP, value.temperatureCelsius?.toBits() ?: 0L)
            .putLong(KEY_APPARENT, value.apparentTemperatureCelsius?.toBits() ?: 0L)
            .putLong(KEY_MIN, value.minimumCelsius?.toBits() ?: 0L)
            .putLong(KEY_MAX, value.maximumCelsius?.toBits() ?: 0L)
            .putInt(KEY_CODE, value.weatherCode ?: -1)
            .apply()
    }

    private fun loadCached(): WeatherState {
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

    companion object {
        internal fun parseWeather(json: String, observedAt: Long): WeatherState {
            return WeatherState(
                availability = WeatherAvailability.AVAILABLE,
                temperatureCelsius = json.number("temperature_2m"),
                apparentTemperatureCelsius = json.number("apparent_temperature"),
                minimumCelsius = json.firstArrayNumber("temperature_2m_min"),
                maximumCelsius = json.firstArrayNumber("temperature_2m_max"),
                weatherCode = json.number("weather_code").toInt(),
                observedAtMillis = observedAt,
            )
        }

        private fun String.number(name: String): Double = Regex(
            "\\\"${Regex.escape(name)}\\\"\\s*:\\s*(-?\\d+(?:\\.\\d+)?)",
        ).find(this)?.groupValues?.get(1)?.toDouble()
            ?: error("Missing weather field: $name")

        private fun String.firstArrayNumber(name: String): Double = Regex(
            "\\\"${Regex.escape(name)}\\\"\\s*:\\s*\\[\\s*(-?\\d+(?:\\.\\d+)?)",
        ).find(this)?.groupValues?.get(1)?.toDouble()
            ?: error("Missing weather field: $name")

        private const val PREFERENCES = "veil_weather"
        private const val KEY_OBSERVED = "observed"
        private const val KEY_TEMP = "temperature"
        private const val KEY_APPARENT = "apparent"
        private const val KEY_MIN = "minimum"
        private const val KEY_MAX = "maximum"
        private const val KEY_CODE = "code"
        private const val REFRESH_MILLIS = 30 * 60 * 1000L
        private const val STALE_MILLIS = 2 * 60 * 60 * 1000L
        private const val LOCATION_MAX_AGE_MILLIS = 30 * 60 * 1000L
    }
}
