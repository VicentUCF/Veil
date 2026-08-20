package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.CalendarEventSummary
import dev.vicent.veil.launcher.model.FocusTimerStatus
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.WorkspaceCapability
import dev.vicent.veil.config.LauncherConfig
import java.util.Calendar
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class WorkspaceDataPolicyTest {
    @Test
    fun `dock stays hidden only in current`() {
        assertEquals(false, WorkspaceLayoutPolicy.showsContextDock(LauncherContextKind.CURRENT))
        assertEquals(
            listOf(true, true, true, true, true, true),
            listOf(
                LauncherContextKind.WORK,
                LauncherContextKind.FOCUS,
                LauncherContextKind.MEDIA,
                LauncherContextKind.GAME,
                LauncherContextKind.TOOLS,
                LauncherContextKind.ON_THE_GO,
            ).map(WorkspaceLayoutPolicy::showsContextDock),
        )
    }

    @Test
    fun `work agenda shows at most three events from today`() {
        val now = at(dayOffset = 0, hour = 9)
        val events = listOf(
            event("one", at(0, 10)),
            event("two", at(0, 11)),
            event("three", at(0, 12)),
            event("four", at(0, 13)),
            event("tomorrow", at(1, 9)),
        )

        assertEquals(
            listOf("one", "two", "three"),
            AgendaPolicy.workEvents(events, now).map(CalendarEventSummary::title),
        )
    }

    @Test
    fun `network and permission backed data activates only for declaring view`() {
        val byKind = LauncherConfig.workspaceCatalog.associateBy { it.kind }

        assertEquals(
            setOf(LauncherContextKind.CURRENT, LauncherContextKind.ON_THE_GO),
            byKind.values.filter {
                WorkspaceActivationPolicy.uses(it, WorkspaceCapability.WEATHER)
            }.mapTo(mutableSetOf()) { it.kind },
        )
        assertEquals(
            setOf(LauncherContextKind.GAME),
            byKind.values.filter {
                WorkspaceActivationPolicy.uses(it, WorkspaceCapability.STEAM)
            }.mapTo(mutableSetOf()) { it.kind },
        )
        assertEquals(
            setOf(LauncherContextKind.MEDIA),
            byKind.values.filter {
                WorkspaceActivationPolicy.uses(it, WorkspaceCapability.AUDIO)
            }.mapTo(mutableSetOf()) { it.kind },
        )
    }

    @Test
    fun `work agenda falls back to only the next available event`() {
        val now = at(dayOffset = 0, hour = 18)
        val events = listOf(
            event("next", at(1, 9)),
            event("later", at(1, 11)),
        )

        assertEquals(
            listOf("next"),
            AgendaPolicy.workEvents(events, now).map(CalendarEventSummary::title),
        )
    }

    @Test
    fun `running focus derives remaining time from its deadline`() {
        assertEquals(
            45_000L,
            FocusTimerPolicy.remainingMillis(
                status = FocusTimerStatus.RUNNING,
                endAtMillis = 100_000L,
                storedRemainingMillis = 70_000L,
                nowMillis = 55_000L,
            ),
        )
    }

    @Test
    fun `paused focus keeps its stored remaining time`() {
        assertEquals(
            70_000L,
            FocusTimerPolicy.remainingMillis(
                status = FocusTimerStatus.PAUSED,
                endAtMillis = 100_000L,
                storedRemainingMillis = 70_000L,
                nowMillis = 95_000L,
            ),
        )
    }

    @Test
    fun `expired running focus reaches zero for repository completion`() {
        assertEquals(
            0L,
            FocusTimerPolicy.remainingMillis(
                status = FocusTimerStatus.RUNNING,
                endAtMillis = 50_000L,
                storedRemainingMillis = 70_000L,
                nowMillis = 55_000L,
            ),
        )
    }

    @Test
    fun `system usage reports the used fraction`() {
        assertEquals(0.75f, StorageUsagePolicy.usedFraction(availableBytes = 25L, totalBytes = 100L))
    }

    @Test
    fun `system usage bounds invalid available values`() {
        assertEquals(1f, StorageUsagePolicy.usedFraction(availableBytes = -1L, totalBytes = 100L))
        assertEquals(0f, StorageUsagePolicy.usedFraction(availableBytes = 150L, totalBytes = 100L))
    }

    @Test
    fun `system usage is unavailable without a valid total`() {
        assertNull(StorageUsagePolicy.usedFraction(availableBytes = 0L, totalBytes = 0L))
        assertNull(StorageUsagePolicy.usedFraction(availableBytes = 0L, totalBytes = -1L))
    }

    @Test
    fun `game library includes games and pinned misclassified apps in stable order`() {
        val installed = listOf(
            GameLibraryCandidate("game.z", "Zelda", dev.vicent.veil.launcher.model.AppCategory.GAME),
            GameLibraryCandidate("emulator", "Dolphin", dev.vicent.veil.launcher.model.AppCategory.GENERAL),
            GameLibraryCandidate("other", "Calculator", dev.vicent.veil.launcher.model.AppCategory.GENERAL),
            GameLibraryCandidate("game.a", "álamo", dev.vicent.veil.launcher.model.AppCategory.GAME),
            GameLibraryCandidate("game.a", "álamo duplicate", dev.vicent.veil.launcher.model.AppCategory.GAME),
        )

        assertEquals(
            listOf("emulator", "game.z", "game.a"),
            GameLibraryPolicy.gameLibraryPackages(installed, setOf("emulator")),
        )
    }

    private fun event(title: String, startMillis: Long) = CalendarEventSummary(
        id = title.hashCode().toLong(),
        title = title,
        startMillis = startMillis,
        endMillis = startMillis + 30 * 60_000L,
    )

    private fun at(dayOffset: Int, hour: Int): Long = Calendar.getInstance().run {
        add(Calendar.DAY_OF_YEAR, dayOffset)
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        timeInMillis
    }
}
