package dev.vicent.veil.launcher.repository

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import dev.vicent.veil.launcher.ExternalLinkPolicy
import java.io.ByteArrayOutputStream
import java.net.URL
import javax.net.ssl.HttpsURLConnection
import org.json.JSONObject

internal data class SteamMetadata(val title: String, val artworkUrl: String?)

internal class SteamRemoteClient {
    fun fetchMetadata(appId: Int): SteamMetadata? {
        val json = fetchText("$APP_DETAILS_ENDPOINT?appids=$appId")
        val root = JSONObject(json).optJSONObject(appId.toString()) ?: return null
        if (!root.optBoolean("success", false)) return null
        val data = root.optJSONObject("data") ?: return null
        val title = data.optString("name").trim().take(MAX_TITLE_CHARS)
            .takeIf(String::isNotEmpty) ?: return null
        val artworkUrl = data.optString("header_image")
            .trim()
            .takeIf(ExternalLinkPolicy::isAllowedSteamArtwork)
        return SteamMetadata(title, artworkUrl)
    }

    fun fetchText(url: String): String {
        val connection = open(url, ExternalLinkPolicy::isAllowedSteamApi)
        return try {
            check(connection.responseCode in 200..299)
            check(connection.contentType?.substringBefore(';')?.trim() == "application/json")
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                val output = StringBuilder()
                val buffer = CharArray(8_192)
                while (true) {
                    val remaining = MAX_TEXT_RESPONSE_CHARS - output.length
                    check(remaining > 0 || reader.read() == -1) { "Steam response exceeds limit" }
                    if (remaining == 0) break
                    val count = reader.read(buffer, 0, minOf(buffer.size, remaining))
                    if (count < 0) break
                    output.append(buffer, 0, count)
                }
                output.toString()
            }
        } finally {
            connection.disconnect()
        }
    }

    fun fetchBitmap(url: String): Bitmap? {
        if (!ExternalLinkPolicy.isAllowedSteamArtwork(url)) return null
        val connection = open(url, ExternalLinkPolicy::isAllowedSteamArtwork)
        return try {
            check(connection.responseCode in 200..299)
            check(connection.contentType?.substringBefore(';')?.trim()?.startsWith("image/") == true)
            check(connection.contentLength in -1..MAX_ARTWORK_BYTES)
            val bytes = connection.inputStream.use { input ->
                val output = ByteArrayOutputStream()
                val buffer = ByteArray(8_192)
                var total = 0
                while (true) {
                    val remaining = MAX_ARTWORK_BYTES - total
                    check(remaining > 0 || input.read() == -1) { "Steam artwork exceeds limit" }
                    if (remaining == 0) break
                    val count = input.read(buffer, 0, minOf(buffer.size, remaining))
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

    private fun open(url: String, isAllowed: (String) -> Boolean): HttpsURLConnection {
        check(isAllowed(url))
        return (URL(url).openConnection() as HttpsURLConnection).apply {
            connectTimeout = TIMEOUT_MILLIS
            readTimeout = TIMEOUT_MILLIS
            instanceFollowRedirects = false
            setRequestProperty("User-Agent", "Veil/0.1")
            setRequestProperty("Accept", "application/json,image/*")
        }
    }

    private fun decodeBoundedBitmap(bytes: ByteArray): Bitmap? {
        if (bytes.isEmpty()) return null
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        var sample = 1
        while (bounds.outWidth / sample > MAX_ARTWORK_WIDTH ||
            bounds.outHeight / sample > MAX_ARTWORK_HEIGHT
        ) sample *= 2
        return BitmapFactory.decodeByteArray(
            bytes,
            0,
            bytes.size,
            BitmapFactory.Options().apply { inSampleSize = sample },
        )
    }

    private companion object {
        const val APP_DETAILS_ENDPOINT = "https://store.steampowered.com/api/appdetails"
        const val MAX_TITLE_CHARS = 200
        const val MAX_TEXT_RESPONSE_CHARS = 512 * 1_024
        const val MAX_ARTWORK_BYTES = 2 * 1_024 * 1_024
        const val MAX_ARTWORK_WIDTH = 1_024
        const val MAX_ARTWORK_HEIGHT = 512
        const val TIMEOUT_MILLIS = 5_000
    }
}
