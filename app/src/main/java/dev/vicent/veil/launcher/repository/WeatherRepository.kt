package dev.vicent.veil.launcher.repository

import android.content.Context
import dev.vicent.veil.launcher.model.WeatherAvailability
import dev.vicent.veil.launcher.model.WeatherState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class WeatherRepository(context: Context) {
    private val appContext = context.applicationContext
    private val locationSource = WeatherLocationSource(appContext)
    private val client = OpenMeteoClient()
    private val cache = WeatherCache(appContext)
    private val mutableState = MutableStateFlow(cache.load())
    val state: StateFlow<WeatherState> = mutableState.asStateFlow()
    private val refreshMutex = Mutex()

    suspend fun refresh(accessGranted: Boolean, force: Boolean = false) = refreshMutex.withLock {
        if (!accessGranted) {
            mutableState.value = mutableState.value.copy(
                availability = WeatherAvailability.NEEDS_PERMISSION,
            )
            return@withLock
        }
        val cached = mutableState.value
        val age = cached.observedAtMillis?.let { System.currentTimeMillis() - it }
            ?: Long.MAX_VALUE
        if (!force && cached.availability == WeatherAvailability.AVAILABLE &&
            age < REFRESH_MILLIS
        ) return@withLock
        mutableState.value = cached.copy(
            availability = WeatherAvailability.LOADING,
            isStale = age > STALE_MILLIS,
        )
        val location = withTimeoutOrNull(LOCATION_TIMEOUT_MILLIS) {
            locationSource.currentLocation()
        }
        if (location == null) {
            mutableState.value = cached.fallback(age)
            return@withLock
        }
        val result = runCatching {
            client.fetch(location.latitude, location.longitude)
        }.getOrNull()
        mutableState.value = if (result != null) {
            cache.save(result)
            result
        } else {
            cached.fallback(age)
        }
    }

    private fun WeatherState.fallback(age: Long): WeatherState = copy(
        availability = if (temperatureCelsius != null) {
            WeatherAvailability.AVAILABLE
        } else {
            WeatherAvailability.UNAVAILABLE
        },
        isStale = age > STALE_MILLIS,
    )

    private companion object {
        const val REFRESH_MILLIS = 30 * 60 * 1000L
        const val STALE_MILLIS = 2 * 60 * 60 * 1000L
        const val LOCATION_TIMEOUT_MILLIS = 6_000L
    }
}
