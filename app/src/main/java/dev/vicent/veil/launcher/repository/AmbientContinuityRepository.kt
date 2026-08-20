package dev.vicent.veil.launcher.repository

import android.content.Context
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.ContinuityItem
import dev.vicent.veil.launcher.SystemTimeProvider
import dev.vicent.veil.launcher.TimeProvider
import dev.vicent.veil.launcher.system.ContinuityNotificationService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AmbientContinuityRepository(
    private val context: Context,
    private val timeProvider: TimeProvider = SystemTimeProvider,
) {
    private val mutableItems = MutableStateFlow<List<ContinuityItem>>(emptyList())
    val items: StateFlow<List<ContinuityItem>> = mutableItems.asStateFlow()
    private val mutableNotificationIndicatorPackages = MutableStateFlow<Set<String>>(emptySet())
    val notificationIndicatorPackages: StateFlow<Set<String>> =
        mutableNotificationIndicatorPackages.asStateFlow()
    private val onboardingStore = ContinuityOnboardingStore(context)
    private val mediaSource = MediaSessionContinuitySource(context, timeProvider, ::publish)

    private var scope: CoroutineScope? = null
    private var accessEnabled = false
    private var notificationItems: List<ContinuityItem> = emptyList()
    private val dismissedIds = mutableSetOf<String>()

    fun start(scope: CoroutineScope) {
        if (this.scope != null) return
        this.scope = scope
        scope.launch {
            ContinuityNotificationService.notifications.collectLatest { notifications ->
                notificationItems = if (accessEnabled) {
                    notifications.map(ContinuityNotificationMapper::toContinuityItem)
                } else {
                    emptyList()
                }
                publish()
            }
        }
        scope.launch {
            ContinuityNotificationService.notificationIndicatorPackages.collectLatest { packages ->
                mutableNotificationIndicatorPackages.value = if (accessEnabled) {
                    packages
                } else {
                    emptySet()
                }
            }
        }
        scope.launch {
            while (isActive) {
                delay(EXPIRY_TICK_MILLIS)
                publish()
            }
        }
        scope.launch {
            try {
                awaitCancellation()
            } finally {
                setAccessEnabled(false)
            }
        }
    }

    fun setAccessEnabled(enabled: Boolean) {
        if (accessEnabled == enabled) return
        accessEnabled = enabled
        mediaSource.setEnabled(enabled)
        if (enabled) {
            notificationItems = ContinuityNotificationService.notifications.value.map(
                ContinuityNotificationMapper::toContinuityItem,
            )
            mutableNotificationIndicatorPackages.value =
                ContinuityNotificationService.notificationIndicatorPackages.value
        } else {
            notificationItems = emptyList()
            mutableNotificationIndicatorPackages.value = emptySet()
        }
        publish()
    }

    fun isNotificationOnboardingSeen(): Boolean = onboardingStore.isSeen()

    fun markNotificationOnboardingSeen() {
        onboardingStore.markSeen()
    }

    fun perform(itemId: String, action: ContinuityAction, positionMillis: Long? = null): Boolean {
        val successful = when (action) {
            ContinuityAction.OPEN -> mediaSource.open(itemId) ||
                ContinuityNotificationService.open(itemId)
            else -> mediaSource.perform(itemId, action, positionMillis)
        }
        if (!successful) {
            dismissedIds += itemId
            publish()
        }
        return successful
    }

    fun pauseMedia(itemId: String): Boolean {
        return mediaSource.pause(itemId)
    }


    private fun publish() {
        val now = timeProvider.currentTimeMillis()
        mutableItems.value = (notificationItems + mediaSource.items).filter { item ->
            val expiresAt = item.expiresAtMillis
            item.id !in dismissedIds &&
                (expiresAt == null || expiresAt > now)
        }
    }


    private companion object {
        const val MAX_TEXT_LENGTH = 120
        const val PAUSED_MEDIA_LIFETIME_MILLIS = 30 * 60 * 1000L
        const val EXPIRY_TICK_MILLIS = 30_000L
        const val MAX_ARTWORK_SIZE = 512
        const val TRACK_CHANGE_GRACE_MILLIS = 1_500L
    }
}
