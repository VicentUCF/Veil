package dev.vicent.veil

import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class MainActivityHomeIntentTest {
    @Test
    fun `Home from overview returns to the main launcher surface`() {
        assertFalse(
            shouldHandleAsRepeatedHomePress(
                externalSurfaceLaunched = false,
                hasCompletedFirstResume = true,
                wasStoppedSinceLastResume = true,
            ),
        )
    }

    @Test
    fun `Home while Veil is foreground keeps the repeated press behavior`() {
        assertTrue(
            shouldHandleAsRepeatedHomePress(
                externalSurfaceLaunched = false,
                hasCompletedFirstResume = true,
                wasStoppedSinceLastResume = false,
            ),
        )
    }

    @Test
    fun `first Home delivery after process recreation stays on launcher Home`() {
        assertFalse(
            shouldHandleAsRepeatedHomePress(
                externalSurfaceLaunched = false,
                hasCompletedFirstResume = false,
                wasStoppedSinceLastResume = false,
            ),
        )
    }

    @Test
    fun `Home after opening an external surface returns to launcher Home`() {
        assertFalse(
            shouldHandleAsRepeatedHomePress(
                externalSurfaceLaunched = true,
                hasCompletedFirstResume = true,
                wasStoppedSinceLastResume = false,
            ),
        )
    }
}
