package dev.vicent.veil.launcher.repository

import android.content.Context
import dev.vicent.veil.R
import dev.vicent.veil.launcher.GameFeedCachePolicy
import dev.vicent.veil.launcher.GameFeedPolicy
import dev.vicent.veil.launcher.SystemTimeProvider
import dev.vicent.veil.launcher.TimeProvider
import dev.vicent.veil.launcher.model.GameFeedAvailability
import dev.vicent.veil.launcher.model.GameFeedState
import dev.vicent.veil.launcher.model.SteamChartEntry
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

class SteamGameRepository(
    context: Context,
    private val timeProvider: TimeProvider = SystemTimeProvider,
) {
    private val applicationContext = context.applicationContext
    private val cache = SteamGameCache(applicationContext, timeProvider)
    private val remote = SteamRemoteClient()
    private val refreshMutex = Mutex()
    private val mutableState = MutableStateFlow(cache.load())
    val state: StateFlow<GameFeedState> = mutableState.asStateFlow()

    suspend fun refresh(force: Boolean = false) = refreshMutex.withLock {
        val now = timeProvider.currentTimeMillis()
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
            cache.save(result)
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
            val ranks = SteamResponseParser.parseChart(remote.fetchText(CHART_ENDPOINT))
                .take(MAX_CHART_ENTRIES)
            check(ranks.isNotEmpty())

            val metadata = ranks.map { rank ->
                async { runCatching { remote.fetchMetadata(rank.appId) }.getOrNull() }
            }.awaitAll()
            val chart = ranks.mapIndexed { index, rank ->
                val appMetadata = metadata[index]
                SteamChartEntry(
                    appId = rank.appId,
                    rank = rank.rank,
                    previousRank = rank.previousRank,
                    peakPlayers = rank.peakPlayers,
                    title = appMetadata?.title
                        ?: applicationContext.getString(R.string.steam_unknown_app, rank.appId),
                    artworkUrl = appMetadata?.artworkUrl,
                    storeUrl = "$STORE_APP_BASE${rank.appId}/",
                )
            }

            val newsGroups = chart.take(NEWS_GAME_COUNT).map { entry ->
                async {
                    runCatching {
                        SteamResponseParser.parseNews(
                            json = remote.fetchText(
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
                runCatching { remote.fetchBitmap(url) }.getOrNull()
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


    private companion object {
        private const val CHART_ENDPOINT =
            "https://api.steampowered.com/ISteamChartsService/GetMostPlayedGames/v1/"
        private const val NEWS_ENDPOINT =
            "https://api.steampowered.com/ISteamNews/GetNewsForApp/v2/"
        private const val STORE_APP_BASE = "https://store.steampowered.com/app/"
        private const val MAX_CHART_ENTRIES = 5
        private const val NEWS_GAME_COUNT = 3
        private const val NEWS_PER_GAME = 2
        private const val MAX_NEWS_ENTRIES = 5
    }
}
