package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.LauncherContext
import dev.vicent.veil.launcher.model.LauncherPreferences
import dev.vicent.veil.launcher.model.QuickActionSpec

internal class LauncherContextResolver(
    contexts: List<LauncherContext>,
    private val quickActionCount: Int,
) {
    val emptyContexts: List<ResolvedLauncherContext> = contexts.map { context ->
        ResolvedLauncherContext(definition = context, apps = emptyList())
    }

    fun resolve(
        installedApps: List<LauncherApp>,
        preferences: LauncherPreferences,
        appScanComplete: Boolean,
    ): List<ResolvedLauncherContext> {
        val appsByPackage = installedApps.associateBy(LauncherApp::packageName)
        val candidates = installedApps.map { AppCandidate(it.packageName, it.category) }

        return emptyContexts.map { context ->
            val override = preferences.contextAppOverrides[context.definition.kind]
            if (override != null) {
                val slots = (override + List(quickActionCount) { null }).take(quickActionCount)
                val quickActions = slots.mapIndexed { index, packageName ->
                    packageName?.let(appsByPackage::get)
                        ?.let(ResolvedQuickAction::App)
                        ?: ResolvedQuickAction.Empty(index)
                }
                return@map context.withQuickActions(quickActions)
            }

            if (!appScanComplete) {
                val quickActions = context.definition.quickActions
                    .take(quickActionCount)
                    .mapIndexed { index, spec ->
                        when (spec) {
                            is QuickActionSpec.App -> spec.packageCandidates
                                .firstNotNullOfOrNull(appsByPackage::get)
                                ?.let(ResolvedQuickAction::App)
                                ?: ResolvedQuickAction.Empty(index)
                            is QuickActionSpec.Setting -> ResolvedQuickAction.Setting(spec.id)
                        }
                    }
                    .padWithEmptySlots()
                return@map context.withQuickActions(quickActions)
            }

            val configuredPackageCandidates = context.definition.quickActions
                .filterIsInstance<QuickActionSpec.App>()
                .map(QuickActionSpec.App::packageCandidates)
            val resolvedAppPackages = ContextAppSelector.selectQuickSlots(
                kind = context.definition.kind,
                configuredPackageCandidates = configuredPackageCandidates,
                installedApps = candidates,
                count = configuredPackageCandidates.size,
            )
            var appSlotIndex = 0
            val resolvedDefaults = context.definition.quickActions.mapIndexed { index, spec ->
                when (spec) {
                    is QuickActionSpec.App -> resolvedAppPackages
                        .getOrNull(appSlotIndex++)
                        ?.let(appsByPackage::get)
                        ?.let(ResolvedQuickAction::App)
                        ?: ResolvedQuickAction.Empty(index)
                    is QuickActionSpec.Setting -> ResolvedQuickAction.Setting(spec.id)
                }
            }.take(quickActionCount)

            context.withQuickActions(resolvedDefaults.padWithEmptySlots())
        }
    }

    fun priorityPackageNames(preferences: LauncherPreferences): List<String> =
        buildList {
            emptyContexts.forEach { context ->
                context.definition.quickActions.forEach { action ->
                    if (action is QuickActionSpec.App) addAll(action.packageCandidates)
                }
            }
            preferences.contextAppOverrides.values.forEach { slots ->
                slots.forEach { packageName -> if (packageName != null) add(packageName) }
            }
        }.distinct()

    private fun List<ResolvedQuickAction>.padWithEmptySlots(): List<ResolvedQuickAction> =
        (this + List(quickActionCount) { index ->
            ResolvedQuickAction.Empty(size + index)
        }).take(quickActionCount)

    private fun ResolvedLauncherContext.withQuickActions(
        quickActions: List<ResolvedQuickAction>,
    ): ResolvedLauncherContext = copy(
        apps = quickActions.mapNotNull { (it as? ResolvedQuickAction.App)?.app },
        quickActions = quickActions,
    )
}
