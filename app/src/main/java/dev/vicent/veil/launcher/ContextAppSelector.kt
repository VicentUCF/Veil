package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.AppCategory
import dev.vicent.veil.launcher.model.LauncherContextKind

data class AppCandidate(
    val packageName: String,
    val category: AppCategory,
)

object ContextAppSelector {
    fun selectQuickSlots(
        kind: LauncherContextKind,
        configuredPackageCandidates: List<List<String>>,
        installedApps: List<AppCandidate>,
        count: Int,
    ): List<String?> {
        val installedPackages = installedApps.mapTo(mutableSetOf(), AppCandidate::packageName)
        val reserved = configuredPackageCandidates
            .asSequence()
            .flatten()
            .filter(installedPackages::contains)
            .toSet()
        val used = mutableSetOf<String>()
        val fallbacks = orderedCandidates(kind, installedApps)
            .filter { it.packageName !in reserved }
            .iterator()
        return configuredPackageCandidates.take(count).map { candidates ->
            val resolved = candidates.firstOrNull { candidate ->
                candidate in installedPackages && used.add(candidate)
            }
                ?: generateSequence { if (fallbacks.hasNext()) fallbacks.next() else null }
                    .map(AppCandidate::packageName)
                    .firstOrNull(used::add)
            resolved
        }
    }

    private fun orderedCandidates(
        kind: LauncherContextKind,
        installedApps: List<AppCandidate>,
    ): Sequence<AppCandidate> {
        val category = wantedCategory(kind) ?: return emptySequence()
        return installedApps.asSequence()
            .filter { it.category == category }
            .sortedBy(AppCandidate::packageName)
    }

    private fun wantedCategory(kind: LauncherContextKind): AppCategory? = when (kind) {
        LauncherContextKind.WORK -> AppCategory.WORK
        LauncherContextKind.MEDIA -> AppCategory.MEDIA
        LauncherContextKind.GAME -> AppCategory.GAME
        else -> null
    }
}
