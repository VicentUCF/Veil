package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.WeatherAvailability
import dev.vicent.veil.launcher.repository.OpenMeteoClient
import dev.vicent.veil.launcher.repository.readBounded
import java.io.StringReader
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.junit.Test

class WeatherRepositoryTest {
    @Test
    fun `open meteo response maps only the fields shown by Veil`() {
        val state = OpenMeteoClient.parse(
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

    @Test
    fun `weather parser rejects implausible remote values`() {
        assertFailsWith<IllegalArgumentException> {
            OpenMeteoClient.parse(
                json = """{
                    "current":{"temperature_2m":999,"apparent_temperature":17.1,"weather_code":2},
                    "daily":{"temperature_2m_max":[22.8],"temperature_2m_min":[12.3]}
                }""",
                observedAt = 42L,
            )
        }
    }

    @Test
    fun `bounded reader rejects oversized responses`() {
        assertEquals("1234", StringReader("1234").readBounded(4))
        assertFailsWith<IllegalStateException> {
            StringReader("12345").readBounded(4)
        }
    }
}
