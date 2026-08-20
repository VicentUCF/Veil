package dev.vicent.veil.launcher.repository

import android.content.Context
import androidx.core.content.edit
import dev.vicent.veil.launcher.SearchLearningPolicy
import dev.vicent.veil.launcher.SystemTimeProvider
import dev.vicent.veil.launcher.TimeProvider
import dev.vicent.veil.launcher.model.AppSearchLearningEntry
import dev.vicent.veil.launcher.model.AppSearchLearningState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SearchLearningRepository(
    context: Context,
    private val timeProvider: TimeProvider = SystemTimeProvider,
) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
        Context.MODE_PRIVATE,
    )
    private val mutableState = MutableStateFlow(
        AppSearchLearningState(SearchLearningPolicy.prune(readEntries(), now())),
    )
    val state: StateFlow<AppSearchLearningState> = mutableState.asStateFlow()

    init {
        writeEntries(mutableState.value.entries)
    }

    @Synchronized
    fun recordSuccessfulSelection(query: String, packageName: String) {
        update(SearchLearningPolicy.record(mutableState.value.entries, query, packageName, now()))
    }

    @Synchronized
    fun retainInstalledPackages(packageNames: Set<String>) {
        update(
            SearchLearningPolicy.prune(
                entries = mutableState.value.entries,
                nowMillis = now(),
                installedPackages = packageNames,
            ),
        )
    }

    @Synchronized
    fun removePackage(packageName: String) {
        update(mutableState.value.entries.filterNot { it.packageName == packageName })
    }

    private fun update(entries: List<AppSearchLearningEntry>) {
        if (entries == mutableState.value.entries) return
        writeEntries(entries)
        mutableState.value = AppSearchLearningState(entries)
    }

    private fun readEntries(): List<AppSearchLearningEntry> {
        val count = preferences.getInt(KEY_COUNT, 0).coerceIn(0, MAX_STORED_ENTRIES_TO_READ)
        return buildList {
            repeat(count) { index ->
                val query = preferences.getString("$KEY_QUERY_PREFIX$index", null)
                    ?: return@repeat
                val packageName = preferences.getString("$KEY_PACKAGE_PREFIX$index", null)
                    ?: return@repeat
                add(
                    AppSearchLearningEntry(
                        query = query,
                        packageName = packageName,
                        selectionCount = preferences.getInt("$KEY_COUNT_PREFIX$index", 0),
                        lastSelectedAtMillis = preferences.getLong("$KEY_LAST_USED_PREFIX$index", -1L),
                    ),
                )
            }
        }
    }

    private fun writeEntries(entries: List<AppSearchLearningEntry>) {
        preferences.edit {
            clear()
            putInt(KEY_COUNT, entries.size)
            entries.forEachIndexed { index, entry ->
                putString("$KEY_QUERY_PREFIX$index", entry.query)
                putString("$KEY_PACKAGE_PREFIX$index", entry.packageName)
                putInt("$KEY_COUNT_PREFIX$index", entry.selectionCount)
                putLong("$KEY_LAST_USED_PREFIX$index", entry.lastSelectedAtMillis)
            }
        }
    }

    private fun now(): Long = timeProvider.currentTimeMillis().coerceAtLeast(0L)

    companion object {
        const val PREFERENCES_NAME = "veil_search_learning"
        private const val KEY_COUNT = "entry_count"
        private const val KEY_QUERY_PREFIX = "query_"
        private const val KEY_PACKAGE_PREFIX = "package_"
        private const val KEY_COUNT_PREFIX = "selection_count_"
        private const val KEY_LAST_USED_PREFIX = "last_used_"
        private const val MAX_STORED_ENTRIES_TO_READ = SearchLearningPolicy.MAX_ASSOCIATIONS * 2
    }
}
