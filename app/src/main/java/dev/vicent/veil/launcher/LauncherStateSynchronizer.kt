package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.repository.AmbientContinuityRepository
import dev.vicent.veil.launcher.repository.AppRepository
import dev.vicent.veil.launcher.repository.AudioMixerRepository
import dev.vicent.veil.launcher.repository.CalendarRepository
import dev.vicent.veil.launcher.repository.FocusTimerRepository
import dev.vicent.veil.launcher.repository.LauncherPreferencesRepository
import dev.vicent.veil.launcher.repository.QuickNotesRepository
import dev.vicent.veil.launcher.repository.SteamGameRepository
import dev.vicent.veil.launcher.repository.SystemStatusRepository
import dev.vicent.veil.launcher.repository.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

internal class LauncherStateSynchronizer(
    private val appRepository: AppRepository,
    private val continuityRepository: AmbientContinuityRepository,
    private val calendarRepository: CalendarRepository,
    private val weatherRepository: WeatherRepository,
    private val focusTimerRepository: FocusTimerRepository,
    private val quickNotesRepository: QuickNotesRepository,
    private val systemStatusRepository: SystemStatusRepository,
    private val audioMixerRepository: AudioMixerRepository,
    private val steamGameRepository: SteamGameRepository,
    private val preferencesRepository: LauncherPreferencesRepository,
    private val contextResolver: LauncherContextResolver,
    private val state: MutableStateFlow<LauncherUiState>,
) {
    private var hasStarted = false

    fun start(scope: CoroutineScope) {
        if (hasStarted) return
        hasStarted = true

        continuityRepository.start(scope)
        audioMixerRepository.start(scope)
        systemStatusRepository.start(scope)
        calendarRepository.startObserving(scope) { state.value.calendarAccessGranted }
        focusTimerRepository.startObserving(scope)

        scope.launch {
            preferencesRepository.state.collect { preferences ->
                state.update { currentState ->
                    currentState.copy(preferences = preferences)
                        .withInstalledApps(
                            installedApps = currentState.installedApps,
                            appScanComplete = !currentState.isLoading,
                        )
                }
            }
        }
        scope.launch {
            continuityRepository.items.collect { items ->
                val now = System.currentTimeMillis()
                state.update { currentState ->
                    val selectedMedia = ContinuityRanker.selectMedia(items, now)
                    audioMixerRepository.setMediaPlaying(selectedMedia?.isPlaying == true)
                    currentState.copy(
                        currentContinuity = ContinuityRanker.selectCurrent(items, now),
                        mediaContinuity = selectedMedia,
                        workProgress = ContinuityRanker.selectWorkProgress(
                            items = items,
                            workPackages = currentState.workPackageNames(),
                            nowMillis = now,
                        ),
                    )
                }
            }
        }
        scope.launch {
            continuityRepository.notificationIndicatorPackages.collect { packages ->
                state.update { it.copy(notificationIndicatorPackages = packages) }
            }
        }
        scope.launch {
            val priorityApps = appRepository.loadPriorityApps(
                contextResolver.priorityPackageNames(preferencesRepository.state.value),
            )
            state.update { currentState ->
                if (!currentState.isLoading) {
                    currentState
                } else {
                    currentState.withInstalledApps(
                        installedApps = mergeApps(currentState.installedApps, priorityApps),
                        appScanComplete = false,
                    )
                }
            }
        }
        scope.launch {
            val installedApps = appRepository.loadLaunchableApps()
            state.update { currentState ->
                currentState.withInstalledApps(
                    installedApps = installedApps,
                    appScanComplete = true,
                ).copy(isLoading = false)
            }
        }

        scope.launch {
            calendarRepository.events.collect { events ->
                state.update { it.copy(calendarEvents = events) }
            }
        }
        scope.launch {
            weatherRepository.state.collect { weather ->
                state.update { it.copy(weather = weather) }
            }
        }
        scope.launch {
            focusTimerRepository.state.collect { focus ->
                state.update { it.copy(focusTimer = focus) }
            }
        }
        scope.launch {
            quickNotesRepository.notes.collect { notes ->
                state.update { it.copy(quickNotes = notes) }
            }
        }
        scope.launch {
            systemStatusRepository.status.collect { systemStatus ->
                state.update { it.copy(systemStatus = systemStatus) }
            }
        }
        scope.launch {
            audioMixerRepository.state.collect { audioMixer ->
                state.update { it.copy(audioMixer = audioMixer) }
            }
        }
        scope.launch {
            steamGameRepository.state.collect { gameFeed ->
                state.update { it.copy(gameFeed = gameFeed) }
            }
        }
    }

    fun refreshApps(scope: CoroutineScope) {
        scope.launch {
            val installedApps = appRepository.refreshLaunchableApps()
            state.update { currentState ->
                currentState.withInstalledApps(installedApps)
            }
        }
    }

    fun removeUnavailableApp(packageName: String) {
        state.update { currentState ->
            currentState.withInstalledApps(
                currentState.installedApps.filterNot { app ->
                    app.packageName == packageName
                },
            )
        }
    }

    private fun LauncherUiState.withInstalledApps(
        installedApps: List<LauncherApp>,
        appScanComplete: Boolean = true,
    ): LauncherUiState {
        val resolvedContexts = contextResolver.resolve(
            installedApps = installedApps,
            preferences = preferences,
            appScanComplete = appScanComplete,
        )
        return copy(
            installedApps = installedApps,
            contexts = resolvedContexts,
            workProgress = ContinuityRanker.selectWorkProgress(
                continuityRepository.items.value,
                resolvedContexts
                    .firstOrNull { it.definition.kind == LauncherContextKind.WORK }
                    ?.apps
                    ?.mapTo(mutableSetOf(), LauncherApp::packageName)
                    .orEmpty(),
                System.currentTimeMillis(),
            ),
        )
    }

    private fun LauncherUiState.workPackageNames(): Set<String> =
        contexts
            .firstOrNull { it.definition.kind == LauncherContextKind.WORK }
            ?.apps
            ?.mapTo(mutableSetOf(), LauncherApp::packageName)
            .orEmpty()

    private fun mergeApps(
        current: List<LauncherApp>,
        incoming: List<LauncherApp>,
    ): List<LauncherApp> = (current + incoming).distinctBy(LauncherApp::packageName)
}
