package dev.vicent.veil.launcher

internal data class AppNotificationIndicatorCandidate(
    val packageName: String,
    val ownPackageName: String,
    val category: String?,
    val progressMax: Int,
    val isClearable: Boolean,
    val isOngoing: Boolean,
    val isForegroundService: Boolean,
    val canShowBadge: Boolean?,
)

internal object AppNotificationIndicatorPolicy {
    private val excludedCategories = setOf(
        "navigation",
        "progress",
        "service",
        "transport",
    )

    fun shouldShow(candidate: AppNotificationIndicatorCandidate): Boolean = when {
        candidate.packageName == candidate.ownPackageName -> false
        candidate.isOngoing || candidate.isForegroundService -> false
        candidate.category in excludedCategories -> false
        candidate.progressMax > 0 -> false
        candidate.canShowBadge == false -> false
        candidate.canShowBadge == null && !candidate.isClearable -> false
        else -> true
    }
}

internal class AppNotificationIndicatorTracker {
    private val packagesByKey = mutableMapOf<String, String>()

    @Synchronized
    fun replace(signals: Iterable<Pair<String, String>>) {
        packagesByKey.clear()
        signals.forEach { (key, packageName) -> packagesByKey[key] = packageName }
    }

    @Synchronized
    fun update(key: String, packageName: String?) {
        if (packageName == null) packagesByKey.remove(key)
        else packagesByKey[key] = packageName
    }

    @Synchronized
    fun remove(key: String) {
        packagesByKey.remove(key)
    }

    @Synchronized
    fun clear() {
        packagesByKey.clear()
    }

    @Synchronized
    fun packages(): Set<String> = packagesByKey.values.toSet()
}
