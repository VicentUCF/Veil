package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.AppSearchLearningEntry

internal object SearchLearningPolicy {
    const val MAX_ASSOCIATIONS = 100
    const val RETENTION_MILLIS = 90L * 24L * 60L * 60L * 1_000L
    const val MAX_QUERY_LENGTH = 64
    private const val MIN_QUERY_LENGTH = 2
    private const val MAX_SELECTION_COUNT = 10_000

    fun record(
        entries: List<AppSearchLearningEntry>,
        rawQuery: String,
        packageName: String,
        nowMillis: Long,
    ): List<AppSearchLearningEntry> {
        val query = normalizeSearchText(rawQuery).take(MAX_QUERY_LENGTH)
        if (query.length < MIN_QUERY_LENGTH || packageName.isBlank() || nowMillis < 0L) {
            return prune(entries, nowMillis)
        }
        val current = prune(entries, nowMillis).toMutableList()
        val existingIndex = current.indexOfFirst {
            it.query == query && it.packageName == packageName
        }
        val updated = if (existingIndex >= 0) {
            current.removeAt(existingIndex)
        } else {
            AppSearchLearningEntry(
                query = query,
                packageName = packageName,
                selectionCount = 0,
                lastSelectedAtMillis = nowMillis,
            )
        }
        current += updated.copy(
            selectionCount = (updated.selectionCount + 1).coerceAtMost(MAX_SELECTION_COUNT),
            lastSelectedAtMillis = nowMillis,
        )
        return limit(current, nowMillis)
    }

    fun prune(
        entries: List<AppSearchLearningEntry>,
        nowMillis: Long,
        installedPackages: Set<String>? = null,
    ): List<AppSearchLearningEntry> {
        val oldestAllowed = nowMillis - RETENTION_MILLIS
        val sanitized = entries.mapNotNull { entry ->
            val query = normalizeSearchText(entry.query).take(MAX_QUERY_LENGTH)
            if (
                query.length < MIN_QUERY_LENGTH ||
                entry.packageName.isBlank() ||
                entry.selectionCount <= 0 ||
                entry.lastSelectedAtMillis !in oldestAllowed..nowMillis ||
                installedPackages?.contains(entry.packageName) == false
            ) {
                null
            } else {
                entry.copy(
                    query = query,
                    selectionCount = entry.selectionCount.coerceAtMost(MAX_SELECTION_COUNT),
                )
            }
        }.groupBy { it.query to it.packageName }
            .map { (key, duplicates) ->
                AppSearchLearningEntry(
                    query = key.first,
                    packageName = key.second,
                    selectionCount = duplicates.sumOf(AppSearchLearningEntry::selectionCount)
                        .coerceAtMost(MAX_SELECTION_COUNT),
                    lastSelectedAtMillis = duplicates.maxOf(
                        AppSearchLearningEntry::lastSelectedAtMillis,
                    ),
                )
            }
        return limit(sanitized, nowMillis)
    }

    private fun limit(
        entries: List<AppSearchLearningEntry>,
        nowMillis: Long,
    ): List<AppSearchLearningEntry> = entries.sortedWith(
        compareByDescending<AppSearchLearningEntry> { relevance(it, nowMillis) }
            .thenByDescending(AppSearchLearningEntry::lastSelectedAtMillis)
            .thenByDescending(AppSearchLearningEntry::selectionCount)
            .thenBy(AppSearchLearningEntry::query)
            .thenBy(AppSearchLearningEntry::packageName),
    ).take(MAX_ASSOCIATIONS)

    private fun relevance(entry: AppSearchLearningEntry, nowMillis: Long): Double {
        val ageDays = (nowMillis - entry.lastSelectedAtMillis).coerceAtLeast(0L).toDouble() /
            (24.0 * 60.0 * 60.0 * 1_000.0)
        return entry.selectionCount / (1.0 + ageDays / 14.0)
    }
}
