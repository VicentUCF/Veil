package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.SteamNewsItem
import org.junit.Test
import kotlin.test.assertEquals

class GameFeedPolicyTest {
    @Test
    fun `news are deduplicated filtered and sorted newest first`() {
        val old = news("old", "https://store.steampowered.com/old", 100L)
        val newest = news("new", "https://store.steampowered.com/new", 300L)
        val duplicate = newest.copy(publishedAtMillis = 200L)
        val unsafe = news("unsafe", "http://example.com", 400L)

        assertEquals(
            listOf("new", "old"),
            GameFeedPolicy.mergeNews(listOf(listOf(old, newest), listOf(duplicate, unsafe)))
                .map(SteamNewsItem::id),
        )
    }

    private fun news(id: String, url: String, date: Long) = SteamNewsItem(
        id = id,
        appId = 10,
        gameTitle = "Game",
        title = id,
        url = url,
        publishedAtMillis = date,
    )
}
