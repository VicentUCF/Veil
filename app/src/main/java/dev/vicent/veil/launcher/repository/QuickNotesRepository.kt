package dev.vicent.veil.launcher.repository

import android.content.Context
import androidx.core.content.edit
import dev.vicent.veil.launcher.QuickNotesPolicy
import dev.vicent.veil.launcher.model.QuickNote
import dev.vicent.veil.launcher.model.QuickNoteChecklistItem
import dev.vicent.veil.launcher.model.QuickNoteType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class QuickNotesRepository(context: Context) {
    private val preferences = context.applicationContext.getSharedPreferences(
        PREFERENCES_NAME,
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
        val nextId = preferences.getLong(KEY_NEXT_ID, defaultNextId(current))
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
        writeNotes(updated, preferences.getLong(KEY_NEXT_ID, defaultNextId(updated)))
    }

    @Synchronized
    fun delete(id: Long) {
        val current = mutableNotes.value
        val updated = QuickNotesPolicy.delete(current, id)
        if (updated.size == current.size) return
        writeNotes(updated, preferences.getLong(KEY_NEXT_ID, defaultNextId(updated)))
    }

    private fun readNotes(): List<QuickNote> {
        val count = preferences.getInt(KEY_COUNT, 0).coerceIn(0, QuickNotesPolicy.MAX_NOTES)
        val usedIds = mutableSetOf<Long>()
        return buildList {
            repeat(count) { index ->
                val id = preferences.getLong("$KEY_ID_PREFIX$index", index + 1L)
                val title = preferences.getString("$KEY_TITLE_PREFIX$index", null)
                    ?: preferences.getString("$LEGACY_TEXT_PREFIX$index", null)
                    ?: return@repeat
                val checklistCount = preferences.getInt(
                    "$KEY_CHECKLIST_COUNT_PREFIX$index",
                    0,
                ).coerceIn(0, QuickNotesPolicy.MAX_CHECKLIST_ITEMS)
                val checklist = buildList {
                    repeat(checklistCount) { itemIndex ->
                        val itemText = preferences.getString(
                            "$KEY_CHECKLIST_TEXT_PREFIX${index}_$itemIndex",
                            null,
                        ).orEmpty()
                        if (itemText.isBlank()) return@repeat
                        add(
                            QuickNoteChecklistItem(
                                id = preferences.getLong(
                                    "$KEY_CHECKLIST_ID_PREFIX${index}_$itemIndex",
                                    itemIndex + 1L,
                                ),
                                text = itemText,
                                checked = preferences.getBoolean(
                                    "$KEY_CHECKLIST_CHECKED_PREFIX${index}_$itemIndex",
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
                        type = preferences.getString("$KEY_TYPE_PREFIX$index", null)
                            ?.let { stored ->
                                runCatching { QuickNoteType.valueOf(stored) }.getOrNull()
                            }
                            ?: if (checklist.isNotEmpty()) {
                                QuickNoteType.CHECKLIST
                            } else {
                                QuickNoteType.TEXT
                            },
                        body = preferences.getString("$KEY_BODY_PREFIX$index", "").orEmpty(),
                        checklist = checklist,
                    ),
                ) ?: return@repeat
                if (usedIds.add(id)) add(note)
            }
        }
    }

    private fun writeNotes(notes: List<QuickNote>, nextId: Long) {
        preferences.edit {
            clear()
            putInt(KEY_COUNT, notes.size)
            putLong(KEY_NEXT_ID, nextId.coerceAtLeast(defaultNextId(notes)))
            notes.forEachIndexed { index, note ->
                putLong("$KEY_ID_PREFIX$index", note.id)
                putString("$KEY_TITLE_PREFIX$index", note.title)
                putString("$KEY_TYPE_PREFIX$index", note.type.name)
                putString("$KEY_BODY_PREFIX$index", note.body)
                putInt("$KEY_CHECKLIST_COUNT_PREFIX$index", note.checklist.size)
                note.checklist.forEachIndexed { itemIndex, item ->
                    putLong("$KEY_CHECKLIST_ID_PREFIX${index}_$itemIndex", item.id)
                    putString("$KEY_CHECKLIST_TEXT_PREFIX${index}_$itemIndex", item.text)
                    putBoolean("$KEY_CHECKLIST_CHECKED_PREFIX${index}_$itemIndex", item.checked)
                }
            }
        }
        mutableNotes.value = notes
    }

    private fun defaultNextId(notes: List<QuickNote>): Long =
        (notes.maxOfOrNull(QuickNote::id) ?: 0L) + 1L

    companion object {
        const val PREFERENCES_NAME = "veil_quick_notes"
        private const val KEY_COUNT = "count"
        private const val KEY_NEXT_ID = "next_id"
        private const val KEY_ID_PREFIX = "id_"
        private const val KEY_TITLE_PREFIX = "title_"
        private const val KEY_TYPE_PREFIX = "type_"
        private const val KEY_BODY_PREFIX = "body_"
        private const val KEY_CHECKLIST_COUNT_PREFIX = "checklist_count_"
        private const val KEY_CHECKLIST_ID_PREFIX = "checklist_id_"
        private const val KEY_CHECKLIST_TEXT_PREFIX = "checklist_text_"
        private const val KEY_CHECKLIST_CHECKED_PREFIX = "checklist_checked_"
        private const val LEGACY_TEXT_PREFIX = "text_"
    }
}
