package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.LauncherContext
import dev.vicent.veil.launcher.model.ContinuityAction
import dev.vicent.veil.launcher.model.ContinuityItem
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.launcher.repository.AmbientContinuityRepository
import dev.vicent.veil.launcher.repository.AppRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ResolvedLauncherContext(
    val definition: LauncherContext,
    val apps: List<LauncherApp>,
)

data class LauncherUiState(
    val contexts: List<ResolvedLauncherContext>,
    val installedApps: List<LauncherApp> = emptyList(),
    val activeContextIndex: Int = 0,
    val isLoading: Boolean = true,
    val isDrawerOpen: Boolean = false,
    val continuityAccessGranted: Boolean = false,
    val currentContinuity: ContinuityItem? = null,
    val mediaContinuity: ContinuityItem.Media? = null,
    val isContinuityOnboardingDismissed: Boolean = continuityOnboardingDismissed,
)

class LauncherController(
    private val appRepository: AppRepository,
    private val continuityRepository: AmbientContinuityRepository,
    contexts: List<LauncherContext>,
    private val automaticHomeAppCount: Int,
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
        scope.launch {
            continuityRepository.items.collect { items ->
                val now = System.currentTimeMillis()
                mutableState.update { currentState ->
                    currentState.copy(
                        currentContinuity = ContinuityRanker.selectCurrent(items, now),
                        mediaContinuity = ContinuityRanker.selectMedia(items, now),
                    )
                }
            }
        }

        scope.launch {
            val installedApps = appRepository.loadLaunchableApps()
            val resolvedContexts = initialContexts.map { context ->
                val configuredApps = appRepository.resolveConfiguredApps(
                    packageNames = context.definition.apps,
                    installedApps = installedApps,
                )
                val apps = when (context.definition.kind) {
                    LauncherContextKind.CURRENT -> {
                        val automaticApps = appRepository.selectAutomaticHomeApps(
                            installedApps = installedApps,
                            count = automaticHomeAppCount,
                        )
                        (configuredApps + automaticApps)
                            .distinctBy(LauncherApp::packageName)
                            .take(automaticHomeAppCount)
                    }
                    LauncherContextKind.WORK,
                    LauncherContextKind.MEDIA,
                    LauncherContextKind.SOCIAL,
                    -> appRepository.selectContextApps(
                        kind = context.definition.kind,
                        configuredApps = configuredApps,
                        installedApps = installedApps,
                        count = automaticHomeAppCount,
                    )
                    LauncherContextKind.TOOLS -> emptyList()
                }
                context.copy(apps = apps)
            }

            mutableState.update { currentState ->
                currentState.copy(
                    contexts = resolvedContexts,
                    installedApps = installedApps,
                    isLoading = false,
                )
            }
        }
    }

    fun setContinuityAccessGranted(granted: Boolean) {
        continuityRepository.setAccessEnabled(granted)
        mutableState.update { it.copy(continuityAccessGranted = granted) }
    }

    fun dismissContinuityOnboarding() {
        continuityOnboardingDismissed = true
        mutableState.update { it.copy(isContinuityOnboardingDismissed = true) }
    }

    fun performContinuityAction(itemId: String, action: ContinuityAction) {
        continuityRepository.perform(itemId, action)
    }

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

    fun selectContext(index: Int) {
        if (index !in mutableState.value.contexts.indices) return
        mutableState.update { currentState -> currentState.copy(activeContextIndex = index) }
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
