package dev.vicent.veil.launcher.model

data class SystemStatus(
    val batteryPercent: Int? = null,
    val isCharging: Boolean = false,
    val storageAvailableBytes: Long = 0,
    val storageTotalBytes: Long = 0,
    val memoryAvailableBytes: Long = 0,
    val memoryTotalBytes: Long = 0,
    val connectionType: ConnectionType = ConnectionType.NONE,
    val connectionSignalLevel: Int? = null,
    val deviceManufacturer: String? = null,
    val deviceModel: String? = null,
    val androidVersion: String? = null,
    val securityPatch: String? = null,
)

enum class ConnectionType { NONE, WIFI, CELLULAR, ETHERNET, OTHER }
