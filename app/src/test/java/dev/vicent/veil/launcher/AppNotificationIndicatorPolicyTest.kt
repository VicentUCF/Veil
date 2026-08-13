package dev.vicent.veil.launcher

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AppNotificationIndicatorPolicyTest {
    @Test
    fun `badgeable message notification is admitted without reading content`() {
        assertTrue(
            AppNotificationIndicatorPolicy.shouldShow(candidate(category = "msg")),
        )
    }

    @Test
    fun `channel badge preference is respected`() {
        assertFalse(
            AppNotificationIndicatorPolicy.shouldShow(candidate(canShowBadge = false)),
        )
    }

    @Test
    fun `own ongoing service media navigation and progress notifications are excluded`() {
        val excluded = listOf(
            candidate(packageName = "dev.vicent.veil"),
            candidate(isOngoing = true),
            candidate(isForegroundService = true),
            candidate(category = "service"),
            candidate(category = "transport"),
            candidate(category = "navigation"),
            candidate(category = "progress"),
            candidate(progressMax = 100),
        )

        excluded.forEach { item ->
            assertFalse(AppNotificationIndicatorPolicy.shouldShow(item), item.toString())
        }
    }

    @Test
    fun `pre Android O fallback requires a clearable notification`() {
        assertTrue(
            AppNotificationIndicatorPolicy.shouldShow(
                candidate(canShowBadge = null, isClearable = true),
            ),
        )
        assertFalse(
            AppNotificationIndicatorPolicy.shouldShow(
                candidate(canShowBadge = null, isClearable = false),
            ),
        )
    }

    @Test
    fun `tracker deduplicates packages and keeps dot until last notification is removed`() {
        val tracker = AppNotificationIndicatorTracker()
        tracker.replace(
            listOf(
                "whatsapp-1" to "com.whatsapp",
                "whatsapp-2" to "com.whatsapp",
                "mail-1" to "com.example.mail",
            ),
        )

        assertEquals(setOf("com.whatsapp", "com.example.mail"), tracker.packages())

        tracker.remove("whatsapp-1")
        assertTrue("com.whatsapp" in tracker.packages())

        tracker.remove("whatsapp-2")
        assertFalse("com.whatsapp" in tracker.packages())
        assertEquals(setOf("com.example.mail"), tracker.packages())
    }

    @Test
    fun `tracker updates ranking result and clears on disconnect`() {
        val tracker = AppNotificationIndicatorTracker()
        tracker.update("message", "com.whatsapp")
        assertEquals(setOf("com.whatsapp"), tracker.packages())

        tracker.update("message", null)
        assertEquals(emptySet(), tracker.packages())

        tracker.update("mail", "com.example.mail")
        tracker.clear()
        assertEquals(emptySet(), tracker.packages())
    }

    private fun candidate(
        packageName: String = "com.whatsapp",
        category: String? = "msg",
        progressMax: Int = 0,
        isClearable: Boolean = true,
        isOngoing: Boolean = false,
        isForegroundService: Boolean = false,
        canShowBadge: Boolean? = true,
    ) = AppNotificationIndicatorCandidate(
        packageName = packageName,
        ownPackageName = "dev.vicent.veil",
        category = category,
        progressMax = progressMax,
        isClearable = isClearable,
        isOngoing = isOngoing,
        isForegroundService = isForegroundService,
        canShowBadge = canShowBadge,
    )
}
