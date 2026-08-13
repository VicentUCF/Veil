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
    }
}
