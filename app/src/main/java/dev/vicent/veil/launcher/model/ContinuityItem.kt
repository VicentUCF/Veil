package dev.vicent.veil.launcher.model

sealed interface ContinuityItem {
    val id: String
    val packageName: String
    val appLabel: String
    val title: String
    val subtitle: String?
    val updatedAtMillis: Long
    val expiresAtMillis: Long?
    val supportedActions: Set<ContinuityAction>

    data class Media(
        override val id: String,
        override val packageName: String,
        override val appLabel: String,
        override val title: String,
        override val subtitle: String?,
        override val updatedAtMillis: Long,
        override val expiresAtMillis: Long?,
        override val supportedActions: Set<ContinuityAction>,
        val isPlaying: Boolean,
        val isVideo: Boolean,
    ) : ContinuityItem

    data class Navigation(
        override val id: String,
        override val packageName: String,
        override val appLabel: String,
        override val title: String,
        override val subtitle: String?,
        override val updatedAtMillis: Long,
        override val expiresAtMillis: Long?,
        override val supportedActions: Set<ContinuityAction>,
    ) : ContinuityItem

    data class Progress(
        override val id: String,
        override val packageName: String,
        override val appLabel: String,
        override val title: String,
        override val subtitle: String?,
        override val updatedAtMillis: Long,
        override val expiresAtMillis: Long?,
        override val supportedActions: Set<ContinuityAction>,
        val progress: Float?,
        val isComplete: Boolean,
    ) : ContinuityItem
}

enum class ContinuityAction {
    OPEN,
    TOGGLE_PLAYBACK,
}
