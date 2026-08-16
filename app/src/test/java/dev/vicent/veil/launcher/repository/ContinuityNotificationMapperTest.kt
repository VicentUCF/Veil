package dev.vicent.veil.launcher.repository

import dev.vicent.veil.launcher.model.ContinuityItem
import dev.vicent.veil.launcher.system.ActiveNotification
import kotlin.test.assertEquals
import kotlin.test.assertNull
import org.junit.Test

class ContinuityNotificationMapperTest {
    @Test
    fun `completed progress receives a bounded lifetime`() {
        val postedAt = 1_000L

        val result = ContinuityNotificationMapper.toContinuityItem(
            notification(
                kind = ActiveNotification.Kind.PROGRESS,
                isComplete = true,
                postedAtMillis = postedAt,
            ),
        ) as ContinuityItem.Progress

        assertEquals(postedAt + 10 * 60 * 1_000L, result.expiresAtMillis)
        assertEquals(true, result.isComplete)
    }

    @Test
    fun `active progress remains unbounded`() {
        val result = ContinuityNotificationMapper.toContinuityItem(
            notification(
                kind = ActiveNotification.Kind.PROGRESS,
                isComplete = false,
                postedAtMillis = 1_000L,
            ),
        ) as ContinuityItem.Progress

        assertNull(result.expiresAtMillis)
        assertEquals(emptySet(), result.supportedActions)
    }

    private fun notification(
        kind: ActiveNotification.Kind,
        isComplete: Boolean,
        postedAtMillis: Long,
    ) = ActiveNotification(
        id = "notification:test",
        packageName = "dev.example",
        appLabel = "Example",
        title = "Task",
        text = "Running",
        postedAtMillis = postedAtMillis,
        kind = kind,
        progress = .5f,
        isComplete = isComplete,
        contentIntent = null,
    )
}
