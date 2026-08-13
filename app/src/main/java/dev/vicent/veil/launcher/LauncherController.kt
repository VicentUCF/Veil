package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.LauncherContext
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.ContinuityItem
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.model.CalendarEventSummary
import dev.vicent.veil.launcher.model.FocusTimerState
import dev.vicent.veil.launcher.model.QuickActionSpec
import dev.vicent.veil.launcher.model.SystemStatus
import dev.vicent.veil.launcher.model.WeatherState
import dev.vicent.veil.launcher.repository.AmbientContinuityRepository
import dev.vicent.veil.launcher.repository.AppRepository
import dev.vicent.veil.launcher.repository.CalendarRepository
import dev.vicent.veil.launcher.repository.FocusTimerRepository
import dev.vicent.veil.launcher.repository.SystemStatusRepository
import dev.vicent.veil.launcher.repository.WeatherRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResolvedLauncherContext(
    val definition: LauncherContext,
    val apps: List<LauncherApp>,
    val quickActions: List<ResolvedQuickAction> = emptyList(),
)

sealed interface ResolvedQuickAction {
    data class App(val app: LauncherApp) : ResolvedQuickAction
    data class Setting(val id: String) : ResolvedQuickAction
}

data class LauncherUiState(
    val contexts: List<ResolvedLauncherContext>,
    val installedApps: List<LauncherApp> = emptyList(),
    val activeContextIndex: Int = 0,
    val isLoading: Boolean = true,
    val isDrawerOpen: Boolean = false,
    val continuityAccessGranted: Boolean = false,
    val currentContinuity: ContinuityItem? = null,
    val mediaContinuity: ContinuityItem.Media? = null,
    val workProgress: ContinuityItem.Progress? = null,
    val calendarEvents: List<CalendarEventSummary> = emptyList(),
    val calendarAccessGranted: Boolean = false,
    val weather: WeatherState = WeatherState(),
    val locationAccessGranted: Boolean = false,
    val focusTimer: FocusTimerState = FocusTimerState(),
    val systemStatus: SystemStatus = SystemStatus(),
    val isContinuityOnboardingDismissed: Boolean = continuityOnboardingDismissed,
)

