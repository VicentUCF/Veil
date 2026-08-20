package dev.vicent.veil.launcher.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.content.edit
import dev.vicent.veil.launcher.ExternalLinkPolicy
import dev.vicent.veil.launcher.GameFeedCachePolicy
import dev.vicent.veil.launcher.SystemTimeProvider
import dev.vicent.veil.launcher.TimeProvider
import dev.vicent.veil.launcher.model.GameFeedAvailability
import dev.vicent.veil.launcher.model.GameFeedState
import dev.vicent.veil.launcher.model.SteamChartEntry
import dev.vicent.veil.launcher.model.SteamNewsItem
import java.io.File

internal class SteamGameCache(
    context: Context,
    private val timeProvider: TimeProvider = SystemTimeProvider,
) {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val artworkFile = File(context.cacheDir, ARTWORK_FILE)

    fun save(value: GameFeedState) {
        val artworkAppId = value.heroArtwork?.let { artwork ->
            runCatching {
                artworkFile.outputStream().use { output ->
                    check(artwork.compress(Bitmap.CompressFormat.JPEG, 86, output))
                }
                value.chart.firstOrNull()?.appId ?: -1
            }.getOrDefault(-1)
        } ?: run {
            runCatching { artworkFile.delete() }
            -1
        }
        preferences.edit {
            clear()
            putLong(KEY_FETCHED_AT, value.fetchedAtMillis ?: 0L)
            putInt(KEY_CHART_COUNT, value.chart.size)
            value.chart.forEachIndexed { index, entry ->
                putInt("chart_${index}_appid", entry.appId)
                putInt("chart_${index}_rank", entry.rank)
                putInt("chart_${index}_previous", entry.previousRank ?: -1)
                putInt("chart_${index}_peak", entry.peakPlayers ?: -1)
                putString("chart_${index}_title", entry.title)
                putString("chart_${index}_artwork", entry.artworkUrl)
                putString("chart_${index}_store", entry.storeUrl)
            }
            putInt(KEY_NEWS_COUNT, value.news.size)
            value.news.forEachIndexed { index, item ->
                putString("news_${index}_id", item.id)
                putInt("news_${index}_appid", item.appId)
                putString("news_${index}_game", item.gameTitle)
                putString("news_${index}_title", item.title)
                putString("news_${index}_url", item.url)
                putLong("news_${index}_date", item.publishedAtMillis)
            }
            putInt(KEY_ARTWORK_APP_ID, artworkAppId)
        }
    }

    fun load(): GameFeedState {
        val fetchedAt = preferences.getLong(KEY_FETCHED_AT, 0L)
        if (fetchedAt <= 0L) return GameFeedState()
        val chart = List(
            preferences.getInt(KEY_CHART_COUNT, 0).coerceIn(0, MAX_CHART_ENTRIES),
        ) { index ->
            val appId = preferences.getInt("chart_${index}_appid", -1)
            val title = preferences.getString("chart_${index}_title", null).orEmpty()
            val storeUrl = preferences.getString("chart_${index}_store", null).orEmpty()
            if (appId <= 0 || title.isBlank() ||
                !ExternalLinkPolicy.isAllowedSteamBrowserLink(storeUrl)
            ) return@List null
            SteamChartEntry(
                appId = appId,
                rank = preferences.getInt("chart_${index}_rank", index + 1),
                previousRank = preferences.getInt("chart_${index}_previous", -1).takeIf { it > 0 },
                peakPlayers = preferences.getInt("chart_${index}_peak", -1).takeIf { it >= 0 },
                title = title,
                artworkUrl = preferences.getString("chart_${index}_artwork", null)
                    ?.takeIf(ExternalLinkPolicy::isAllowedSteamArtwork),
                storeUrl = storeUrl,
            )
        }.filterNotNull()
        if (chart.isEmpty()) return GameFeedState()

        val news = List(
            preferences.getInt(KEY_NEWS_COUNT, 0).coerceIn(0, MAX_NEWS_ENTRIES),
        ) { index ->
            val id = preferences.getString("news_${index}_id", null).orEmpty()
            val title = preferences.getString("news_${index}_title", null).orEmpty()
            val url = preferences.getString("news_${index}_url", null).orEmpty()
            if (id.isBlank() || title.isBlank() ||
                !ExternalLinkPolicy.isAllowedSteamBrowserLink(url)
            ) return@List null
            SteamNewsItem(
                id = id,
                appId = preferences.getInt("news_${index}_appid", -1),
                gameTitle = preferences.getString("news_${index}_game", null).orEmpty(),
                title = title,
                url = url,
                publishedAtMillis = preferences.getLong("news_${index}_date", 0L),
            )
        }.filterNotNull()
        val artwork = if (
            preferences.getInt(KEY_ARTWORK_APP_ID, -1) == chart.first().appId && artworkFile.isFile
        ) {
            runCatching { BitmapFactory.decodeFile(artworkFile.absolutePath) }.getOrNull()
        } else null
        val now = timeProvider.currentTimeMillis()
        return GameFeedState(
            availability = GameFeedAvailability.AVAILABLE,
            chart = chart,
            news = news,
            heroArtwork = artwork,
            fetchedAtMillis = fetchedAt,
            isStale = GameFeedCachePolicy.isStale(fetchedAt, now),
        )
    }

    private companion object {
        const val PREFERENCES = "veil_game_feed"
        const val ARTWORK_FILE = "veil_game_hero.jpg"
        const val KEY_FETCHED_AT = "fetched_at"
        const val KEY_CHART_COUNT = "chart_count"
        const val KEY_NEWS_COUNT = "news_count"
        const val KEY_ARTWORK_APP_ID = "artwork_app_id"
        const val MAX_CHART_ENTRIES = 5
        const val MAX_NEWS_ENTRIES = 5
    }
}
