package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.AppCategory
import dev.vicent.veil.launcher.model.LauncherContextKind

data class AppCandidate(
    val packageName: String,
    val category: AppCategory,
)

object ContextAppSelector {
    fun selectPackageNames(
        kind: LauncherContextKind,
        configuredPackageNames: List<String>,
        installedApps: List<AppCandidate>,
        count: Int,
    ): List<String> {
        val wantedCategory = when (kind) {
            LauncherContextKind.WORK -> AppCategory.WORK
            LauncherContextKind.MEDIA -> AppCategory.MEDIA
            LauncherContextKind.SOCIAL -> AppCategory.SOCIAL
            else -> return configuredPackageNames.distinct().take(count)
        }
        val installedPackages = installedApps.mapTo(mutableSetOf(), AppCandidate::packageName)
        val configured = configuredPackageNames.filter(installedPackages::contains).distinct()
        val configuredSet = configured.toSet()
        val automatic = installedApps.asSequence()
            .filter { it.category == wantedCategory && it.packageName !in configuredSet }
            .map(AppCandidate::packageName)
            .distinct()
        return (configured.asSequence() + automatic).take(count).toList()
    }
}
