package dev.vicent.veil.launcher.system

import android.app.Notification
import android.app.PendingIntent
import android.content.ComponentName
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
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
        publish(activeNotifications.orEmpty().mapNotNull(::toActiveNotification))
    }

    override fun onListenerDisconnected() {
        publish(emptyList())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            requestRebind(ComponentName(this, ContinuityNotificationService::class.java))
        }
    }

    override fun onDestroy() {
        publish(emptyList())
        super.onDestroy()
    }

    override fun onNotificationPosted(statusBarNotification: StatusBarNotification) {
        val item = toActiveNotification(statusBarNotification)
        val next = currentNotifications.value
            .filterNot { it.id == statusBarNotification.key }
            .toMutableList()
        if (item != null) next += item
        publish(next)
    }

    override fun onNotificationRemoved(statusBarNotification: StatusBarNotification) {
        publish(currentNotifications.value.filterNot { it.id == statusBarNotification.key })
    }

    private fun publish(items: List<ActiveNotification>) {
        currentNotifications.value = items.sortedByDescending(ActiveNotification::postedAtMillis)
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
        private val currentNotifications = MutableStateFlow<List<ActiveNotification>>(emptyList())
        internal val notifications: StateFlow<List<ActiveNotification>> =
            currentNotifications.asStateFlow()

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
