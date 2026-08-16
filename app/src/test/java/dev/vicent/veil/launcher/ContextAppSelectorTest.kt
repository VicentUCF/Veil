package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.AppCategory
import dev.vicent.veil.launcher.model.LauncherContextKind
import org.junit.Test
import kotlin.test.assertEquals

class ContextAppSelectorTest {
    @Test
    fun `quick slots preserve configured positions and fill missing apps deterministically`() {
        val installed = listOf(
            AppCandidate("z.work", AppCategory.WORK),
            AppCandidate("fixed.one", AppCategory.GENERAL),
            AppCandidate("a.work", AppCategory.WORK),
            AppCandidate("fixed.three", AppCategory.GENERAL),
        )

        val result = ContextAppSelector.selectQuickSlots(
            kind = LauncherContextKind.WORK,
            configuredPackageCandidates = listOf(
                listOf("fixed.one"),
                listOf("missing"),
                listOf("fixed.three"),
            ),
            installedApps = installed,
            count = 3,
        )

        assertEquals(listOf("fixed.one", "a.work", "fixed.three"), result)
    }

    @Test
    fun `quick slots replace an app that becomes unavailable without moving stable neighbors`() {
        val remaining = listOf(
            AppCandidate("fixed.one", AppCategory.GENERAL),
            AppCandidate("fixed.three", AppCategory.GENERAL),
            AppCandidate("a.work", AppCategory.WORK),
            AppCandidate("z.work", AppCategory.WORK),
        )

        val result = ContextAppSelector.selectQuickSlots(
            kind = LauncherContextKind.WORK,
            configuredPackageCandidates = listOf(
                listOf("fixed.one"),
                listOf("removed.work"),
                listOf("fixed.three"),
            ),
            installedApps = remaining,
            count = 3,
        )

        assertEquals(listOf("fixed.one", "a.work", "fixed.three"), result)
    }

    @Test
    fun `slot selects the first installed provider candidate`() {
        val installed = listOf(
            AppCandidate("browser.second", AppCategory.GENERAL),
            AppCandidate("unrelated", AppCategory.GENERAL),
        )

        val result = ContextAppSelector.selectQuickSlots(
            kind = LauncherContextKind.CURRENT,
            configuredPackageCandidates = listOf(
                listOf("browser.first", "browser.second"),
            ),
            installedApps = installed,
            count = 1,
        )

        assertEquals(listOf("browser.second"), result)
    }

    @Test
    fun `context without a category leaves an unrelated missing slot empty`() {
        val result = ContextAppSelector.selectQuickSlots(
            kind = LauncherContextKind.CURRENT,
            configuredPackageCandidates = listOf(listOf("missing.phone")),
            installedApps = listOf(AppCandidate("unrelated", AppCategory.GENERAL)),
            count = 1,
        )

        assertEquals(listOf(null), result)
    }

}
