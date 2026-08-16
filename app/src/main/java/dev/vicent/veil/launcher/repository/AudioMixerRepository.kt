package dev.vicent.veil.launcher.repository

import android.content.Context
import android.media.AudioManager
import android.media.audiofx.Visualizer
import dev.vicent.veil.launcher.model.AudioChannel
import dev.vicent.veil.launcher.model.AudioChannelLevel
import dev.vicent.veil.launcher.model.AudioMixerState
import dev.vicent.veil.launcher.model.AudioSpectrumAvailability
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln

class AudioMixerRepository(private val context: Context) {
    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val mutableState = MutableStateFlow(AudioMixerState(channels = readChannels()))
    val state: StateFlow<AudioMixerState> = mutableState.asStateFlow()

    private var visualizer: Visualizer? = null
    private var visualizerPermissionGranted = false
    private var appVisible = false
    private var mediaPlaying = false
    private var mediaWorkspaceVisible = false
    private var smoothedSpectrum = FloatArray(SPECTRUM_BANDS)

    fun start(scope: CoroutineScope) {
        scope.launch {
            try {
                while (isActive) {
                    refreshChannels()
                    delay(VOLUME_REFRESH_MILLIS)
                }
            } finally {
                releaseVisualizer()
            }
        }
    }

    fun setVisualizerPermissionGranted(granted: Boolean) {
        if (visualizerPermissionGranted == granted) return
        visualizerPermissionGranted = granted
        updateVisualizer()
    }

    fun setAppVisible(visible: Boolean) {
        if (appVisible == visible) return
        appVisible = visible
        updateVisualizer()
    }

    fun setMediaPlaying(playing: Boolean) {
        if (mediaPlaying == playing) return
        mediaPlaying = playing
        updateVisualizer()
    }

    fun setMediaWorkspaceVisible(visible: Boolean) {
        if (mediaWorkspaceVisible == visible) return
        mediaWorkspaceVisible = visible
        updateVisualizer()
    }

    fun setVolume(channel: AudioChannel, fraction: Float): Boolean {
        val stream = channel.streamType()
        val maximum = audioManager.getStreamMaxVolume(stream).coerceAtLeast(1)
        val index = (maximum * fraction.coerceIn(0f, 1f)).toInt().coerceIn(0, maximum)
        val successful = runCatching {
            audioManager.setStreamVolume(stream, index, 0)
        }.isSuccess
        refreshChannels()
        return successful
    }

    private fun refreshChannels() {
        val channels = readChannels()
        if (channels != mutableState.value.channels) {
            mutableState.value = mutableState.value.copy(channels = channels)
        }
    }

    private fun readChannels(): List<AudioChannelLevel> = AudioChannel.entries.map { channel ->
        val stream = channel.streamType()
        AudioChannelLevel(
            channel = channel,
            current = audioManager.getStreamVolume(stream),
            maximum = audioManager.getStreamMaxVolume(stream).coerceAtLeast(1),
        )
    }

    private fun updateVisualizer() {
        releaseVisualizer()
        when {
            !visualizerPermissionGranted -> publishSpectrum(AudioSpectrumAvailability.NEEDS_PERMISSION)
            !shouldRunAudioVisualizer(
                permissionGranted = visualizerPermissionGranted,
                appVisible = appVisible,
                mediaWorkspaceVisible = mediaWorkspaceVisible,
                mediaPlaying = mediaPlaying,
            ) -> publishSpectrum(AudioSpectrumAvailability.IDLE)
            else -> startVisualizer()
        }
    }

    private fun startVisualizer() {
        val created = runCatching {
            Visualizer(OUTPUT_MIX_SESSION).apply {
                captureSize = Visualizer.getCaptureSizeRange()[1]
                scalingMode = Visualizer.SCALING_MODE_NORMALIZED
                setDataCaptureListener(
                    object : Visualizer.OnDataCaptureListener {
                        override fun onWaveFormDataCapture(
                            visualizer: Visualizer?,
                            waveform: ByteArray?,
                            samplingRate: Int,
                        ) = Unit

                        override fun onFftDataCapture(
                            visualizer: Visualizer?,
                            fft: ByteArray?,
                            samplingRate: Int,
                        ) {
                            if (fft != null) publishFft(fft, samplingRate)
                        }
                    },
                    Visualizer.getMaxCaptureRate() / CAPTURE_RATE_DIVISOR,
                    false,
                    true,
                )
                enabled = true
            }
        }.getOrNull()
        visualizer = created
        if (created == null) publishSpectrum(AudioSpectrumAvailability.UNAVAILABLE)
    }

