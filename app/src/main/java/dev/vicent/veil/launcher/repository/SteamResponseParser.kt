package dev.vicent.veil.launcher.repository

import dev.vicent.veil.launcher.ExternalLinkPolicy
import dev.vicent.veil.launcher.model.SteamNewsItem
import org.json.JSONObject

internal object SteamResponseParser {
    data class RawChartEntry(
        val appId: Int,
        val rank: Int,
        val previousRank: Int?,
        val peakPlayers: Int?,
    )

    fun parseChart(json: String): List<RawChartEntry> {
        val ranks = JSONObject(json).getJSONObject("response").getJSONArray("ranks")
        return buildList {
            repeat(ranks.length()) { index ->
                val item = ranks.optJSONObject(index) ?: return@repeat
                val appId = item.optInt("appid", -1)
                val rank = item.optInt("rank", -1)
                if (appId <= 0 || rank <= 0) return@repeat
                add(
                    RawChartEntry(
                        appId = appId,
                        rank = rank,
                        previousRank = item.optInt("last_week_rank", -1).takeIf { it > 0 },
                        peakPlayers = item.optInt("peak_in_game", -1).takeIf { it >= 0 },
                    ),
                )
            }
        }
    }

    fun parseNews(json: String, gameTitle: String): List<SteamNewsItem> {
        val root = JSONObject(json).getJSONObject("appnews")
        val appId = root.optInt("appid", -1)
        val items = root.getJSONArray("newsitems")
        return buildList {
            repeat(items.length()) { index ->
                val item = items.optJSONObject(index) ?: return@repeat
                val id = item.optString("gid").trim().take(MAX_ID_CHARS)
                val title = item.optString("title").trim().take(MAX_TITLE_CHARS)
                val url = item.optString("url").trim()
                if (id.isBlank() || title.isBlank() ||
                    !ExternalLinkPolicy.isAllowedSteamBrowserLink(url)
                ) return@repeat
                add(
                    SteamNewsItem(
                        id = id,
                        appId = appId,
                        gameTitle = gameTitle,
                        title = title,
                        url = url,
                        publishedAtMillis = item.optLong("date", 0L) * 1_000L,
                    ),
                )
            }
        }
    }

    private const val MAX_ID_CHARS = 128
    private const val MAX_TITLE_CHARS = 200
}
