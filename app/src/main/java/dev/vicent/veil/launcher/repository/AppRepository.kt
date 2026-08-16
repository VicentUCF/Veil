package dev.vicent.veil.launcher.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.ResolveInfo
import android.os.Build
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.AppCategory
import java.text.Collator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class AppRepository(context: Context) {
    private val packageManager = context.packageManager
    private val ownPackageName = context.packageName
    private val iconLoader = AppIconLoader(packageManager)
    private val cacheMutex = Mutex()
    private var cachedApps: List<LauncherApp>? = null

    suspend fun loadLaunchableApps(): List<LauncherApp> = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            cachedApps ?: queryLaunchableApps().also { cachedApps = it }
        }
    }

    suspend fun refreshLaunchableApps(): List<LauncherApp> = withContext(Dispatchers.IO) {
        cacheMutex.withLock {
            queryLaunchableApps().also { cachedApps = it }
        }
    }

    /**
     * Resolves only the apps needed by the context docks. This keeps personalized shortcuts
     * available during a cold launcher start while the complete app list is still loading.
     */
    suspend fun loadPriorityApps(packageNames: Collection<String>): List<LauncherApp> =
        withContext(Dispatchers.IO) {
            packageNames.asSequence()
                .distinct()
                .mapNotNull { packageName ->
                    val launcherIntent = Intent(Intent.ACTION_MAIN)
                        .addCategory(Intent.CATEGORY_LAUNCHER)
                        .setPackage(packageName)
                    packageManager.queryIntentActivities(launcherIntent, 0)
                        .asSequence()
                        .firstOrNull(::isLaunchable)
                        ?.toLauncherApp()
                }
                .toList()
        }

    private fun queryLaunchableApps(): List<LauncherApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val collator = Collator.getInstance()

        return packageManager.queryIntentActivities(launcherIntent, 0)
            .asSequence()
            .filter(::isLaunchable)
            .map { resolveInfo -> resolveInfo.toLauncherApp() }
            .distinctBy(LauncherApp::packageName)
            .sortedWith { first, second -> collator.compare(first.label, second.label) }
            .toList()
    }

    private fun isLaunchable(resolveInfo: ResolveInfo): Boolean {
        val activityInfo = resolveInfo.activityInfo ?: return false
        return activityInfo.exported &&
            activityInfo.enabled &&
            activityInfo.applicationInfo.enabled &&
            activityInfo.packageName != ownPackageName
    }

    private fun ResolveInfo.toLauncherApp(): LauncherApp {
        val launchActivity = requireNotNull(activityInfo)
        val packageName = launchActivity.packageName
        return LauncherApp(
            packageName = packageName,
            label = loadLabel(packageManager)
                .toString()
                .trim()
                .ifBlank { packageName },
            componentName = ComponentName(packageName, launchActivity.name),
            icon = iconLoader.load(this),
            category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                launchActivity.applicationInfo.category.toAppCategory()
            } else {
                AppCategory.GENERAL
            },
        )
    }

    private fun Int.toAppCategory(): AppCategory = when (this) {
        android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.WORK
        android.content.pm.ApplicationInfo.CATEGORY_AUDIO,
        android.content.pm.ApplicationInfo.CATEGORY_VIDEO,
        android.content.pm.ApplicationInfo.CATEGORY_IMAGE,
        -> AppCategory.MEDIA
        android.content.pm.ApplicationInfo.CATEGORY_GAME -> AppCategory.GAME
        else -> AppCategory.GENERAL
    }
}
