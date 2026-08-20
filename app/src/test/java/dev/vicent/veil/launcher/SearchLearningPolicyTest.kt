package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.AppSearchLearningEntry
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.Test

class SearchLearningPolicyTest {
    private val now = 100L * DAY_MILLIS

    @Test
    fun `record stores only normalized successful query associations`() {
        val recorded = SearchLearningPolicy.record(
            entries = emptyList(),
            rawQuery = "  ÍNSTA  ",
            packageName = "com.instagram.android",
            nowMillis = now,
        )
        val ignored = SearchLearningPolicy.record(
            entries = recorded,
            rawQuery = "i",
            packageName = "com.other",
            nowMillis = now,
        )

        assertEquals(1, ignored.size)
        assertEquals("insta", ignored.single().query)
        assertEquals(1, ignored.single().selectionCount)
    }

    @Test
    fun `record increments an existing association and refreshes recency`() {
        val old = AppSearchLearningEntry("inst", "com.instagram", 2, now - DAY_MILLIS)

        val updated = SearchLearningPolicy.record(
            listOf(old),
            "inst",
            "com.instagram",
            now,
        ).single()

        assertEquals(3, updated.selectionCount)
        assertEquals(now, updated.lastSelectedAtMillis)
    }

    @Test
    fun `prune expires old entries and removes uninstalled packages`() {
        val entries = listOf(
            AppSearchLearningEntry("inst", "installed", 1, now),
            AppSearchLearningEntry(
                "old",
                "installed",
                1,
                now - SearchLearningPolicy.RETENTION_MILLIS - 1,
            ),
            AppSearchLearningEntry("gone", "uninstalled", 1, now),
        )

        val pruned = SearchLearningPolicy.prune(entries, now, setOf("installed"))

        assertEquals(listOf("inst"), pruned.map(AppSearchLearningEntry::query))
    }

    @Test
    fun `learning remains bounded to one hundred strongest associations`() {
        val entries = List(150) { index ->
            AppSearchLearningEntry(
                query = "query$index",
                packageName = "package$index",
                selectionCount = index + 1,
                lastSelectedAtMillis = now,
            )
        }

        val pruned = SearchLearningPolicy.prune(entries, now)

        assertEquals(SearchLearningPolicy.MAX_ASSOCIATIONS, pruned.size)
        assertTrue(pruned.all { it.selectionCount >= 51 })
    }

    private companion object {
        const val DAY_MILLIS = 24L * 60L * 60L * 1_000L
    }
}
