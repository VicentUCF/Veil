package dev.vicent.veil.launcher

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class NotificationContinuityPolicyTest {
    @Test
    fun `private notification categories are always excluded`() {
        listOf("call", "msg", "alarm", "email", "social").forEach { category ->
            assertNull(NotificationContinuityPolicy.classify(category, progressMax = 100))
        }
    }

    @Test
    fun `only navigation and progress signals are admitted`() {
        assertEquals(
            NotificationContinuityKind.NAVIGATION,
            NotificationContinuityPolicy.classify("navigation", progressMax = 0),
        )
        assertEquals(
            NotificationContinuityKind.PROGRESS,
            NotificationContinuityPolicy.classify("progress", progressMax = 0),
        )
        assertEquals(
            NotificationContinuityKind.PROGRESS,
            NotificationContinuityPolicy.classify("service", progressMax = 100),
        )
        assertNull(NotificationContinuityPolicy.classify("service", progressMax = 0))
    }
}
