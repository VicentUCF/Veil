package dev.vicent.veil.launcher

object GameFeedCachePolicy {
    const val RefreshMillis = 60 * 60 * 1_000L
    const val StaleMillis = 24 * 60 * 60 * 1_000L

    fun isFresh(fetchedAtMillis: Long?, nowMillis: Long): Boolean =
        fetchedAtMillis != null && nowMillis - fetchedAtMillis < RefreshMillis

    fun isStale(fetchedAtMillis: Long?, nowMillis: Long): Boolean =
        fetchedAtMillis != null && nowMillis - fetchedAtMillis > StaleMillis
}
