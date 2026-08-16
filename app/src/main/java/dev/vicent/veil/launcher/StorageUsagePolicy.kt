package dev.vicent.veil.launcher

object StorageUsagePolicy {
    fun usedFraction(availableBytes: Long, totalBytes: Long): Float? {
        if (totalBytes <= 0L) return null
        val boundedAvailable = availableBytes.coerceIn(0L, totalBytes)
        return (totalBytes - boundedAvailable).toFloat() / totalBytes
    }
}