class LauncherController(
    private val appRepository: AppRepository,
    private val continuityRepository: AmbientContinuityRepository,
    private val calendarRepository: CalendarRepository,
    private val weatherRepository: WeatherRepository,
    private val focusTimerRepository: FocusTimerRepository,
    private val systemStatusRepository: SystemStatusRepository,
    contexts: List<LauncherContext>,
    private val quickActionCount: Int,
) {
    private val initialContexts = contexts.map { context ->
        ResolvedLauncherContext(definition = context, apps = emptyList())
    }

    private val mutableState = MutableStateFlow(LauncherUiState(contexts = initialContexts))
    val state: StateFlow<LauncherUiState> = mutableState.asStateFlow()

    private var hasLoaded = false

    fun load(scope: CoroutineScope) {
        if (hasLoaded) return
        hasLoaded = true

        continuityRepository.start(scope)
        calendarRepository.startObserving(scope) { mutableState.value.calendarAccessGranted }
        focusTimerRepository.startObserving(scope)
        scope.launch {
            continuityRepository.items.collect { items ->
                val now = System.currentTimeMillis()
                mutableState.update { currentState ->
                    currentState.copy(
                        currentContinuity = ContinuityRanker.selectCurrent(items, now),
                        mediaContinuity = ContinuityRanker.selectMedia(items, now),
                        workProgress = ContinuityRanker.selectWorkProgress(
                            items = items,
                            workPackages = currentState.contexts
                                .firstOrNull { it.definition.kind == LauncherContextKind.WORK }
                                ?.apps
                                ?.mapTo(mutableSetOf(), LauncherApp::packageName)
                                .orEmpty(),
                            nowMillis = now,
                        ),
                    )
                }
            }
        }

        scope.launch {
            val installedApps = appRepository.loadLaunchableApps()
            val resolvedContexts = initialContexts.map { context ->
                val configuredPackages = context.definition.quickActions
                    .filterIsInstance<QuickActionSpec.App>()
                    .map(QuickActionSpec.App::packageName)
                val apps = appRepository.selectQuickApps(
                    kind = context.definition.kind,
                    configuredPackageNames = configuredPackages,
                    installedApps = installedApps,
                    count = quickActionCount,
                )
                val appIterator = apps.iterator()
                val quickActions = context.definition.quickActions.mapNotNull { spec ->
                    when (spec) {
                        is QuickActionSpec.App -> if (appIterator.hasNext()) {
                            ResolvedQuickAction.App(appIterator.next())
                        } else null
                        is QuickActionSpec.Setting -> ResolvedQuickAction.Setting(spec.id)
                    }
                }.take(quickActionCount)
                context.copy(apps = apps, quickActions = quickActions)
            }

            mutableState.update { currentState ->
                val now = System.currentTimeMillis()
                val workPackages = resolvedContexts
                    .firstOrNull { it.definition.kind == LauncherContextKind.WORK }
                    ?.apps
                    ?.mapTo(mutableSetOf(), LauncherApp::packageName)
                    .orEmpty()
                currentState.copy(
                    contexts = resolvedContexts,
                    installedApps = installedApps,
                    isLoading = false,
                    workProgress = ContinuityRanker.selectWorkProgress(
                        continuityRepository.items.value,
                        workPackages,
                        now,
                    ),
                )
            }
        }

        scope.launch {
            calendarRepository.events.collect { events ->
                mutableState.update { it.copy(calendarEvents = events) }
            }
        }
        scope.launch {
            weatherRepository.state.collect { weather ->
                mutableState.update { it.copy(weather = weather) }
            }
        }
        scope.launch {
            focusTimerRepository.state.collect { focus ->
                mutableState.update { it.copy(focusTimer = focus) }
            }
        }
        scope.launch {
            systemStatusRepository.status.collect { systemStatus ->
                mutableState.update { it.copy(systemStatus = systemStatus) }
            }
        }
        systemStatusRepository.refresh()
    }

    fun setContinuityAccessGranted(granted: Boolean) {
        continuityRepository.setAccessEnabled(granted)
        mutableState.update { it.copy(continuityAccessGranted = granted) }
    }

    fun dismissContinuityOnboarding() {
        continuityOnboardingDismissed = true
        mutableState.update { it.copy(isContinuityOnboardingDismissed = true) }
    }

    fun performContinuityAction(itemId: String, action: ContinuityAction, positionMillis: Long? = null) {
        continuityRepository.perform(itemId, action, positionMillis)
    }

    fun setCalendarAccessGranted(granted: Boolean, scope: CoroutineScope) {
        mutableState.update { it.copy(calendarAccessGranted = granted) }
        scope.launch { calendarRepository.refresh(granted) }
    }

    fun setLocationAccessGranted(granted: Boolean, scope: CoroutineScope) {
        mutableState.update { it.copy(locationAccessGranted = granted) }
        scope.launch { weatherRepository.refresh(granted) }
    }

    fun refreshVisibleData(scope: CoroutineScope) {
        systemStatusRepository.refresh()
        scope.launch { calendarRepository.refresh(mutableState.value.calendarAccessGranted) }
        if (mutableState.value.contexts.getOrNull(mutableState.value.activeContextIndex)
                ?.definition?.kind == LauncherContextKind.CURRENT
        ) {
            scope.launch { weatherRepository.refresh(mutableState.value.locationAccessGranted) }
        }
    }

    fun openCalendarEvent(eventId: Long) = calendarRepository.open(eventId)

    fun startFocus(minutes: Int) = focusTimerRepository.start(minutes)
    fun pauseFocus() = focusTimerRepository.pause()
    fun resumeFocus() = focusTimerRepository.resume()
    fun finishFocus() = focusTimerRepository.finish()
    fun restoreFocusAlarm() = focusTimerRepository.restoreScheduledAlarm()

    fun openDrawer() {
        mutableState.update { currentState -> currentState.copy(isDrawerOpen = true) }
    }

    fun closeDrawer() {
        mutableState.update { currentState -> currentState.copy(isDrawerOpen = false) }
    }

    fun toggleDrawer() {
        mutableState.update { currentState ->
            currentState.copy(isDrawerOpen = !currentState.isDrawerOpen)
        }
    }

    fun handleHomePressed() {
        mutableState.update { currentState ->
            when {
                currentState.isDrawerOpen -> currentState.copy(isDrawerOpen = false)
                else -> currentState.copy(isDrawerOpen = true)
            }
        }
    }

    fun selectContext(index: Int) {
        if (index !in mutableState.value.contexts.indices) return
        mutableState.update { currentState ->
            currentState.copy(activeContextIndex = index)
        }
    }

    fun stepContext(direction: Int) {
        val contextCount = mutableState.value.contexts.size
        if (contextCount == 0 || direction == 0) return

        val nextIndex = (mutableState.value.activeContextIndex + direction)
            .floorMod(contextCount)
        selectContext(nextIndex)
    }

    fun removeUnavailableApp(packageName: String) {
        mutableState.update { currentState ->
            currentState.copy(
                installedApps = currentState.installedApps.filterNot {
                    it.packageName == packageName
                },
                contexts = currentState.contexts.map { context ->
                    context.copy(apps = context.apps.filterNot { it.packageName == packageName })
                },
            )
        }
    }
}

private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus

private var continuityOnboardingDismissed = false
