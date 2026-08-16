package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.QuickNote
import dev.vicent.veil.launcher.model.QuickNoteType

object QuickNotesPolicy {
    const val MAX_NOTES = 3
    const val MAX_TITLE_LENGTH = 60
    const val MAX_BODY_LENGTH = 2_000
    const val MAX_CHECKLIST_ITEMS = 12
    const val MAX_CHECKLIST_ITEM_LENGTH = 120

    fun add(notes: List<QuickNote>, note: QuickNote): List<QuickNote> {
        val sanitized = sanitize(note) ?: return notes
        if (notes.size >= MAX_NOTES || notes.any { it.id == note.id }) return notes
        return notes + sanitized
    }

    fun update(notes: List<QuickNote>, note: QuickNote): List<QuickNote> {
        val sanitized = sanitize(note) ?: return notes
        if (notes.none { it.id == note.id }) return notes
        return notes.map { current -> if (current.id == note.id) sanitized else current }
    }

    fun delete(notes: List<QuickNote>, id: Long): List<QuickNote> =
        notes.filterNot { it.id == id }

    fun sanitize(note: QuickNote): QuickNote? {
        val title = sanitizeTitle(note.title) ?: return null
        val usedItemIds = mutableSetOf<Long>()
        val checklist = note.checklist.asSequence()
            .mapNotNull { item ->
                val text = item.text.trim().take(MAX_CHECKLIST_ITEM_LENGTH)
                if (text.isEmpty() || !usedItemIds.add(item.id)) null else item.copy(text = text)
            }
            .take(MAX_CHECKLIST_ITEMS)
            .toList()
        return note.copy(
            title = title,
            body = if (note.type == QuickNoteType.TEXT) {
                note.body.trim().take(MAX_BODY_LENGTH)
            } else "",
            checklist = if (note.type == QuickNoteType.CHECKLIST) checklist else emptyList(),
        )
    }

    fun sanitizeTitle(title: String): String? = title
        .trim()
        .take(MAX_TITLE_LENGTH)
        .takeIf(String::isNotEmpty)
}
