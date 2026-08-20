package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.AppSearchLearningEntry
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.Test

class AppSearchPolicyTest {
    private val now = 100L * DAY_MILLIS

    @Test
    fun `normalization ignores accents case and repeated whitespace`() {
        assertEquals("camara rapida", normalizeSearchText("  CÁMARA   Rápida "))
    }

    @Test
    fun `text ranking supports prefix words package and controlled typo`() {
        val apps = listOf(
            app("com.instagram.android", "Instagram", 0),
            app("com.example.notes", "Quick Notes", 1),
            app("dev.calendar", "Agenda", 2),
        )

        assertEquals("Instagram", rank(apps, "inst").first().label)
        assertEquals("Quick Notes", rank(apps, "qu no").single().label)
        assertEquals("Agenda", rank(apps, "calendar").single().label)
        assertEquals("Instagram", rank(apps, "instgram").single().label)
        assertTrue(rank(apps, "isn").none { it.label == "Instagram" })
    }

    @Test
    fun `direct and compatible learning move a selected app before generic matches`() {
        val apps = listOf(
            app("com.example.instant", "Instant Notes", 0),
            app("com.instagram.android", "Instagram", 1),
        )
        val learned = listOf(entry("instagram", "com.instagram.android", 3, now))

        assertEquals("Instagram", rank(apps, "inst", learned).first().label)
        assertEquals("Instagram", rank(apps, "instagram", learned).first().label)
    }

    @Test
    fun `exact visible name stays ahead of learned association`() {
        val apps = listOf(
            app("com.instagram.android", "Instagram", 0),
            app("com.instagram.lite", "Instagram Lite", 1),
        )
        val learned = listOf(entry("instagram", "com.instagram.lite", 20, now))

        assertEquals("Instagram", rank(apps, "instagram", learned).first().label)
    }

    @Test
    fun `recent selection can overtake an old habit and unrelated aliases stay hidden`() {
        val apps = listOf(
            app("com.example.instant", "Instant Notes", 0),
            app("com.instagram.android", "Instagram", 1),
            app("com.calendar", "Calendar", 2),
        )
        val learned = listOf(
            entry("inst", "com.example.instant", 5, now - 89L * DAY_MILLIS),
            entry("inst", "com.instagram.android", 1, now),
        )

        assertEquals("Instagram", rank(apps, "inst", learned).first().label)
        assertFalse(rank(apps, "calendar", learned).any { it.label == "Instagram" })
    }

    @Test
    fun `expired alias no longer introduces an unrelated app`() {
        val apps = listOf(app("com.instagram.android", "Instagram", 0))
        val expired = listOf(
            entry(
                "photo",
                "com.instagram.android",
                5,
                now - SearchLearningPolicy.RETENTION_MILLIS - 1,
            ),
        )

        assertTrue(rank(apps, "photo", expired).isEmpty())
    }

    @Test
    fun `settings matching shares normalization and typo tolerance`() {
        assertTrue(AppSearchPolicy.matches("configuracion", "Configuración del sistema"))
        assertTrue(AppSearchPolicy.matches("configuracon", "Configuración del sistema"))
        assertFalse(AppSearchPolicy.matches("inst", "Configuración del sistema"))
    }

    private fun rank(
        apps: List<AppSearchCandidate>,
        query: String,
        learning: List<AppSearchLearningEntry> = emptyList(),
    ) = AppSearchPolicy.rank(apps, query, learning, now)

    private fun app(packageName: String, label: String, index: Int) =
        AppSearchCandidate(packageName, label, index)

    private fun entry(query: String, packageName: String, count: Int, lastUsed: Long) =
        AppSearchLearningEntry(query, packageName, count, lastUsed)

    private companion object {
        const val DAY_MILLIS = 24L * 60L * 60L * 1_000L
    }
}
