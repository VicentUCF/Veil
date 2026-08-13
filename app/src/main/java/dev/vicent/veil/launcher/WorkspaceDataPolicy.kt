package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.CalendarEventSummary
import dev.vicent.veil.launcher.model.FocusTimerStatus
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.AppCategory
import dev.vicent.veil.launcher.model.LauncherApp
import java.util.Calendar
import java.util.Locale

object WorkspaceDataPolicy {
    fun showsContextDock(kind: LauncherContextKind): Boolean =
        kind != LauncherContextKind.CURRENT

    fun workEvents(
        events: List<CalendarEventSummary>,
        nowMillis: Long,
    ): List<CalendarEventSummary> {
        if (events.isEmpty()) return emptyList()
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val eventCalendar = Calendar.getInstance()
        val today = events.filter { event ->
            eventCalendar.timeInMillis = event.startMillis
            eventCalendar.get(Calendar.ERA) == now.get(Calendar.ERA) &&
                eventCalendar.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                eventCalendar.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
        }
        return (today.ifEmpty { events.take(1) }).take(3)
    }

    fun focusRemainingMillis(
        status: FocusTimerStatus,
        endAtMillis: Long,
        storedRemainingMillis: Long,
        nowMillis: Long,
    ): Long = if (status == FocusTimerStatus.RUNNING) {
        (endAtMillis - nowMillis).coerceAtLeast(0L)
    } else {
        storedRemainingMillis.coerceAtLeast(0L)
    }

    fun usedFraction(availableBytes: Long, totalBytes: Long): Float? {
        if (totalBytes <= 0L) return null
        val boundedAvailable = availableBytes.coerceIn(0L, totalBytes)
        return (totalBytes - boundedAvailable).toFloat() / totalBytes
    }

    fun gameLibrary(
        installedApps: List<LauncherApp>,
        favoriteApps: List<LauncherApp>,
    ): List<LauncherApp> {
        val selectedPackages = gameLibraryPackages(
            installedApps = installedApps.map { app ->
                GameLibraryCandidate(app.packageName, app.label, app.category)
            },
            favoritePackages = favoriteApps.map(LauncherApp::packageName).toSet(),
        )
        val appsByPackage = installedApps.associateBy(LauncherApp::packageName)
        return selectedPackages.mapNotNull(appsByPackage::get)
    }

    internal fun gameLibraryPackages(
        installedApps: List<GameLibraryCandidate>,
        favoritePackages: Set<String>,
    ): List<String> = installedApps.asSequence()
        .filter { it.category == AppCategory.GAME || it.packageName in favoritePackages }
        .distinctBy(GameLibraryCandidate::packageName)
        .sortedWith(
            compareBy<GameLibraryCandidate> { it.label.lowercase(Locale.ROOT) }
                .thenBy(GameLibraryCandidate::packageName),
        )
        .map(GameLibraryCandidate::packageName)
        .toList()
}

internal data class GameLibraryCandidate(
    val packageName: String,
    val label: String,
    val category: AppCategory,
)
