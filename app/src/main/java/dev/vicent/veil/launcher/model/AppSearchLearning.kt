package dev.vicent.veil.launcher.model

data class AppSearchLearningEntry(
    val query: String,
    val packageName: String,
    val selectionCount: Int,
    val lastSelectedAtMillis: Long,
)

data class AppSearchLearningState(
    val entries: List<AppSearchLearningEntry> = emptyList(),
)
