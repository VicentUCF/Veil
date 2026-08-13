package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.ContinuityItem

object ContinuityRanker {
    fun selectCurrent(
        items: List<ContinuityItem>,
        nowMillis: Long,
    ): ContinuityItem? = active(items, nowMillis).minWithOrNull(
        compareBy<ContinuityItem> { priority(it) },
    )

    fun selectMedia(
        items: List<ContinuityItem>,
        nowMillis: Long,
    ): ContinuityItem.Media? = active(items, nowMillis)
        .filterIsInstance<ContinuityItem.Media>()
        .minWithOrNull(
            compareBy<ContinuityItem.Media> { if (it.isPlaying) 0 else 1 },
        )

    private fun active(items: List<ContinuityItem>, nowMillis: Long) = items.filter { item ->
        val expiresAt = item.expiresAtMillis
        expiresAt == null || expiresAt > nowMillis
    }

    private fun priority(item: ContinuityItem): Int = when (item) {
        is ContinuityItem.Navigation -> 0
        is ContinuityItem.Media -> if (item.isPlaying) 1 else 3
        is ContinuityItem.Progress -> if (item.isComplete) 4 else 2
    }
}
