package dev.vicent.veil.launcher.repository

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dev.vicent.veil.launcher.ExternalLinkPolicy
import dev.vicent.veil.launcher.GameFeedCachePolicy
import dev.vicent.veil.launcher.GameFeedPolicy
import dev.vicent.veil.launcher.model.GameFeedAvailability
import dev.vicent.veil.launcher.model.GameFeedState
import dev.vicent.veil.launcher.model.SteamChartEntry
import dev.vicent.veil.launcher.model.SteamNewsItem
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.json.JSONObject

class SteamGameRepository(context: Context) {
    private val appContext = context.applicationContext
    private val preferences = appContext.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)
    private val artworkFile = File(appContext.cacheDir, ARTWORK_FILE)
    private val refreshMutex = Mutex()
    private val mutableState = MutableStateFlow(loadCached())
    val state: StateFlow<GameFeedState> = mutableState.asStateFlow()

    suspend fun refresh(force: Boolean = false) = refreshMutex.withLock {
        val now = System.currentTimeMillis()
        val current = mutableState.value
        if (!force && current.chart.isNotEmpty() && GameFeedCachePolicy.isFresh(current.fetchedAtMillis, now)) {
            mutableState.value = current.copy(
                availability = GameFeedAvailability.AVAILABLE,
                isStale = GameFeedCachePolicy.isStale(current.fetchedAtMillis, now),
            )
            return@withLock
        }

        mutableState.value = if (current.chart.isEmpty()) {
            current.copy(availability = GameFeedAvailability.LOADING)
        } else {
            current.copy(isStale = GameFeedCachePolicy.isStale(current.fetchedAtMillis, now))
        }

        val result = runCatching { fetchFeed(now) }.getOrNull()
        if (result != null && result.chart.isNotEmpty()) {
            save(result)
            mutableState.value = result
        } else {
            mutableState.value = current.copy(
                availability = if (current.chart.isEmpty()) {
                    GameFeedAvailability.UNAVAILABLE
                } else {
                    GameFeedAvailability.AVAILABLE
                },
                isStale = current.chart.isNotEmpty() &&
                    GameFeedCachePolicy.isStale(current.fetchedAtMillis, now),
            )
        }
    }

    private suspend fun fetchFeed(fetchedAtMillis: Long): GameFeedState = withContext(Dispatchers.IO) {
        coroutineScope {
            val ranks = parseChartResponse(fetchText(CHART_ENDPOINT)).take(MAX_CHART_ENTRIES)
            check(ranks.isNotEmpty())

            val metadata = ranks.map { rank ->
                async { runCatching { fetchMetadata(rank.appId) }.getOrNull() }
            }.awaitAll()
            val chart = ranks.mapIndexed { index, rank ->
                val appMetadata = metadata[index]
                SteamChartEntry(
                    appId = rank.appId,
                    rank = rank.rank,
                    previousRank = rank.previousRank,
                    peakPlayers = rank.peakPlayers,
                    title = appMetadata?.title ?: "Steam App ${rank.appId}",
                    artworkUrl = appMetadata?.artworkUrl,
                    storeUrl = "$STORE_APP_BASE${rank.appId}/",
                )
            }

            val newsGroups = chart.take(NEWS_GAME_COUNT).map { entry ->
                async {
                    runCatching {
                        parseNewsResponse(
                            json = fetchText(
                                "$NEWS_ENDPOINT?appid=${entry.appId}&count=$NEWS_PER_GAME" +
                                    "&maxlength=1&format=json",
                            ),
                            gameTitle = entry.title,
                        )
                    }.getOrDefault(emptyList())
                }
            }.awaitAll()
            val news = GameFeedPolicy.mergeNews(newsGroups, MAX_NEWS_ENTRIES)

            val artwork = chart.firstOrNull()?.artworkUrl?.let { url ->
                runCatching { fetchBitmap(url) }.getOrNull()
            }
            GameFeedState(
                availability = GameFeedAvailability.AVAILABLE,
                chart = chart,
                news = news,
                heroArtwork = artwork,
                fetchedAtMillis = fetchedAtMillis,
                isStale = false,
            )
        }
    }

    private fun fetchMetadata(appId: Int): SteamMetadata? {
        val json = fetchText("$APP_DETAILS_ENDPOINT?appids=$appId&cc=ES&l=spanish")
        val root = JSONObject(json).optJSONObject(appId.toString()) ?: return null
        if (!root.optBoolean("success", false)) return null
        val data = root.optJSONObject("data") ?: return null
        val title = data.optString("name").trim().takeIf(String::isNotEmpty) ?: return null
        val artworkUrl = data.optString("header_image")
            .trim()
            .takeIf(ExternalLinkPolicy::isSafeHttps)
        return SteamMetadata(title, artworkUrl)
    }

    private fun fetchText(url: String): String {
        val connection = open(url)
        return try {
            check(connection.responseCode in 200..299)
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val output = StringBuilder()
                val buffer = CharArray(8_192)
                while (output.length < MAX_TEXT_RESPONSE_CHARS) {
                    val count = reader.read(buffer, 0, minOf(buffer.size, MAX_TEXT_RESPONSE_CHARS - output.length))
                    if (count < 0) break
                    output.append(buffer, 0, count)
                }
                output.toString()
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun fetchBitmap(url: String): Bitmap? {
        if (!ExternalLinkPolicy.isSafeHttps(url)) return null
        val connection = open(url)
        return try {
            check(connection.responseCode in 200..299)
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8_192)
                var total = 0
                while (total < MAX_ARTWORK_BYTES) {
                    val count = input.read(buffer, 0, minOf(buffer.size, MAX_ARTWORK_BYTES - total))
                    if (count < 0) break
                    output.write(buffer, 0, count)
                    total += count
                }
                output.toByteArray()
            }
            decodeBoundedBitmap(bytes)
        } finally {
            connection.disconnect()
        }
    }

    private fun open(url: String): HttpsURLConnection {
        check(ExternalLinkPolicy.isSafeHttps(url))
        return (URL(url).openConnection() as HttpsURLConnection).apply {
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            instanceFollowRedirects = true
            setRequestProperty("User-Agent", "Veil/0.1")
            setRequestProperty("Accept", "application/json,image/*")
        }
    }

    private fun save(value: GameFeedState) {
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
        preferences.edit().apply {
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
        }.apply()
    }

    private fun loadCached(): GameFeedState {
        val fetchedAt = preferences.getLong(KEY_FETCHED_AT, 0L)
        if (fetchedAt <= 0L) return GameFeedState()
        val chart = List(preferences.getInt(KEY_CHART_COUNT, 0).coerceIn(0, MAX_CHART_ENTRIES)) { index ->
            val appId = preferences.getInt("chart_${index}_appid", -1)
            val title = preferences.getString("chart_${index}_title", null).orEmpty()
            val storeUrl = preferences.getString("chart_${index}_store", null).orEmpty()
            if (appId <= 0 || title.isBlank() || !ExternalLinkPolicy.isSafeHttps(storeUrl)) return@List null
            SteamChartEntry(
                appId = appId,
                rank = preferences.getInt("chart_${index}_rank", index + 1),
                previousRank = preferences.getInt("chart_${index}_previous", -1).takeIf { it > 0 },
                peakPlayers = preferences.getInt("chart_${index}_peak", -1).takeIf { it >= 0 },
                title = title,
                artworkUrl = preferences.getString("chart_${index}_artwork", null)
                    ?.takeIf(ExternalLinkPolicy::isSafeHttps),
                storeUrl = storeUrl,
            )
        }.filterNotNull()
        if (chart.isEmpty()) return GameFeedState()

        val news = List(preferences.getInt(KEY_NEWS_COUNT, 0).coerceIn(0, MAX_NEWS_ENTRIES)) { index ->
            val id = preferences.getString("news_${index}_id", null).orEmpty()
            val title = preferences.getString("news_${index}_title", null).orEmpty()
            val url = preferences.getString("news_${index}_url", null).orEmpty()
            if (id.isBlank() || title.isBlank() || !ExternalLinkPolicy.isSafeHttps(url)) return@List null
            SteamNewsItem(
                id = id,
                appId = preferences.getInt("news_${index}_appid", -1),
                gameTitle = preferences.getString("news_${index}_game", null).orEmpty(),
                title = title,
                url = url,
                publishedAtMillis = preferences.getLong("news_${index}_date", 0L),
            )
        }.filterNotNull()
        val now = System.currentTimeMillis()
        val artwork = if (
            preferences.getInt(KEY_ARTWORK_APP_ID, -1) == chart.first().appId && artworkFile.isFile
        ) {
            runCatching { BitmapFactory.decodeFile(artworkFile.absolutePath) }.getOrNull()
        } else {
            null
        }
        return GameFeedState(
            availability = GameFeedAvailability.AVAILABLE,
            chart = chart,
            news = news,
            heroArtwork = artwork,
            fetchedAtMillis = fetchedAt,
            isStale = GameFeedCachePolicy.isStale(fetchedAt, now),
        )
    }

    private data class SteamMetadata(val title: String, val artworkUrl: String?)

    internal data class RawChartEntry(
        val appId: Int,
        val rank: Int,
        val previousRank: Int?,
        val peakPlayers: Int?,
    )

    companion object {
        internal fun parseChartResponse(json: String): List<RawChartEntry> {
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

        internal fun parseNewsResponse(json: String, gameTitle: String): List<SteamNewsItem> {
            val root = JSONObject(json).getJSONObject("appnews")
            val appId = root.optInt("appid", -1)
            val items = root.getJSONArray("newsitems")
            return buildList {
                repeat(items.length()) { index ->
                    val item = items.optJSONObject(index) ?: return@repeat
                    val id = item.optString("gid").trim()
                    val title = item.optString("title").trim()
                    val url = item.optString("url").trim()
                    if (id.isBlank() || title.isBlank() || !ExternalLinkPolicy.isSafeHttps(url)) {
                        return@repeat
                    }
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

        private fun decodeBoundedBitmap(bytes: ByteArray): Bitmap? {
            if (bytes.isEmpty()) return null
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
            var sample = 1
            while (bounds.outWidth / sample > MAX_ARTWORK_WIDTH ||
                bounds.outHeight / sample > MAX_ARTWORK_HEIGHT
            ) {
                sample *= 2
            }
            return BitmapFactory.decodeByteArray(
                bytes,
                0,
                bytes.size,
                BitmapFactory.Options().apply { inSampleSize = sample },
            )
        }

        private const val PREFERENCES = "veil_game_feed"
        private const val ARTWORK_FILE = "veil_game_hero.jpg"
        private const val KEY_FETCHED_AT = "fetched_at"
        private const val KEY_CHART_COUNT = "chart_count"
        private const val KEY_NEWS_COUNT = "news_count"
        private const val KEY_ARTWORK_APP_ID = "artwork_app_id"
        private const val CHART_ENDPOINT =
            "https://api.steampowered.com/ISteamChartsService/GetMostPlayedGames/v1/"
        private const val NEWS_ENDPOINT =
            "https://api.steampowered.com/ISteamNews/GetNewsForApp/v2/"
        private const val APP_DETAILS_ENDPOINT = "https://store.steampowered.com/api/appdetails"
        private const val STORE_APP_BASE = "https://store.steampowered.com/app/"
        private const val MAX_CHART_ENTRIES = 5
        private const val NEWS_GAME_COUNT = 3
        private const val NEWS_PER_GAME = 2
        private const val MAX_NEWS_ENTRIES = 5
        private const val MAX_TEXT_RESPONSE_CHARS = 512 * 1_024
        private const val MAX_ARTWORK_BYTES = 2 * 1_024 * 1_024
        private const val MAX_ARTWORK_WIDTH = 1_024
        private const val MAX_ARTWORK_HEIGHT = 512
        private const val TIMEOUT_MILLIS = 5_000
    }
}
