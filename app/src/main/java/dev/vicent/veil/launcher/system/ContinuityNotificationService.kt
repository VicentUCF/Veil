package dev.vicent.veil.launcher.system

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import dev.vicent.veil.launcher.AppNotificationIndicatorCandidate
import dev.vicent.veil.launcher.AppNotificationIndicatorPolicy
import dev.vicent.veil.launcher.AppNotificationIndicatorTracker
import dev.vicent.veil.launcher.NotificationContinuityKind
import dev.vicent.veil.launcher.NotificationContinuityPolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

internal data class ActiveNotification(
    val id: String,
    val packageName: String,
    val appLabel: String,
    val title: String,
    val text: String?,
    val postedAtMillis: Long,
    val kind: Kind,
    val progress: Float?,
    val isComplete: Boolean,
    val contentIntent: PendingIntent?,
) {
    enum class Kind { NAVIGATION, PROGRESS }
}

class ContinuityNotificationService : NotificationListenerService() {
    override fun onListenerConnected() {
        val active = runCatching { activeNotifications.orEmpty().toList() }.getOrDefault(emptyList())
        publish(active.mapNotNull(::toActiveNotification))
        replaceNotificationIndicators(active, currentRanking)
    }

    override fun onListenerDisconnected() {
        publish(emptyList())
        clearNotificationIndicators()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(ComponentName(this, ContinuityNotificationService::class.java))
        }
    }

    override fun onDestroy() {
        publish(emptyList())
        clearNotificationIndicators()
        super.onDestroy()
    }

    override fun onNotificationPosted(
        statusBarNotification: StatusBarNotification,
        rankingMap: RankingMap,
    ) {
        val item = toActiveNotification(statusBarNotification)
        val next = currentNotifications.value
            .filterNot { it.id == statusBarNotification.key }
            .toMutableList()
        if (item != null) next += item
        publish(next)
        updateNotificationIndicator(statusBarNotification, rankingMap)
    }

    override fun onNotificationRemoved(statusBarNotification: StatusBarNotification) {
        publish(currentNotifications.value.filterNot { it.id == statusBarNotification.key })
        indicatorTracker.remove(statusBarNotification.key)
        publishNotificationIndicatorPackages()
    }

    override fun onNotificationRankingUpdate(rankingMap: RankingMap) {
        val active = runCatching { activeNotifications.orEmpty().toList() }.getOrDefault(emptyList())
        replaceNotificationIndicators(active, rankingMap)
    }

    private fun publish(items: List<ActiveNotification>) {
        currentNotifications.value = items.sortedByDescending(ActiveNotification::postedAtMillis)
    }

    private fun replaceNotificationIndicators(
        notifications: List<StatusBarNotification>,
        rankingMap: RankingMap,
    ) {
        indicatorTracker.replace(
            notifications.mapNotNull { notification ->
                notification.toIndicatorPackage(rankingMap)?.let { packageName ->
                    notification.key to packageName
                }
            },
        )
        publishNotificationIndicatorPackages()
    }

    private fun updateNotificationIndicator(
        notification: StatusBarNotification,
        rankingMap: RankingMap,
    ) {
        indicatorTracker.update(notification.key, notification.toIndicatorPackage(rankingMap))
        publishNotificationIndicatorPackages()
    }

    private fun StatusBarNotification.toIndicatorPackage(rankingMap: RankingMap): String? {
        val notification = notification
        val ranking = Ranking()
        val hasRanking = rankingMap.getRanking(key, ranking)
        val canShowBadge = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (hasRanking) ranking.canShowBadge() else false
        } else {
            null
        }
        val candidate = AppNotificationIndicatorCandidate(
            packageName = packageName,
            ownPackageName = this@ContinuityNotificationService.packageName,
            category = notification.category,
            progressMax = notification.extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0),
            isClearable = isClearable,
            isOngoing = notification.flags and Notification.FLAG_ONGOING_EVENT != 0,
            isForegroundService = notification.flags and Notification.FLAG_FOREGROUND_SERVICE != 0,
            canShowBadge = canShowBadge,
        )
        return packageName.takeIf { AppNotificationIndicatorPolicy.shouldShow(candidate) }
    }

    private fun clearNotificationIndicators() {
        indicatorTracker.clear()
        publishNotificationIndicatorPackages()
    }

    private fun publishNotificationIndicatorPackages() {
        currentNotificationIndicatorPackages.value = indicatorTracker.packages()
    }

    private fun toActiveNotification(sbn: StatusBarNotification): ActiveNotification? {
        if (sbn.packageName == packageName || sbn.notification.isGroupSummary) return null
        val notification = sbn.notification
        val extras = notification.extras
        val max = extras.getInt(Notification.EXTRA_PROGRESS_MAX, 0)
        val current = extras.getInt(Notification.EXTRA_PROGRESS, 0)
        val continuityKind = NotificationContinuityPolicy.classify(notification.category, max)
            ?: return null

        val title = extras.getCharSequence(Notification.EXTRA_TITLE)
            ?.toString()
            ?.trim()
            ?.takeIf(String::isNotEmpty)
            ?: appLabel(sbn.packageName)
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)
            ?.toString()
            ?.trim()
            ?.takeIf(String::isNotEmpty)

        return ActiveNotification(
            id = sbn.key,
            packageName = sbn.packageName,
            appLabel = appLabel(sbn.packageName),
            title = title.take(MAX_TEXT_LENGTH),
            text = text?.take(MAX_TEXT_LENGTH),
            postedAtMillis = System.currentTimeMillis(),
            kind = if (continuityKind == NotificationContinuityKind.NAVIGATION) {
                ActiveNotification.Kind.NAVIGATION
            } else {
                ActiveNotification.Kind.PROGRESS
            },
            progress = if (max > 0) current.toFloat().div(max).coerceIn(0f, 1f) else null,
            isComplete = if (max > 0) {
                current >= max
            } else {
                notification.flags and Notification.FLAG_ONGOING_EVENT == 0
            },
            contentIntent = notification.contentIntent,
        )
    }

    private fun appLabel(targetPackage: String): String = runCatching {
        val info = packageManager.getApplicationInfo(targetPackage, 0)
        packageManager.getApplicationLabel(info).toString()
    }.getOrDefault(targetPackage)

    private val Notification.isGroupSummary: Boolean
        get() = flags and Notification.FLAG_GROUP_SUMMARY != 0

    companion object {
        private const val MAX_TEXT_LENGTH = 120
        private val indicatorTracker = AppNotificationIndicatorTracker()
        private val currentNotifications = MutableStateFlow<List<ActiveNotification>>(emptyList())
        private val currentNotificationIndicatorPackages = MutableStateFlow<Set<String>>(emptySet())
        internal val notifications: StateFlow<List<ActiveNotification>> =
            currentNotifications.asStateFlow()
        internal val notificationIndicatorPackages: StateFlow<Set<String>> =
            currentNotificationIndicatorPackages.asStateFlow()

        internal fun open(id: String): Boolean {
            val intent = currentNotifications.value.firstOrNull { it.id == id }?.contentIntent
                ?: return false
            return try {
                intent.send()
                true
            } catch (_: PendingIntent.CanceledException) {
                currentNotifications.value = currentNotifications.value.filterNot { it.id == id }
                false
            }
        }
    }
}
