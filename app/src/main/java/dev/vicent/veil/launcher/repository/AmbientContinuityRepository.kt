package dev.vicent.veil.launcher.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.ContinuityItem
import dev.vicent.veil.launcher.system.ActiveNotification
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

class AmbientContinuityRepository(private val context: Context) {
    private val mediaSessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, ContinuityNotificationService::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private val mutableItems = MutableStateFlow<List<ContinuityItem>>(emptyList())
    val items: StateFlow<List<ContinuityItem>> = mutableItems.asStateFlow()

    private var scope: CoroutineScope? = null
    private var accessEnabled = false
    private var notificationItems: List<ContinuityItem> = emptyList()
    private var mediaItems: List<ContinuityItem.Media> = emptyList()
    private var controllers: Map<String, MediaController> = emptyMap()
    private var callbacks: Map<MediaController, MediaController.Callback> = emptyMap()
    private val mediaObservedAt = mutableMapOf<String, Long>()
    private val mediaSignatures = mutableMapOf<String, String>()
    private val dismissedIds = mutableSetOf<String>()

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener {
        refreshMediaSessions()
    }

    fun start(scope: CoroutineScope) {
        if (this.scope != null) return
        this.scope = scope
        scope.launch {
            ContinuityNotificationService.notifications.collectLatest { notifications ->
                notificationItems = if (accessEnabled) {
                    notifications.map(::toContinuityItem)
                } else {
                    emptyList()
                }
                publish()
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
        if (enabled) {
            runCatching {
                mediaSessionManager.addOnActiveSessionsChangedListener(
                    sessionListener,
                    listenerComponent,
                    handler,
                )
            }
            refreshMediaSessions()
            notificationItems = ContinuityNotificationService.notifications.value.map(::toContinuityItem)
        } else {
            runCatching { mediaSessionManager.removeOnActiveSessionsChangedListener(sessionListener) }
            clearMediaCallbacks()
            controllers = emptyMap()
            mediaItems = emptyList()
            notificationItems = emptyList()
        }
        publish()
    }

    fun perform(itemId: String, action: ContinuityAction): Boolean {
        val successful = when (action) {
            ContinuityAction.OPEN -> open(itemId)
            ContinuityAction.TOGGLE_PLAYBACK -> togglePlayback(itemId)
        }
        if (!successful) {
            dismissedIds += itemId
            publish()
        }
        return successful
    }

    private fun refreshMediaSessions() {
        if (!accessEnabled) return
        val activeControllers = try {
            mediaSessionManager.getActiveSessions(listenerComponent)
        } catch (_: SecurityException) {
            emptyList()
        }
        clearMediaCallbacks()
        val now = System.currentTimeMillis()
        val nextControllers = mutableMapOf<String, MediaController>()
        val nextCallbacks = mutableMapOf<MediaController, MediaController.Callback>()

        activeControllers.forEach { controller ->
            val id = mediaId(controller)
            nextControllers[id] = controller
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) = refreshMediaSessions()
                override fun onMetadataChanged(metadata: MediaMetadata?) = refreshMediaSessions()
                override fun onSessionDestroyed() = refreshMediaSessions()
            }
            controller.registerCallback(callback, handler)
            nextCallbacks[controller] = callback
            val signature = controller.continuitySignature()
            if (mediaSignatures[id] != signature) {
                mediaSignatures[id] = signature
                mediaObservedAt[id] = now
            }
        }
        val activeIds = nextControllers.keys
        mediaObservedAt.keys.retainAll(activeIds)
        mediaSignatures.keys.retainAll(activeIds)
        controllers = nextControllers
        callbacks = nextCallbacks
        mediaItems = activeControllers.mapNotNull { controller ->
            controller.toContinuityItem(mediaId(controller), now)
        }
        publish()
    }

