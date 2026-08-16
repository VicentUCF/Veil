package dev.vicent.veil.launcher.repository

import dev.vicent.veil.launcher.model.WeatherAvailability
import dev.vicent.veil.launcher.model.WeatherState
import java.io.Reader
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

internal class OpenMeteoClient {
    suspend fun fetch(latitude: Double, longitude: Double): WeatherState =
        withContext(Dispatchers.IO) {
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
                parse(
                    connection.inputStream.bufferedReader(Charsets.UTF_8).use {
                        it.readBounded(MAX_RESPONSE_CHARS)
                    },
                    System.currentTimeMillis(),
                )
            } finally {
                connection.disconnect()
            }
        }

    companion object {
        internal fun parse(json: String, observedAt: Long): WeatherState {
            val root = JSONObject(json)
            val current = root.getJSONObject("current")
            val daily = root.getJSONObject("daily")
            val temperature = current.getDouble("temperature_2m")
            val apparent = current.getDouble("apparent_temperature")
            val minimum = daily.getJSONArray("temperature_2m_min").getDouble(0)
            val maximum = daily.getJSONArray("temperature_2m_max").getDouble(0)
            val weatherCode = current.getInt("weather_code")
            require(listOf(temperature, apparent, minimum, maximum).all {
                it.isFinite() && it in -100.0..100.0
            })
            require(minimum <= maximum)
            require(weatherCode in 0..99)
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

        private const val MAX_RESPONSE_CHARS = 64 * 1_024
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
