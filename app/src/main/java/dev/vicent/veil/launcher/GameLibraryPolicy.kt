package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.AppCategory
import dev.vicent.veil.launcher.model.LauncherApp
import java.util.Locale

object GameLibraryPolicy {
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
