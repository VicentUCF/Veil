package dev.vicent.veil.launcher

object GameFeedCachePolicy {
    const val REFRESH_MILLIS = 60 * 60 * 1_000L
    const val STALE_MILLIS = 24 * 60 * 60 * 1_000L

    fun isFresh(fetchedAtMillis: Long?, nowMillis: Long): Boolean =
        fetchedAtMillis != null && nowMillis - fetchedAtMillis < REFRESH_MILLIS

    fun isStale(fetchedAtMillis: Long?, nowMillis: Long): Boolean =
        fetchedAtMillis != null && nowMillis - fetchedAtMillis > STALE_MILLIS
}
