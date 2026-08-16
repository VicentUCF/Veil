package dev.vicent.veil.launcher.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import dev.vicent.veil.launcher.model.WeatherAvailability
import dev.vicent.veil.launcher.model.WeatherState
import java.net.URL
import java.io.Reader
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import org.json.JSONObject

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
            val cancellationSignal = CancellationSignal()
            continuation.invokeOnCancellation { cancellationSignal.cancel() }
            LocationManagerCompat.getCurrentLocation(
                locationManager,
                provider,
                cancellationSignal,
                ContextCompat.getMainExecutor(appContext),
            ) {
                if (continuation.isActive) continuation.resume(it ?: cached)
            }
        }
    }

    private suspend fun fetch(latitude: Double, longitude: Double): WeatherState = withContext(Dispatchers.IO) {
        require(latitude.isFinite() && latitude in -90.0..90.0)
        require(longitude.isFinite() && longitude in -180.0..180.0)
        val endpoint = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=$latitude&longitude=$longitude" +
                "&current=temperature_2m,apparent_temperature,weather_code" +
                "&daily=temperature_2m_max,temperature_2m_min&timezone=auto&forecast_days=1",
        )
        val connection = endpoint.openConnection() as HttpsURLConnection
        connection.connectTimeout = 5_000
        connection.readTimeout = 5_000
        connection.instanceFollowRedirects = false
        connection.setRequestProperty("User-Agent", "Veil/0.1")
        connection.setRequestProperty("Accept", "application/json")
        try {
            check(connection.responseCode in 200..299)
            check(connection.contentType?.substringBefore(';')?.trim() == "application/json")
            check(connection.contentLength in -1..MAX_RESPONSE_CHARS)
            parseWeather(
                connection.inputStream.bufferedReader(Charsets.UTF_8).use {
                    it.readBounded(MAX_RESPONSE_CHARS)
                },
                System.currentTimeMillis(),
            )
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
            val root = JSONObject(json)
            val current = root.getJSONObject("current")
            val daily = root.getJSONObject("daily")
            val temperature = current.getDouble("temperature_2m")
            val apparent = current.getDouble("apparent_temperature")
            val minimum = daily.getJSONArray("temperature_2m_min").getDouble(0)
            val maximum = daily.getJSONArray("temperature_2m_max").getDouble(0)
            val weatherCode = current.getInt("weather_code")
            require(listOf(temperature, apparent, minimum, maximum).all {
                it.isFinite() && it in MIN_TEMPERATURE_CELSIUS..MAX_TEMPERATURE_CELSIUS
            })
            require(minimum <= maximum)
            require(weatherCode in MIN_WEATHER_CODE..MAX_WEATHER_CODE)
            require(observedAt > 0L)
            return WeatherState(
                availability = WeatherAvailability.AVAILABLE,
                temperatureCelsius = temperature,
                apparentTemperatureCelsius = apparent,
                minimumCelsius = minimum,
                maximumCelsius = maximum,
                weatherCode = weatherCode,
                observedAtMillis = observedAt,
            )
        }

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
        private const val MAX_RESPONSE_CHARS = 64 * 1_024
        private const val MIN_TEMPERATURE_CELSIUS = -100.0
        private const val MAX_TEMPERATURE_CELSIUS = 100.0
        private const val MIN_WEATHER_CODE = 0
        private const val MAX_WEATHER_CODE = 99
    }
}

internal fun Reader.readBounded(maxChars: Int): String {
    require(maxChars > 0)
    val output = StringBuilder(minOf(maxChars, 8_192))
    val buffer = CharArray(8_192)
    while (true) {
        val remaining = maxChars - output.length
        check(remaining > 0 || read() == -1) { "Response exceeds limit" }
        if (remaining == 0) return output.toString()
        val count = read(buffer, 0, minOf(buffer.size, remaining))
        if (count < 0) return output.toString()
        output.append(buffer, 0, count)
    }
}
