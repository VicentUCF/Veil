package dev.vicent.veil.launcher.model

data class QuickNote(
    val id: Long,
    val title: String,
    val type: QuickNoteType = QuickNoteType.TEXT,
    val body: String = "",
    val checklist: List<QuickNoteChecklistItem> = emptyList(),
)

enum class QuickNoteType { TEXT, CHECKLIST }

data class QuickNoteChecklistItem(
    val id: Long,
    val text: String,
    val checked: Boolean = false,
)
