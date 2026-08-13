package dev.vicent.veil.launcher.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.provider.MediaStore
import android.os.Build
import androidx.core.net.toUri
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.AppCategory
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.AppCandidate
import dev.vicent.veil.launcher.ContextAppSelector
import java.text.Collator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AppRepository(context: Context) {
    private val packageManager = context.packageManager
    private val ownPackageName = context.packageName
    private var cachedApps: List<LauncherApp>? = null

    suspend fun loadLaunchableApps(): List<LauncherApp> = withContext(Dispatchers.IO) {
        cachedApps ?: queryLaunchableApps().also { cachedApps = it }
    }

    fun resolveConfiguredApps(
        packageNames: List<String>,
        installedApps: List<LauncherApp>,
    ): List<LauncherApp> {
        val appsByPackage = installedApps.associateBy(LauncherApp::packageName)
        return packageNames.mapNotNull(appsByPackage::get).distinctBy(LauncherApp::packageName)
    }

    suspend fun selectContextApps(
        kind: LauncherContextKind,
        configuredApps: List<LauncherApp>,
        installedApps: List<LauncherApp>,
        count: Int,
    ): List<LauncherApp> = withContext(Dispatchers.IO) {
        val installedPackages = installedApps.mapTo(mutableSetOf(), LauncherApp::packageName)
        val preferredPackage = when (kind) {
            LauncherContextKind.MEDIA -> resolvePreferredPackage(musicIntent(), installedPackages)
            LauncherContextKind.SOCIAL -> resolvePreferredPackage(messagesIntent(), installedPackages)
            else -> null
        }
        val selectedPackages = ContextAppSelector.selectPackageNames(
            kind = kind,
            configuredPackageNames = configuredApps.map(LauncherApp::packageName) +
                listOfNotNull(preferredPackage),
            installedApps = installedApps.map { AppCandidate(it.packageName, it.category) },
            count = count,
        )
        val appsByPackage = installedApps.associateBy(LauncherApp::packageName)
        selectedPackages.mapNotNull(appsByPackage::get)
    }

    suspend fun selectQuickApps(
        kind: LauncherContextKind,
        configuredPackageNames: List<String>,
        installedApps: List<LauncherApp>,
        count: Int,
    ): List<LauncherApp> = withContext(Dispatchers.Default) {
        val selected = ContextAppSelector.selectQuickSlots(
            kind = kind,
            configuredPackageNames = configuredPackageNames,
            installedApps = installedApps.map { AppCandidate(it.packageName, it.category) },
            count = count,
        )
        val byPackage = installedApps.associateBy(LauncherApp::packageName)
        selected.mapNotNull(byPackage::get)
    }

    suspend fun selectAutomaticHomeApps(
        installedApps: List<LauncherApp>,
        count: Int,
    ): List<LauncherApp> = withContext(Dispatchers.IO) {
        val appsByPackage = installedApps.associateBy(LauncherApp::packageName)
        val installedPackages = appsByPackage.keys
        val preferredPackages = listOfNotNull(
            resolvePreferredPackage(phoneIntent(), installedPackages),
            resolvePreferredPackage(messagesIntent(), installedPackages),
            resolvePreferredPackage(browserIntent(), installedPackages),
            resolvePreferredPackage(musicIntent(), installedPackages),
            resolvePreferredPackage(cameraIntent(), installedPackages),
        ).distinct()

        preferredPackages.mapNotNull(appsByPackage::get).take(count)
    }

    private fun resolvePreferredPackage(
        intent: Intent,
        installedPackages: Set<String>,
    ): String? {
        val defaultPackage = packageManager
            .resolveActivity(intent, 0)
            ?.activityInfo
            ?.packageName
            ?.takeIf(installedPackages::contains)

        if (defaultPackage != null) return defaultPackage

        return packageManager.queryIntentActivities(intent, 0)
            .asSequence()
            .mapNotNull { it.activityInfo?.packageName }
            .firstOrNull(installedPackages::contains)
    }

    private fun queryLaunchableApps(): List<LauncherApp> {
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val collator = Collator.getInstance()

        return packageManager.queryIntentActivities(launcherIntent, 0)
            .asSequence()
            .filter { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo
                activityInfo != null &&
                    activityInfo.exported &&
                    activityInfo.enabled &&
                    activityInfo.applicationInfo.enabled &&
                    activityInfo.packageName != ownPackageName
            }
            .map { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo
                val packageName = activityInfo.packageName
                LauncherApp(
                    packageName = packageName,
                    label = resolveInfo.loadLabel(packageManager)
                        .toString()
                        .trim()
                        .ifBlank { packageName },
                    componentName = ComponentName(packageName, activityInfo.name),
                    icon = runCatching { resolveInfo.loadIcon(packageManager) }.getOrNull(),
                    category = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        activityInfo.applicationInfo.category.toAppCategory()
                    } else {
                        AppCategory.GENERAL
                    },
                )
            }
            .distinctBy(LauncherApp::packageName)
            .sortedWith { first, second -> collator.compare(first.label, second.label) }
            .toList()
    }

    private fun phoneIntent() = Intent(Intent.ACTION_DIAL, "tel:".toUri())

    private fun messagesIntent() = Intent(Intent.ACTION_SENDTO, "smsto:".toUri())

    private fun browserIntent() =
        Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_BROWSER)

    private fun musicIntent() =
        Intent.makeMainSelectorActivity(Intent.ACTION_MAIN, Intent.CATEGORY_APP_MUSIC)

    private fun cameraIntent() = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)

    private fun Int.toAppCategory(): AppCategory = when (this) {
        android.content.pm.ApplicationInfo.CATEGORY_PRODUCTIVITY -> AppCategory.WORK
        android.content.pm.ApplicationInfo.CATEGORY_AUDIO,
        android.content.pm.ApplicationInfo.CATEGORY_VIDEO,
        android.content.pm.ApplicationInfo.CATEGORY_IMAGE,
        -> AppCategory.MEDIA
        android.content.pm.ApplicationInfo.CATEGORY_SOCIAL -> AppCategory.SOCIAL
        else -> AppCategory.GENERAL
    }
}