    private fun MediaController.toContinuityItem(id: String, now: Long): ContinuityItem.Media? {
        val state = playbackState ?: return null
        val metadata = metadata
        val title = metadata?.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: metadata?.getText(MediaMetadata.METADATA_KEY_TITLE)
            ?: return null
        val subtitle = metadata?.getText(MediaMetadata.METADATA_KEY_ARTIST)
            ?: metadata?.getText(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: metadata?.getText(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
        val playing = state.state.isPlaybackActive()
        val observedAt = mediaObservedAt[id] ?: now
        val supported = buildSet {
            add(ContinuityAction.OPEN)
            if (state.actions and (PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE) != 0L) {
                add(ContinuityAction.TOGGLE_PLAYBACK)
            }
        }
        return ContinuityItem.Media(
            id = id,
            packageName = packageName,
            appLabel = appLabel(packageName),
            title = title.toString().trim().take(MAX_TEXT_LENGTH),
            subtitle = subtitle?.toString()?.trim()?.take(MAX_TEXT_LENGTH),
            updatedAtMillis = observedAt,
            expiresAtMillis = if (playing) null else observedAt + PAUSED_MEDIA_LIFETIME_MILLIS,
            supportedActions = supported,
            isPlaying = playing,
            isVideo = packageName.contains("youtube", ignoreCase = true) ||
                packageName.contains("video", ignoreCase = true) ||
                packageName.contains("netflix", ignoreCase = true),
        )
    }

    private fun toContinuityItem(item: ActiveNotification): ContinuityItem = when (item.kind) {
        ActiveNotification.Kind.NAVIGATION -> ContinuityItem.Navigation(
            id = item.id,
            packageName = item.packageName,
            appLabel = item.appLabel,
            title = item.title,
            subtitle = item.text,
            updatedAtMillis = item.postedAtMillis,
            expiresAtMillis = null,
            supportedActions = if (item.contentIntent != null) {
                setOf(ContinuityAction.OPEN)
            } else {
                emptySet()
            },
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
            supportedActions = if (item.contentIntent != null) {
                setOf(ContinuityAction.OPEN)
            } else {
                emptySet()
            },
            progress = item.progress,
            isComplete = item.isComplete,
        )
    }

    private fun open(id: String): Boolean {
        val media = controllers[id]
        if (media != null) {
            val sessionIntent = media.sessionActivity
            if (sessionIntent != null) {
                try {
                    sessionIntent.send()
                    return true
                } catch (_: android.app.PendingIntent.CanceledException) {
                    // Fall through to the package's launcher activity.
                }
            }
            val intent = context.packageManager.getLaunchIntentForPackage(media.packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                ?: return false
            return runCatching { context.startActivity(intent) }.isSuccess
        }
        return ContinuityNotificationService.open(id)
    }

    private fun togglePlayback(id: String): Boolean {
        val controller = controllers[id] ?: return false
        return runCatching {
            if (controller.playbackState?.state.isPlaybackActive()) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
        }.isSuccess
    }

    private fun clearMediaCallbacks() {
        callbacks.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
        callbacks = emptyMap()
    }

    private fun publish() {
        val now = System.currentTimeMillis()
        mutableItems.value = (notificationItems + mediaItems).filter { item ->
            val expiresAt = item.expiresAtMillis
            item.id !in dismissedIds &&
                (expiresAt == null || expiresAt > now)
        }
    }

    private fun appLabel(packageName: String): String = runCatching {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(info).toString()
    }.getOrDefault(packageName)

    private fun MediaController.continuitySignature(): String {
        val metadata = metadata
        return listOf(
            playbackState?.state,
            metadata?.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),
            metadata?.getText(MediaMetadata.METADATA_KEY_TITLE),
            metadata?.getText(MediaMetadata.METADATA_KEY_ARTIST),
        ).joinToString(separator = "|")
    }

    private fun Int?.isPlaybackActive(): Boolean =
        this == PlaybackState.STATE_PLAYING ||
            this == PlaybackState.STATE_BUFFERING ||
            this == PlaybackState.STATE_CONNECTING

    private fun mediaId(controller: MediaController) =
        "media:${controller.packageName}:${controller.sessionToken.hashCode()}"

    private companion object {
        const val MAX_TEXT_LENGTH = 120
        const val PAUSED_MEDIA_LIFETIME_MILLIS = 30 * 60 * 1000L
        const val COMPLETED_PROGRESS_LIFETIME_MILLIS = 10 * 60 * 1000L
        const val EXPIRY_TICK_MILLIS = 30_000L
    }
}
