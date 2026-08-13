package dev.vicent.veil.launcher.repository

import android.content.Context
import dev.vicent.veil.launcher.QuickNotesPolicy
import dev.vicent.veil.launcher.model.QuickNote
import dev.vicent.veil.launcher.model.QuickNoteChecklistItem
import dev.vicent.veil.launcher.model.QuickNoteType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class QuickNotesRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PreferencesName,
        Context.MODE_PRIVATE,
    )
    private val mutableNotes = MutableStateFlow(readNotes())
    val notes: StateFlow<List<QuickNote>> = mutableNotes.asStateFlow()

    @Synchronized
    fun add(
        title: String,
        type: QuickNoteType,
        body: String,
        checklist: List<QuickNoteChecklistItem>,
    ) {
        val current = mutableNotes.value
        val nextId = preferences.getLong(KeyNextId, defaultNextId(current))
            .coerceAtLeast(defaultNextId(current))
        val updated = QuickNotesPolicy.add(
            current,
            QuickNote(id = nextId, title = title, type = type, body = body, checklist = checklist),
        )
        if (updated === current) return
        writeNotes(updated, nextId + 1)
    }

    @Synchronized
    fun update(
        id: Long,
        title: String,
        type: QuickNoteType,
        body: String,
        checklist: List<QuickNoteChecklistItem>,
    ) {
        val current = mutableNotes.value
        val updated = QuickNotesPolicy.update(
            current,
            QuickNote(id = id, title = title, type = type, body = body, checklist = checklist),
        )
        if (updated == current) return
        writeNotes(updated, preferences.getLong(KeyNextId, defaultNextId(updated)))
    }

    @Synchronized
    fun delete(id: Long) {
        val current = mutableNotes.value
        val updated = QuickNotesPolicy.delete(current, id)
        if (updated.size == current.size) return
        writeNotes(updated, preferences.getLong(KeyNextId, defaultNextId(updated)))
    }

    private fun readNotes(): List<QuickNote> {
        val count = preferences.getInt(KeyCount, 0).coerceIn(0, QuickNotesPolicy.MaxNotes)
        val usedIds = mutableSetOf<Long>()
        return buildList {
            repeat(count) { index ->
                val id = preferences.getLong("$KeyIdPrefix$index", index + 1L)
                val title = preferences.getString("$KeyTitlePrefix$index", null)
                    ?: preferences.getString("$LegacyTextPrefix$index", null)
                    ?: return@repeat
                val checklistCount = preferences.getInt(
                    "$KeyChecklistCountPrefix$index",
                    0,
                ).coerceIn(0, QuickNotesPolicy.MaxChecklistItems)
                val checklist = buildList {
                    repeat(checklistCount) { itemIndex ->
                        val itemText = preferences.getString(
                            "$KeyChecklistTextPrefix${index}_$itemIndex",
                            null,
                        ).orEmpty()
                        if (itemText.isBlank()) return@repeat
                        add(
                            QuickNoteChecklistItem(
                                id = preferences.getLong(
                                    "$KeyChecklistIdPrefix${index}_$itemIndex",
                                    itemIndex + 1L,
                                ),
                                text = itemText,
                                checked = preferences.getBoolean(
                                    "$KeyChecklistCheckedPrefix${index}_$itemIndex",
                                    false,
                                ),
                            ),
                        )
                    }
                }
                val note = QuickNotesPolicy.sanitize(
                    QuickNote(
                        id = id,
                        title = title,
                        type = preferences.getString("$KeyTypePrefix$index", null)
                            ?.let { stored ->
                                runCatching { QuickNoteType.valueOf(stored) }.getOrNull()
                            }
                            ?: if (checklist.isNotEmpty()) {
                                QuickNoteType.CHECKLIST
                            } else {
                                QuickNoteType.TEXT
                            },
                        body = preferences.getString("$KeyBodyPrefix$index", "").orEmpty(),
                        checklist = checklist,
                    ),
                ) ?: return@repeat
                if (usedIds.add(id)) add(note)
            }
        }
    }

    private fun writeNotes(notes: List<QuickNote>, nextId: Long) {
        preferences.edit().clear().apply {
            putInt(KeyCount, notes.size)
            putLong(KeyNextId, nextId.coerceAtLeast(defaultNextId(notes)))
            notes.forEachIndexed { index, note ->
                putLong("$KeyIdPrefix$index", note.id)
                putString("$KeyTitlePrefix$index", note.title)
                putString("$KeyTypePrefix$index", note.type.name)
                putString("$KeyBodyPrefix$index", note.body)
                putInt("$KeyChecklistCountPrefix$index", note.checklist.size)
                note.checklist.forEachIndexed { itemIndex, item ->
                    putLong("$KeyChecklistIdPrefix${index}_$itemIndex", item.id)
                    putString("$KeyChecklistTextPrefix${index}_$itemIndex", item.text)
                    putBoolean("$KeyChecklistCheckedPrefix${index}_$itemIndex", item.checked)
                }
            }
        }.apply()
        mutableNotes.value = notes
    }

    private fun defaultNextId(notes: List<QuickNote>): Long =
        (notes.maxOfOrNull(QuickNote::id) ?: 0L) + 1L

    companion object {
        const val PreferencesName = "veil_quick_notes"
        private const val KeyCount = "count"
        private const val KeyNextId = "next_id"
        private const val KeyIdPrefix = "id_"
        private const val KeyTitlePrefix = "title_"
        private const val KeyTypePrefix = "type_"
        private const val KeyBodyPrefix = "body_"
        private const val KeyChecklistCountPrefix = "checklist_count_"
        private const val KeyChecklistIdPrefix = "checklist_id_"
        private const val KeyChecklistTextPrefix = "checklist_text_"
        private const val KeyChecklistCheckedPrefix = "checklist_checked_"
        private const val LegacyTextPrefix = "text_"
    }
}
