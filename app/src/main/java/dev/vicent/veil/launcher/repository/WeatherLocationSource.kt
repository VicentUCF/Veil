package dev.vicent.veil.launcher.repository

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import dev.vicent.veil.launcher.SystemTimeProvider
import dev.vicent.veil.launcher.TimeProvider
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

internal class WeatherLocationSource(
    private val context: Context,
    private val timeProvider: TimeProvider = SystemTimeProvider,
) {
    private val locationManager = context.getSystemService(LocationManager::class.java)

    @SuppressLint("MissingPermission")
    suspend fun currentLocation(): Location? {
        val providers = locationManager.getProviders(true)
        val cached = providers.mapNotNull { provider ->
            runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull()
        }.maxByOrNull(Location::getTime)
        if (cached != null && timeProvider.currentTimeMillis() - cached.time < MAX_AGE_MILLIS) {
            return cached
        }
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
                ContextCompat.getMainExecutor(context),
            ) {
                if (continuation.isActive) continuation.resume(it ?: cached)
            }
        }
    }

    private companion object {
        const val MAX_AGE_MILLIS = 30 * 60 * 1000L
    }
}
