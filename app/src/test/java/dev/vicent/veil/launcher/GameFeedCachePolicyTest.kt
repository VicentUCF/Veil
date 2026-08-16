package dev.vicent.veil.launcher

import org.junit.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class GameFeedCachePolicyTest {
    @Test
    fun `cache is fresh for less than one hour`() {
        val now = 2_000_000_000L
        assertTrue(GameFeedCachePolicy.isFresh(now - GameFeedCachePolicy.REFRESH_MILLIS + 1, now))
        assertFalse(GameFeedCachePolicy.isFresh(now - GameFeedCachePolicy.REFRESH_MILLIS, now))
        assertFalse(GameFeedCachePolicy.isFresh(null, now))
    }

    @Test
    fun `cache becomes stale only after twenty four hours`() {
        val now = 2_000_000_000L
        assertFalse(GameFeedCachePolicy.isStale(now - GameFeedCachePolicy.STALE_MILLIS, now))
        assertTrue(GameFeedCachePolicy.isStale(now - GameFeedCachePolicy.STALE_MILLIS - 1, now))
        assertFalse(GameFeedCachePolicy.isStale(null, now))
    }
}
