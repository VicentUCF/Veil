package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.SteamNewsItem

object GameFeedPolicy {
    fun mergeNews(groups: List<List<SteamNewsItem>>, limit: Int = 5): List<SteamNewsItem> =
        groups.asSequence()
            .flatten()
            .filter { ExternalLinkPolicy.isAllowedSteamBrowserLink(it.url) }
            .distinctBy(SteamNewsItem::id)
            .sortedByDescending(SteamNewsItem::publishedAtMillis)
            .take(limit.coerceAtLeast(0))
            .toList()
}
