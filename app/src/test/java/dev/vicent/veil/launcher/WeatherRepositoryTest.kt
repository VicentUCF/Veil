package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.WeatherAvailability
import dev.vicent.veil.launcher.repository.WeatherRepository
import kotlin.test.assertEquals
import org.junit.Test

class WeatherRepositoryTest {
    @Test
    fun `open meteo response maps only the fields shown by Veil`() {
        val state = WeatherRepository.parseWeather(
            json = """{
                "current":{"temperature_2m":18.4,"apparent_temperature":17.1,"weather_code":2},
                "daily":{"temperature_2m_max":[22.8],"temperature_2m_min":[12.3]}
            }""",
            observedAt = 42L,
        )

        assertEquals(WeatherAvailability.AVAILABLE, state.availability)
        assertEquals(18.4, state.temperatureCelsius)
        assertEquals(17.1, state.apparentTemperatureCelsius)
        assertEquals(12.3, state.minimumCelsius)
        assertEquals(22.8, state.maximumCelsius)
        assertEquals(2, state.weatherCode)
        assertEquals(42L, state.observedAtMillis)
    }
}
