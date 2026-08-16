package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.QuickNote
import dev.vicent.veil.launcher.model.QuickNoteChecklistItem
import dev.vicent.veil.launcher.model.QuickNoteType
import org.junit.Test
import kotlin.test.assertEquals

class QuickNotesPolicyTest {
    @Test
    fun `notes keep creation order and stable ids through edits and deletes`() {
        val added = QuickNotesPolicy.add(
            QuickNotesPolicy.add(emptyList(), QuickNote(id = 41L, title = "Primera")),
            QuickNote(id = 42L, title = "Segunda"),
        )
        val edited = QuickNotesPolicy.update(
            added,
            QuickNote(id = 41L, title = "Actualizada", body = "Contenido"),
        )
        val deleted = QuickNotesPolicy.delete(edited, id = 42L)

        assertEquals(
            listOf(QuickNote(id = 41L, title = "Actualizada", body = "Contenido")),
            deleted,
        )
    }

    @Test
    fun `notes reject blanks and stop at three entries`() {
        val notes = listOf(
            QuickNote(1L, "Una"),
            QuickNote(2L, "Dos"),
            QuickNote(3L, "Tres"),
        )

        assertEquals(notes, QuickNotesPolicy.add(notes, QuickNote(4L, "Cuatro")))
        assertEquals(notes, QuickNotesPolicy.add(notes, QuickNote(4L, "   ")))
        assertEquals(notes, QuickNotesPolicy.update(notes, QuickNote(1L, "   ")))
    }

    @Test
    fun `note fields are trimmed bounded and blank checklist items are discarded`() {
        val longTitle = "x".repeat(QuickNotesPolicy.MAX_TITLE_LENGTH + 20)
        val note = QuickNote(
            id = 1L,
            title = "  $longTitle  ",
            type = QuickNoteType.CHECKLIST,
            body = "  cuerpo  ",
            checklist = listOf(
                QuickNoteChecklistItem(1L, "  comprar  "),
                QuickNoteChecklistItem(2L, "   "),
            ),
        )
        val sanitized = QuickNotesPolicy.sanitize(note)

        assertEquals(QuickNotesPolicy.MAX_TITLE_LENGTH, sanitized?.title?.length)
        assertEquals(QuickNoteType.CHECKLIST, sanitized?.type)
        assertEquals("", sanitized?.body)
        assertEquals(listOf(QuickNoteChecklistItem(1L, "comprar")), sanitized?.checklist)
    }

    @Test
    fun `text and checklist modes keep only their active content`() {
        val checklist = listOf(QuickNoteChecklistItem(1L, "item"))
        val textNote = QuickNotesPolicy.sanitize(
            QuickNote(1L, "Texto", QuickNoteType.TEXT, "contenido", checklist),
        )
        val checklistNote = QuickNotesPolicy.sanitize(
            QuickNote(2L, "Lista", QuickNoteType.CHECKLIST, "oculto", checklist),
        )

        assertEquals(emptyList(), textNote?.checklist)
        assertEquals("contenido", textNote?.body)
        assertEquals("", checklistNote?.body)
        assertEquals(checklist, checklistNote?.checklist)
    }
}
