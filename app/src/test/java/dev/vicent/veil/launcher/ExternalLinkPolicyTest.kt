package dev.vicent.veil.launcher

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExternalLinkPolicyTest {
    @Test
    fun `only absolute https links are accepted`() {
        assertTrue(ExternalLinkPolicy.isSafeHttps("https://store.steampowered.com/app/730/"))
        assertFalse(ExternalLinkPolicy.isSafeHttps("http://store.steampowered.com/app/730/"))
        assertFalse(ExternalLinkPolicy.isSafeHttps("javascript:alert(1)"))
        assertFalse(ExternalLinkPolicy.isSafeHttps("/relative/path"))
        assertFalse(ExternalLinkPolicy.isSafeHttps("https://user@example.com/private"))
        assertFalse(ExternalLinkPolicy.isSafeHttps("https://localhost/private"))
        assertFalse(ExternalLinkPolicy.isSafeHttps("https://example.com:8443/private"))
    }

    @Test
    fun `Steam destinations use separate exact allowlists`() {
        assertTrue(ExternalLinkPolicy.isAllowedSteamApi("https://api.steampowered.com/v1/"))
        assertTrue(ExternalLinkPolicy.isAllowedSteamArtwork("https://shared.fastly.steamstatic.com/a.jpg"))
        assertTrue(ExternalLinkPolicy.isAllowedSteamBrowserLink("https://steamcommunity.com/games/730/announcements/detail/1"))
        assertFalse(ExternalLinkPolicy.isAllowedSteamApi("https://api.steampowered.com.evil.test/v1/"))
        assertFalse(ExternalLinkPolicy.isAllowedSteamArtwork("https://example.com/a.jpg"))
        assertFalse(ExternalLinkPolicy.isAllowedSteamBrowserLink("https://example.com/news"))
    }
}