    private fun publishFft(fft: ByteArray, samplingRateMilliHertz: Int) {
        val magnitudes = calculateSpectrum(
            fft = fft,
            samplingRateHertz = samplingRateMilliHertz / 1000,
            bandCount = SPECTRUM_BANDS,
            minimumFrequency = MINIMUM_FREQUENCY,
            maximumFrequency = MAXIMUM_FREQUENCY,
        )
        magnitudes.forEachIndexed { index, magnitude ->
            smoothedSpectrum[index] = if (magnitude >= smoothedSpectrum[index]) {
                magnitude
            } else {
                smoothedSpectrum[index] * SPECTRUM_DECAY + magnitude * (1f - SPECTRUM_DECAY)
            }
        }
        mutableState.value = mutableState.value.copy(
            spectrum = smoothedSpectrum.toList(),
            spectrumAvailability = AudioSpectrumAvailability.ACTIVE,
        )
    }

    private fun publishSpectrum(availability: AudioSpectrumAvailability) {
        smoothedSpectrum.fill(0f)
        mutableState.value = mutableState.value.copy(
            spectrum = smoothedSpectrum.toList(),
            spectrumAvailability = availability,
        )
    }

    private fun releaseVisualizer() {
        visualizer?.let { current ->
            runCatching { current.enabled = false }
            runCatching { current.release() }
        }
        visualizer = null
    }

    private fun AudioChannel.streamType(): Int = when (this) {
        AudioChannel.MEDIA -> AudioManager.STREAM_MUSIC
        AudioChannel.RING -> AudioManager.STREAM_RING
        AudioChannel.ALARM -> AudioManager.STREAM_ALARM
    }

    private companion object {
        const val OUTPUT_MIX_SESSION = 0
        const val SPECTRUM_BANDS = 16
        const val MINIMUM_FREQUENCY = 50f
        const val MAXIMUM_FREQUENCY = 10_000f
        const val CAPTURE_RATE_DIVISOR = 2
        const val SPECTRUM_DECAY = 0.72f
        const val VOLUME_REFRESH_MILLIS = 750L
    }
}

internal fun shouldRunAudioVisualizer(
    permissionGranted: Boolean,
    appVisible: Boolean,
    mediaWorkspaceVisible: Boolean,
    mediaPlaying: Boolean,
): Boolean = permissionGranted && appVisible && mediaWorkspaceVisible && mediaPlaying

internal fun calculateSpectrum(
    fft: ByteArray,
    samplingRateHertz: Int,
    bandCount: Int,
    minimumFrequency: Float,
    maximumFrequency: Float,
): FloatArray {
    if (fft.size < 4 || samplingRateHertz <= 0 || bandCount <= 0) return FloatArray(bandCount)
    val binCount = fft.size / 2
    val frequencyPerBin = samplingRateHertz.toFloat() / fft.size
    val raw = FloatArray(bandCount)
    val logMinimum = ln(minimumFrequency)
    val logRange = ln(maximumFrequency) - logMinimum

    for (band in 0 until bandCount) {
        val lowerFrequency = exp(logMinimum + logRange * band / bandCount)
        val upperFrequency = exp(logMinimum + logRange * (band + 1) / bandCount)
        val startBin = (lowerFrequency / frequencyPerBin)
            .toInt()
            .coerceIn(1, binCount - 1)
        val endBinExclusive = ceil(upperFrequency / frequencyPerBin)
            .toInt()
            .coerceIn(startBin + 1, binCount)

        for (bin in startBin until endBinExclusive) {
            val real = fft[bin * 2].toFloat()
            val imaginary = fft[bin * 2 + 1].toFloat()
            raw[band] = maxOf(raw[band], hypot(real, imaginary))
        }
    }

    val peak = raw.maxOrNull()?.coerceAtLeast(1f) ?: 1f
    return FloatArray(bandCount) { index ->
        val bassPresence = when (index) {
            0 -> 1.35f
            1 -> 1.20f
            2 -> 1.05f
            else -> 1f
        }
        (raw[index] / peak * bassPresence).coerceIn(0f, 1f)
    }
}
