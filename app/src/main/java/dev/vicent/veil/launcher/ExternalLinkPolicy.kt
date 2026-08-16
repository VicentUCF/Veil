package dev.vicent.veil.launcher

import java.net.URI

object ExternalLinkPolicy {
    fun isSafeHttps(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.isAbsolute &&
            uri.scheme.equals("https", ignoreCase = true) &&
            !uri.host.isNullOrBlank() &&
            uri.rawUserInfo == null &&
            uri.port in setOf(-1, 443) &&
            !uri.host.equals("localhost", ignoreCase = true)
    }.getOrDefault(false)

    fun isAllowedSteamApi(url: String): Boolean = hasAllowedHost(
        url,
        setOf("api.steampowered.com", "store.steampowered.com"),
    )

    fun isAllowedSteamArtwork(url: String): Boolean = hasAllowedDomain(
        url,
        setOf("steamstatic.com", "steamcdn-a.akamaihd.net"),
    )

    fun isAllowedSteamBrowserLink(url: String): Boolean = hasAllowedDomain(
        url,
        setOf("steampowered.com", "steamcommunity.com"),
    )

    private fun hasAllowedHost(url: String, allowedHosts: Set<String>): Boolean =
        normalizedHost(url)?.let(allowedHosts::contains) == true

    private fun hasAllowedDomain(url: String, allowedDomains: Set<String>): Boolean {
        val host = normalizedHost(url) ?: return false
        return allowedDomains.any { domain -> host == domain || host.endsWith(".$domain") }
    }

    private fun normalizedHost(url: String): String? {
        if (!isSafeHttps(url)) return null
        return runCatching { URI(url).host.lowercase() }.getOrNull()
    }
}
