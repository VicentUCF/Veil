package dev.vicent.veil.launcher

import java.net.URI

object ExternalLinkPolicy {
    fun isSafeHttps(url: String): Boolean = runCatching {
        val uri = URI(url)
        uri.scheme.equals("https", ignoreCase = true) && !uri.host.isNullOrBlank()
    }.getOrDefault(false)
}
