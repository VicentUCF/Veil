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
            configuredPackageNames = listOf("fixed.one", "missing", "fixed.three"),
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
            configuredPackageNames = listOf("fixed.one", "removed.work", "fixed.three"),
            installedApps = remaining,
            count = 3,
        )

        assertEquals(listOf("fixed.one", "a.work", "fixed.three"), result)
    }

    @Test
    fun `configured apps lead and automatic category apps fill remaining slots`() {
        val installed = listOf(
            AppCandidate("work.auto", AppCategory.WORK),
            AppCandidate("configured.media", AppCategory.MEDIA),
            AppCandidate("work.second", AppCategory.WORK),
        )

        val result = ContextAppSelector.selectPackageNames(
            kind = LauncherContextKind.WORK,
            configuredPackageNames = listOf("configured.media"),
            installedApps = installed,
            count = 3,
        )

        assertEquals(listOf("configured.media", "work.auto", "work.second"), result)
    }

    @Test
    fun `missing configured apps duplicates and overflow are removed`() {
        val installed = listOf(
            AppCandidate("one", AppCategory.SOCIAL),
            AppCandidate("two", AppCategory.SOCIAL),
            AppCandidate("three", AppCategory.SOCIAL),
        )

        val result = ContextAppSelector.selectPackageNames(
            kind = LauncherContextKind.SOCIAL,
            configuredPackageNames = listOf("missing", "one", "one"),
            installedApps = installed,
            count = 2,
        )

        assertEquals(listOf("one", "two"), result)
    }
}
