package dev.vicent.veil.launcher.model

enum class AudioChannel { MEDIA, RING, ALARM }

data class AudioChannelLevel(
    val channel: AudioChannel,
    val current: Int,
    val maximum: Int,
) {
    val fraction: Float
        get() = if (maximum > 0) current.toFloat() / maximum else 0f
}

enum class AudioSpectrumAvailability { NEEDS_PERMISSION, IDLE, ACTIVE, UNAVAILABLE }

data class AudioMixerState(
    val channels: List<AudioChannelLevel> = emptyList(),
    val spectrum: List<Float> = List(16) { 0f },
    val spectrumAvailability: AudioSpectrumAvailability = AudioSpectrumAvailability.NEEDS_PERMISSION,
)
