package dev.vicent.veil.launcher.repository

import org.junit.Test
import kotlin.test.assertEquals

class SteamGameRepositoryTest {
    @Test
    fun `chart parser keeps valid public ranking fields`() {
        val parsed = SteamGameRepository.parseChartResponse(
            """
            {"response":{"ranks":[
              {"rank":1,"appid":730,"last_week_rank":3,"peak_in_game":1200000},
              {"rank":2,"appid":570,"peak_in_game":800000},
              {"rank":0,"appid":-1}
            ]}}
            """.trimIndent(),
        )

        assertEquals(listOf(730, 570), parsed.map { it.appId })
        assertEquals(3, parsed.first().previousRank)
        assertEquals(null, parsed[1].previousRank)
        assertEquals(1_200_000, parsed.first().peakPlayers)
    }

    @Test
    fun `news parser rejects incomplete and non https entries`() {
        val parsed = SteamGameRepository.parseNewsResponse(
            """
            {"appnews":{"appid":730,"newsitems":[
              {"gid":"one","title":"Update","url":"https://store.steampowered.com/news/1","date":100},
              {"gid":"two","title":"Unsafe","url":"http://example.com","date":200},
              {"gid":"three","title":"","url":"https://example.com","date":300}
            ]}}
            """.trimIndent(),
            gameTitle = "Counter-Strike 2",
        )

        assertEquals(listOf("one"), parsed.map { it.id })
        assertEquals(100_000L, parsed.single().publishedAtMillis)
        assertEquals("Counter-Strike 2", parsed.single().gameTitle)
    }
}
