package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.ContinuityItem
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ContinuityRankerTest {
    @Test
    fun `work progress only accepts packages assigned to work`() {
        val work = progress("work", "com.work", 20L)
        val personal = progress("personal", "com.personal", 30L)

        assertEquals(
            work,
            ContinuityRanker.selectWorkProgress(
                listOf(personal, work),
                setOf("com.work"),
                nowMillis = 40L,
            ),
        )
    }

    @Test
    fun `current prioritizes navigation then playing media then progress`() {
        val items = listOf(
            progress("progress", updatedAt = 300),
            media("media", playing = true, updatedAt = 200),
            navigation("navigation", updatedAt = 100),
        )

        assertEquals("navigation", ContinuityRanker.selectCurrent(items, nowMillis = 500)?.id)
        assertEquals(
            "media",
            ContinuityRanker.selectCurrent(items.filterNot { it.id == "navigation" }, 500)?.id,
        )
    }

    @Test
    fun `paused media wins over completed progress and system order wins ties`() {
        val items = listOf(
            media("old", playing = false, updatedAt = 100),
            media("new", playing = false, updatedAt = 200),
            progress("complete", updatedAt = 400, complete = true),
        )

        assertEquals("old", ContinuityRanker.selectCurrent(items, nowMillis = 500)?.id)
        assertEquals("old", ContinuityRanker.selectMedia(items, nowMillis = 500)?.id)
    }

    @Test
    fun `expired items never become current`() {
        val item = media("expired", playing = false, updatedAt = 100, expiresAt = 499)

        assertNull(ContinuityRanker.selectCurrent(listOf(item), nowMillis = 500))
    }

    private fun media(
        id: String,
        playing: Boolean,
        updatedAt: Long,
        expiresAt: Long? = null,
    ) = ContinuityItem.Media(
        id = id,
        packageName = "media.app",
        appLabel = "Media",
        title = id,
        subtitle = null,
        updatedAtMillis = updatedAt,
        expiresAtMillis = expiresAt,
        supportedActions = setOf(ContinuityAction.OPEN),
        isPlaying = playing,
        isVideo = false,
    )

    private fun navigation(id: String, updatedAt: Long) = ContinuityItem.Navigation(
        id = id,
        packageName = "maps.app",
        appLabel = "Maps",
        title = id,
        subtitle = null,
        updatedAtMillis = updatedAt,
        expiresAtMillis = null,
        supportedActions = setOf(ContinuityAction.OPEN),
    )

    private fun progress(
        id: String,
        packageName: String = "download.app",
        updatedAt: Long,
        complete: Boolean = false,
    ) =
        ContinuityItem.Progress(
            id = id,
            packageName = packageName,
            appLabel = "Downloads",
            title = id,
            subtitle = null,
            updatedAtMillis = updatedAt,
            expiresAtMillis = null,
            supportedActions = setOf(ContinuityAction.OPEN),
            progress = .5f,
            isComplete = complete,
        )
}
