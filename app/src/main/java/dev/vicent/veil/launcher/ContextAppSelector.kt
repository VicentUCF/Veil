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
        configuredPackageNames: List<String>,
        installedApps: List<AppCandidate>,
        count: Int,
    ): List<String> {
        val installedPackages = installedApps.mapTo(mutableSetOf(), AppCandidate::packageName)
        val reserved = configuredPackageNames.filter(installedPackages::contains).toSet()
        val used = mutableSetOf<String>()
        val fallbacks = orderedCandidates(kind, installedApps)
            .filter { it.packageName !in reserved }
            .iterator()
        return configuredPackageNames.take(count).mapNotNull { configured ->
            val resolved = configured.takeIf { it in installedPackages && used.add(it) }
                ?: generateSequence { if (fallbacks.hasNext()) fallbacks.next() else null }
                    .map(AppCandidate::packageName)
                    .firstOrNull(used::add)
            resolved
        }
    }

    fun selectPackageNames(
        kind: LauncherContextKind,
        configuredPackageNames: List<String>,
        installedApps: List<AppCandidate>,
        count: Int,
    ): List<String> {
        val wantedCategory = wantedCategory(kind)
            ?: return configuredPackageNames.distinct().take(count)
        val installedPackages = installedApps.mapTo(mutableSetOf(), AppCandidate::packageName)
        val configured = configuredPackageNames.filter(installedPackages::contains).distinct()
        val configuredSet = configured.toSet()
        val automatic = installedApps.asSequence()
            .filter { it.category == wantedCategory && it.packageName !in configuredSet }
            .map(AppCandidate::packageName)
            .distinct()
        return (configured.asSequence() + automatic).take(count).toList()
    }

    private fun orderedCandidates(
        kind: LauncherContextKind,
        installedApps: List<AppCandidate>,
    ): Sequence<AppCandidate> {
        val category = wantedCategory(kind)
        return installedApps.asSequence().sortedWith(
            compareBy<AppCandidate> { candidate ->
                if (category != null && candidate.category == category) 0 else 1
            }.thenBy(AppCandidate::packageName),
        )
    }

    private fun wantedCategory(kind: LauncherContextKind): AppCategory? = when (kind) {
        LauncherContextKind.WORK -> AppCategory.WORK
        LauncherContextKind.MEDIA -> AppCategory.MEDIA
        LauncherContextKind.SOCIAL -> AppCategory.SOCIAL
        else -> null
    }
}
