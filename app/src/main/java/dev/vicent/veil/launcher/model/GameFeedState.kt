package dev.vicent.veil.launcher.model

import android.graphics.Bitmap

enum class GameFeedAvailability { IDLE, LOADING, AVAILABLE, UNAVAILABLE }

data class SteamChartEntry(
    val appId: Int,
    val rank: Int,
    val previousRank: Int? = null,
    val peakPlayers: Int? = null,
    val title: String,
    val artworkUrl: String? = null,
    val storeUrl: String,
)

data class SteamNewsItem(
    val id: String,
    val appId: Int,
    val gameTitle: String,
    val title: String,
    val url: String,
    val publishedAtMillis: Long,
)

data class GameFeedState(
    val availability: GameFeedAvailability = GameFeedAvailability.IDLE,
    val chart: List<SteamChartEntry> = emptyList(),
    val news: List<SteamNewsItem> = emptyList(),
    val heroArtwork: Bitmap? = null,
    val fetchedAtMillis: Long? = null,
    val isStale: Boolean = false,
)
