package dev.vicent.veil.launcher.repository

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import dev.vicent.veil.launcher.model.SystemStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SystemStatusRepository(private val context: Context) {
    private val mutableStatus = MutableStateFlow(SystemStatus())
    val status: StateFlow<SystemStatus> = mutableStatus.asStateFlow()

    fun refresh() {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, 0) ?: 0
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, 100)?.coerceAtLeast(1) ?: 100
        val status = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val storage = StatFs(Environment.getDataDirectory().absolutePath)
        val connectivity = context.getSystemService(ConnectivityManager::class.java)
        val capabilities = runCatching {
            connectivity.getNetworkCapabilities(connectivity.activeNetwork)
        }.getOrNull()
        val connection = when {
            capabilities == null -> "Sin conexión"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi‑Fi"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Datos móviles"
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "Ethernet"
            else -> "Conectado"
        }
        mutableStatus.value = SystemStatus(
            batteryPercent = level * 100 / scale,
            isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL,
            storageAvailableBytes = storage.availableBytes,
            storageTotalBytes = storage.totalBytes,
            connectionLabel = connection,
        )
    }
}
