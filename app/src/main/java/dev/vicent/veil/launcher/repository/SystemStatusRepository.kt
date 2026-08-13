@file:Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")

package dev.vicent.veil.launcher.repository

import android.app.ActivityManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.wifi.WifiManager
import android.os.BatteryManager
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.telephony.PhoneStateListener
import android.telephony.SignalStrength
import android.telephony.TelephonyCallback
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import dev.vicent.veil.launcher.model.ConnectionType
import dev.vicent.veil.launcher.model.SystemStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SystemStatusRepository(context: Context) {
    private val context = context.applicationContext
    private val connectivityManager = context.getSystemService(ConnectivityManager::class.java)
    private val telephonyManager = context.getSystemService(TelephonyManager::class.java)
    private val wifiManager = context.getSystemService(WifiManager::class.java)
    private val mutableStatus = MutableStateFlow(SystemStatus())
    val status: StateFlow<SystemStatus> = mutableStatus.asStateFlow()

    private var started = false
    private var cellularSignalLevel: Int? = null
    private var networkCallback: ConnectivityManager.NetworkCallback? = null
    private var telephonyCallback: TelephonyCallback? = null

    @Suppress("DEPRECATION")
    private var phoneStateListener: PhoneStateListener? = null

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            refresh()
        }
    }

    fun start(scope: CoroutineScope) {
        if (started) return
        started = true

        ContextCompat.registerReceiver(
            context,
            batteryReceiver,
            IntentFilter(Intent.ACTION_BATTERY_CHANGED),
            ContextCompat.RECEIVER_NOT_EXPORTED,
        )
        registerNetworkListener()
        registerCellularSignalListener()
        refresh()

        scope.launch {
            try {
                awaitCancellation()
            } finally {
                stop()
            }
        }
    }

    fun refresh() {
        val battery = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = battery?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = battery?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val batteryPercent = if (level >= 0 && scale > 0) {
            (level * 100 / scale).coerceIn(0, 100)
        } else {
            null
        }
        val batteryStatus = battery?.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
        val storage = runCatching {
            StatFs(Environment.getDataDirectory().absolutePath)
        }.getOrNull()
        val memory = ActivityManager.MemoryInfo()
        runCatching {
            context.getSystemService(ActivityManager::class.java)?.getMemoryInfo(memory)
        }
        val capabilities = runCatching {
            connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        }.getOrNull()
        val connectionType = capabilities.connectionType()
        val connectionSignalLevel = when (connectionType) {
            ConnectionType.WIFI -> capabilities?.wifiSignalLevel()
            ConnectionType.CELLULAR -> cellularSignalLevel ?: currentCellularSignalLevel()
            ConnectionType.NONE, ConnectionType.ETHERNET, ConnectionType.OTHER -> null
        }
        mutableStatus.value = SystemStatus(
            batteryPercent = batteryPercent,
            isCharging = batteryStatus == BatteryManager.BATTERY_STATUS_CHARGING ||
                batteryStatus == BatteryManager.BATTERY_STATUS_FULL,
            storageAvailableBytes = storage?.availableBytes ?: 0L,
            storageTotalBytes = storage?.totalBytes ?: 0L,
            memoryAvailableBytes = memory.availMem,
            memoryTotalBytes = memory.totalMem,
            connectionType = connectionType,
            connectionSignalLevel = connectionSignalLevel,
            connectionLabel = connectionType.label,
            deviceManufacturer = Build.MANUFACTURER.trim().takeIf(String::isNotEmpty),
            deviceModel = Build.MODEL.trim().takeIf(String::isNotEmpty),
            androidVersion = Build.VERSION.RELEASE.trim().takeIf(String::isNotEmpty),
            securityPatch = Build.VERSION.SECURITY_PATCH.trim().takeIf(String::isNotEmpty),
        )
    }

    private fun registerNetworkListener() {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: android.net.Network) = refresh()
            override fun onLost(network: android.net.Network) = refresh()
            override fun onCapabilitiesChanged(
                network: android.net.Network,
                networkCapabilities: NetworkCapabilities,
            ) = refresh()
        }
        val registered = runCatching {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                connectivityManager.registerDefaultNetworkCallback(callback)
            } else {
                val request = NetworkRequest.Builder()
                    .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    .build()
                connectivityManager.registerNetworkCallback(request, callback)
            }
        }.isSuccess
        if (registered) networkCallback = callback
    }

    private fun registerCellularSignalListener() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val callback = object : TelephonyCallback(), TelephonyCallback.SignalStrengthsListener {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength) {
                    cellularSignalLevel = signalStrength.level.coerceIn(0, MAX_SIGNAL_LEVEL)
                    refresh()
                }
            }
            if (runCatching {
                    telephonyManager.registerTelephonyCallback(context.mainExecutor, callback)
                }.isSuccess
            ) {
                telephonyCallback = callback
            }
        } else {
            @Suppress("DEPRECATION")
            val listener = object : PhoneStateListener() {
                override fun onSignalStrengthsChanged(signalStrength: SignalStrength?) {
                    cellularSignalLevel = signalStrength?.level?.coerceIn(0, MAX_SIGNAL_LEVEL)
                    refresh()
                }
            }
            @Suppress("DEPRECATION")
            if (runCatching {
                    telephonyManager.listen(listener, PhoneStateListener.LISTEN_SIGNAL_STRENGTHS)
                }.isSuccess
            ) {
                phoneStateListener = listener
            }
        }
    }

    private fun stop() {
        runCatching { context.unregisterReceiver(batteryReceiver) }
        networkCallback?.let { callback ->
            runCatching { connectivityManager.unregisterNetworkCallback(callback) }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            telephonyCallback?.let { callback ->
                runCatching { telephonyManager.unregisterTelephonyCallback(callback) }
            }
        } else {
            @Suppress("DEPRECATION")
            phoneStateListener?.let { listener ->
                runCatching { telephonyManager.listen(listener, PhoneStateListener.LISTEN_NONE) }
            }
        }
        networkCallback = null
        telephonyCallback = null
        phoneStateListener = null
        started = false
    }

    private fun NetworkCapabilities?.connectionType(): ConnectionType = when {
        this == null -> ConnectionType.NONE
        hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
        hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
        hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
        else -> ConnectionType.OTHER
    }

    private fun NetworkCapabilities.wifiSignalLevel(): Int? {
        val rssi = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            signalStrength
        } else {
            @Suppress("DEPRECATION")
            runCatching { wifiManager.connectionInfo.rssi }.getOrDefault(INVALID_WIFI_RSSI)
        }
        return wifiSignalLevel(rssi)
    }

    private fun currentCellularSignalLevel(): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
        return runCatching { telephonyManager.signalStrength?.level?.coerceIn(0, MAX_SIGNAL_LEVEL) }
            .getOrNull()
    }

    private val ConnectionType.label: String
        get() = when (this) {
            ConnectionType.NONE -> "Sin conexión"
            ConnectionType.WIFI -> "Wi‑Fi"
            ConnectionType.CELLULAR -> "Datos móviles"
            ConnectionType.ETHERNET -> "Ethernet"
            ConnectionType.OTHER -> "Conectado"
        }

    companion object {
        private const val MAX_SIGNAL_LEVEL = 4
        private const val INVALID_WIFI_RSSI = -127

        internal fun wifiSignalLevel(rssi: Int): Int? = when {
            rssi == NetworkCapabilities.SIGNAL_STRENGTH_UNSPECIFIED || rssi == INVALID_WIFI_RSSI -> null
            rssi >= -55 -> 4
            rssi >= -67 -> 3
            rssi >= -75 -> 2
            rssi >= -85 -> 1
            else -> 0
        }
    }
}
