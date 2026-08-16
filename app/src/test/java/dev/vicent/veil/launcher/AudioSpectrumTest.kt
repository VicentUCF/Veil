package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.repository.calculateSpectrum
import dev.vicent.veil.launcher.repository.shouldRunAudioVisualizer
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AudioSpectrumTest {
    @Test
    fun visualizerOnlyRunsWithEveryForegroundGateOpen() {
        assertTrue(
            shouldRunAudioVisualizer(
                permissionGranted = true,
                appVisible = true,
                mediaWorkspaceVisible = true,
                mediaPlaying = true,
            ),
        )
        assertFalse(shouldRunAudioVisualizer(false, true, true, true))
        assertFalse(shouldRunAudioVisualizer(true, false, true, true))
        assertFalse(shouldRunAudioVisualizer(true, true, false, true))
        assertFalse(shouldRunAudioVisualizer(true, true, true, false))
    }

    @Test
    fun `empty FFT produces silent bands`() {
        val result = calculateSpectrum(
            fft = ByteArray(128),
            samplingRateHertz = 44_100,
            bandCount = 16,
            minimumFrequency = 50f,
            maximumFrequency = 10_000f,
        )

        assertEquals(16, result.size)
        assertTrue(result.all { it == 0f })
    }

    @Test
    fun `FFT energy is normalized into bounded logarithmic bands`() {
        val fft = ByteArray(256)
        fft[8] = 100
        fft[9] = 80
        fft[80] = 50
        fft[81] = 40

        val result = calculateSpectrum(
            fft = fft,
            samplingRateHertz = 44_100,
            bandCount = 16,
            minimumFrequency = 50f,
            maximumFrequency = 10_000f,
        )

        assertEquals(16, result.size)
        assertTrue(result.any { it > 0f })
        assertTrue(result.all { it in 0f..1f })
    }

    @Test
    fun `low frequency bands always sample at least one FFT bin`() {
        val fft = ByteArray(1024) { 20 }

        val result = calculateSpectrum(
            fft = fft,
            samplingRateHertz = 44_100,
            bandCount = 16,
            minimumFrequency = 50f,
            maximumFrequency = 10_000f,
        )

        assertTrue(result.take(5).all { it > 0f })
    }
}
