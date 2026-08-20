package dev.vicent.veil.launcher.repository

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import dev.vicent.veil.launcher.TimeProvider
import dev.vicent.veil.launcher.model.AccentMode
import dev.vicent.veil.launcher.model.FocusTimerStatus
import dev.vicent.veil.launcher.model.HomeTextTone
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.QuickNoteType
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RepositoryPersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Before
    @After
    fun clearPreferences() {
        listOf(
            LauncherPreferencesRepository.PREFERENCES_NAME,
            QuickNotesRepository.PREFERENCES_NAME,
            FocusTimerStore.PREFERENCES_NAME,
            SearchLearningRepository.PREFERENCES_NAME,
        ).forEach { name ->
            context.getSharedPreferences(name, Context.MODE_PRIVATE).edit().clear().commit()
        }
    }

    @Test
    fun launcherAppearanceSurvivesRepositoryRecreation() {
        LauncherPreferencesRepository(context).apply {
            setAccentMode(AccentMode.SKY)
            setHomeTextTone(HomeTextTone.DARK)
            setWallpaperScrimIntensity(0.73f)
        }

        val restored = LauncherPreferencesRepository(context).state.value

        assertEquals(AccentMode.SKY, restored.accentMode)
        assertEquals(HomeTextTone.DARK, restored.homeTextTone)
        assertEquals(0.73f, restored.wallpaperScrimIntensity)
    }

    @Test
    fun workspaceSelectionAndSetupSurviveRepositoryRecreation() {
        val repository = LauncherPreferencesRepository(context)
        val replacement = LauncherContextKind.entries.first {
            it != LauncherContextKind.CURRENT &&
                it !in repository.state.value.selectedWorkspaceKinds
        }
        repository.apply {
            replaceWorkspace(0, replacement)
            moveWorkspace(0, 3)
            completeWorkspaceSetup()
        }
        val expected = repository.state.value.selectedWorkspaceKinds

        val restored = LauncherPreferencesRepository(context).state.value

        assertEquals(expected, restored.selectedWorkspaceKinds)
        assertEquals(true, restored.workspaceSetupCompleted)
    }

    @Test
    fun quickNotesSurviveRepositoryRecreation() {
        QuickNotesRepository(context).add(
            title = "Prueba",
            type = QuickNoteType.TEXT,
            body = "Contenido",
            checklist = emptyList(),
        )

        val restored = QuickNotesRepository(context).notes.value.single()

        assertEquals("Prueba", restored.title)
        assertEquals("Contenido", restored.body)
    }

    @Test
    fun focusStoreUsesInjectedTimeWhenRestoringRemainingDuration() {
        val now = 50_000L
        val store = FocusTimerStore(context, TimeProvider { now })
        store.write(
            status = FocusTimerStatus.RUNNING,
            duration = 20_000L,
            remaining = 20_000L,
            endAt = now + 7_000L,
        )

        val restored = store.read(
            exactAlarmAvailable = true,
            notificationsAvailable = true,
        )

        assertEquals(7_000L, restored.remainingMillis)
    }

    @Test
    fun searchLearningSurvivesRepositoryRecreationAndRemovesMissingApps() {
        val now = 100_000L
        SearchLearningRepository(context, TimeProvider { now }).apply {
            recordSuccessfulSelection("Ínsta", "com.instagram.android")
            recordSuccessfulSelection("insta", "com.instagram.android")
        }

        val restored = SearchLearningRepository(context, TimeProvider { now })
        val entry = restored.state.value.entries.single()
        assertEquals("insta", entry.query)
        assertEquals(2, entry.selectionCount)

        restored.retainInstalledPackages(emptySet())
        assertEquals(0, restored.state.value.entries.size)
    }
}
