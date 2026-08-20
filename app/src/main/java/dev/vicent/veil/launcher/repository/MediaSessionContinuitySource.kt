package dev.vicent.veil.launcher.repository

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import androidx.core.graphics.scale
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.ContinuityItem
import dev.vicent.veil.launcher.TimeProvider
import dev.vicent.veil.launcher.system.ContinuityNotificationService

internal class MediaSessionContinuitySource(
    private val context: Context,
    private val timeProvider: TimeProvider,
    private val onItemsChanged: () -> Unit,
) {
    private val sessionManager = context.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(context, ContinuityNotificationService::class.java)
    private val handler = Handler(Looper.getMainLooper())
    private var enabled = false
    private var controllers: Map<String, MediaController> = emptyMap()
    private var callbacks: Map<MediaController, MediaController.Callback> = emptyMap()
    private val observedAt = mutableMapOf<String, Long>()
    private val signatures = mutableMapOf<String, String>()
    private val actions = mutableMapOf<String, Long>()
    private val pendingTrackChanges = mutableMapOf<String, PendingTrackChange>()

    var items: List<ContinuityItem.Media> = emptyList()
        private set

    private val sessionListener = MediaSessionManager.OnActiveSessionsChangedListener {
        refresh()
    }

    fun setEnabled(enabled: Boolean) {
        if (this.enabled == enabled) return
        this.enabled = enabled
        if (enabled) {
            runCatching {
                sessionManager.addOnActiveSessionsChangedListener(
                    sessionListener,
                    listenerComponent,
                    handler,
                )
            }
            refresh()
        } else {
            runCatching { sessionManager.removeOnActiveSessionsChangedListener(sessionListener) }
            clearCallbacks()
            controllers = emptyMap()
            items = emptyList()
            actions.clear()
            pendingTrackChanges.clear()
            onItemsChanged()
        }
    }

    fun open(id: String): Boolean {
        val controller = controllers[id] ?: return false
        controller.sessionActivity?.let { sessionIntent ->
            try {
                sessionIntent.send()
                return true
            } catch (_: android.app.PendingIntent.CanceledException) {
                // Fall through to the package launcher.
            }
        }
        val intent = context.packageManager.getLaunchIntentForPackage(controller.packageName)
            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            ?: return false
        return runCatching { context.startActivity(intent) }.isSuccess
    }

    fun perform(id: String, action: ContinuityAction, positionMillis: Long?): Boolean =
        when (action) {
            ContinuityAction.OPEN -> open(id)
            ContinuityAction.TOGGLE_PLAYBACK -> togglePlayback(id)
            ContinuityAction.SKIP_PREVIOUS -> skipTrack(id) { skipToPrevious() }
            ContinuityAction.SKIP_NEXT -> skipTrack(id) { skipToNext() }
            ContinuityAction.SEEK_TO -> positionMillis?.let { position ->
                transport(id) { seekTo(position) }
            } ?: false
        }

    fun pause(id: String): Boolean {
        val controller = controllers[id] ?: return false
        val availableActions = controller.playbackState?.actions
            ?.takeIf { it != 0L }
            ?: actions[id]
            ?: return false
        if (availableActions and (PlaybackState.ACTION_PAUSE or PlaybackState.ACTION_PLAY_PAUSE) == 0L) {
            return false
        }
        pendingTrackChanges.remove(id)
        return runCatching { controller.transportControls.pause() }.isSuccess
    }

    private fun refresh() {
        if (!enabled) return
        val activeControllers = try {
            sessionManager.getActiveSessions(listenerComponent)
        } catch (_: SecurityException) {
            emptyList()
        }
        clearCallbacks()
        val now = timeProvider.currentTimeMillis()
        val nextControllers = mutableMapOf<String, MediaController>()
        val nextCallbacks = mutableMapOf<MediaController, MediaController.Callback>()

        activeControllers.forEach { controller ->
            val id = mediaId(controller)
            nextControllers[id] = controller
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) = refresh()
                override fun onMetadataChanged(metadata: MediaMetadata?) = refresh()
                override fun onSessionDestroyed() = refresh()
            }
            controller.registerCallback(callback, handler)
            nextCallbacks[controller] = callback
            controller.playbackState?.actions?.takeIf { it != 0L }?.let { actions[id] = it }
            val signature = controller.continuitySignature()
            if (signatures[id] != signature) {
                signatures[id] = signature
                observedAt[id] = now
            }
        }
        val activeIds = nextControllers.keys
        observedAt.keys.retainAll(activeIds)
        signatures.keys.retainAll(activeIds)
        actions.keys.retainAll(activeIds)
        pendingTrackChanges.keys.retainAll(activeIds)
        activeControllers.forEach { controller ->
            val id = mediaId(controller)
            val pending = pendingTrackChanges[id] ?: return@forEach
            val completed = controller.trackSignature() != pending.previousTrackSignature &&
                controller.playbackState?.state.isPlaybackActive()
            if (completed || SystemClock.elapsedRealtime() >= pending.expiresAtElapsedRealtime) {
                pendingTrackChanges.remove(id)
            }
        }
        controllers = nextControllers
        callbacks = nextCallbacks
        items = activeControllers.mapNotNull { controller ->
            controller.toContinuityItem(mediaId(controller), now)
        }
        onItemsChanged()
    }

    private fun MediaController.toContinuityItem(id: String, now: Long): ContinuityItem.Media? {
        val playback = playbackState ?: return null
        val currentMetadata = metadata
        val title = currentMetadata?.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: currentMetadata?.getText(MediaMetadata.METADATA_KEY_TITLE)
            ?: return null
        val subtitle = currentMetadata?.getText(MediaMetadata.METADATA_KEY_ARTIST)
            ?: currentMetadata?.getText(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: currentMetadata?.getText(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
        val pending = pendingTrackChanges[id]
            ?.takeIf { SystemClock.elapsedRealtime() < it.expiresAtElapsedRealtime }
        val playing = playback.state.isPlaybackActive() || pending?.keepPlaying == true
        val firstObservedAt = observedAt[id] ?: now
        val availableActions = playback.actions.takeIf { it != 0L } ?: actions[id] ?: 0L
        val supported = buildSet {
            add(ContinuityAction.OPEN)
            if (
                availableActions and (
                    PlaybackState.ACTION_PLAY or PlaybackState.ACTION_PAUSE or
                        PlaybackState.ACTION_PLAY_PAUSE
                    ) != 0L
            ) add(ContinuityAction.TOGGLE_PLAYBACK)
            if (availableActions and PlaybackState.ACTION_SKIP_TO_PREVIOUS != 0L) add(ContinuityAction.SKIP_PREVIOUS)
            if (availableActions and PlaybackState.ACTION_SKIP_TO_NEXT != 0L) add(ContinuityAction.SKIP_NEXT)
            if (availableActions and PlaybackState.ACTION_SEEK_TO != 0L) add(ContinuityAction.SEEK_TO)
        }
        return ContinuityItem.Media(
            id = id,
            packageName = packageName,
            appLabel = appLabel(packageName),
            title = title.toString().trim().take(MAX_TEXT_LENGTH),
            subtitle = subtitle?.toString()?.trim()?.take(MAX_TEXT_LENGTH),
            updatedAtMillis = firstObservedAt,
            expiresAtMillis = if (playing) null else firstObservedAt + PAUSED_MEDIA_LIFETIME_MILLIS,
            supportedActions = supported,
            isPlaying = playing,
            isVideo = packageName.contains("youtube", true) ||
                packageName.contains("video", true) || packageName.contains("netflix", true),
            durationMillis = currentMetadata?.getLong(MediaMetadata.METADATA_KEY_DURATION)
                ?.takeIf { it > 0L },
            positionMillis = playback.position.takeIf { it >= 0L },
            positionUpdatedAtElapsedRealtime = playback.lastPositionUpdateTime.takeIf { it > 0L },
            playbackSpeed = playback.playbackSpeed,
            artwork = currentMetadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
                ?.scaledToFit(MAX_ARTWORK_SIZE)
                ?: currentMetadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
                    ?.scaledToFit(MAX_ARTWORK_SIZE),
        )
    }

    private fun togglePlayback(id: String): Boolean {
        val controller = controllers[id] ?: return false
        val pending = pendingTrackChanges.remove(id)
        return runCatching {
            if (controller.playbackState?.state.isPlaybackActive() || pending?.keepPlaying == true) {
                controller.transportControls.pause()
            } else {
                controller.transportControls.play()
            }
        }.isSuccess
    }

    private fun skipTrack(
        id: String,
        action: MediaController.TransportControls.() -> Unit,
    ): Boolean {
        val controller = controllers[id] ?: return false
        val transition = PendingTrackChange(
            previousTrackSignature = controller.trackSignature(),
            keepPlaying = controller.playbackState?.state.isPlaybackActive(),
            expiresAtElapsedRealtime = SystemClock.elapsedRealtime() + TRACK_CHANGE_GRACE_MILLIS,
        )
        pendingTrackChanges[id] = transition
        if (runCatching { controller.transportControls.action() }.isFailure) {
            pendingTrackChanges.remove(id)
            return false
        }
        handler.postDelayed(
            {
                if (pendingTrackChanges[id] === transition) {
                    pendingTrackChanges.remove(id)
                    refresh()
                }
            },
            TRACK_CHANGE_GRACE_MILLIS,
        )
        return true
    }

    private fun transport(
        id: String,
        action: MediaController.TransportControls.() -> Unit,
    ): Boolean {
        val controller = controllers[id] ?: return false
        return runCatching { controller.transportControls.action() }.isSuccess
    }

    private fun clearCallbacks() {
        callbacks.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
        callbacks = emptyMap()
    }

    private fun Bitmap.scaledToFit(maxSize: Int): Bitmap {
        if (width <= maxSize && height <= maxSize) return this
        val ratio = minOf(maxSize.toFloat() / width, maxSize.toFloat() / height)
        return scale(
            (width * ratio).toInt().coerceAtLeast(1),
            (height * ratio).toInt().coerceAtLeast(1),
        )
    }

    private fun appLabel(packageName: String): String = runCatching {
        val info = context.packageManager.getApplicationInfo(packageName, 0)
        context.packageManager.getApplicationLabel(info).toString()
    }.getOrDefault(packageName)

    private fun MediaController.continuitySignature(): String = listOf(
        playbackState?.state,
        metadata?.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),
        metadata?.getText(MediaMetadata.METADATA_KEY_TITLE),
        metadata?.getText(MediaMetadata.METADATA_KEY_ARTIST),
    ).joinToString("|")

    private fun MediaController.trackSignature(): String = listOf(
        metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_ID),
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            metadata?.getString(MediaMetadata.METADATA_KEY_MEDIA_URI)
        } else null,
        metadata?.getText(MediaMetadata.METADATA_KEY_DISPLAY_TITLE),
        metadata?.getText(MediaMetadata.METADATA_KEY_TITLE),
        metadata?.getText(MediaMetadata.METADATA_KEY_ARTIST),
        metadata?.getText(MediaMetadata.METADATA_KEY_ALBUM),
        metadata?.getLong(MediaMetadata.METADATA_KEY_DURATION),
    ).joinToString("|")

    private fun Int?.isPlaybackActive(): Boolean =
        this == PlaybackState.STATE_PLAYING || this == PlaybackState.STATE_BUFFERING ||
            this == PlaybackState.STATE_CONNECTING

    private fun mediaId(controller: MediaController) =
        "media:${controller.packageName}:${controller.sessionToken.hashCode()}"

    private data class PendingTrackChange(
        val previousTrackSignature: String,
        val keepPlaying: Boolean,
        val expiresAtElapsedRealtime: Long,
    )

    private companion object {
        const val MAX_TEXT_LENGTH = 120
        const val PAUSED_MEDIA_LIFETIME_MILLIS = 30 * 60 * 1000L
        const val MAX_ARTWORK_SIZE = 512
        const val TRACK_CHANGE_GRACE_MILLIS = 1_500L
    }
}
