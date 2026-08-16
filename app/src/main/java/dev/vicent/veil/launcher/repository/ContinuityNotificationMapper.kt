package dev.vicent.veil.launcher.repository

import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.ContinuityItem
import dev.vicent.veil.launcher.system.ActiveNotification

internal object ContinuityNotificationMapper {
    private const val COMPLETED_PROGRESS_LIFETIME_MILLIS = 10 * 60 * 1000L

    fun toContinuityItem(item: ActiveNotification): ContinuityItem = when (item.kind) {
        ActiveNotification.Kind.NAVIGATION -> ContinuityItem.Navigation(
            id = item.id,
            packageName = item.packageName,
            appLabel = item.appLabel,
            title = item.title,
            subtitle = item.text,
            updatedAtMillis = item.postedAtMillis,
            expiresAtMillis = null,
            supportedActions = item.openAction(),
        )
        ActiveNotification.Kind.PROGRESS -> ContinuityItem.Progress(
            id = item.id,
            packageName = item.packageName,
            appLabel = item.appLabel,
            title = item.title,
            subtitle = item.text,
            updatedAtMillis = item.postedAtMillis,
            expiresAtMillis = if (item.isComplete) {
                item.postedAtMillis + COMPLETED_PROGRESS_LIFETIME_MILLIS
            } else {
                null
            },
            supportedActions = item.openAction(),
            progress = item.progress,
            isComplete = item.isComplete,
        )
    }

    private fun ActiveNotification.openAction(): Set<ContinuityAction> =
        if (contentIntent != null) setOf(ContinuityAction.OPEN) else emptySet()
}
