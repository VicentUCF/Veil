package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.LauncherContext
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
    val activeContextIndex: Int = 0,
    val isLoading: Boolean = true,
)

class LauncherController(
    private val appRepository: AppRepository,
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

        scope.launch {
            val installedApps = appRepository.loadLaunchableApps()
            val resolvedContexts = initialContexts.map { context ->
                val configuredApps = appRepository.resolveConfiguredApps(
                    packageNames = context.definition.apps,
                    installedApps = installedApps,
                )
                val apps = if (
                    context.definition.id == "home" && configuredApps.isEmpty()
                ) {
                    appRepository.selectAutomaticHomeApps(
                        installedApps = installedApps,
                        count = automaticHomeAppCount,
                    )
                } else {
                    configuredApps
                }
                context.copy(apps = apps)
            }

            mutableState.update { currentState ->
                currentState.copy(contexts = resolvedContexts, isLoading = false)
            }
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
                contexts = currentState.contexts.map { context ->
                    context.copy(apps = context.apps.filterNot { it.packageName == packageName })
                },
            )
        }
    }
}

private fun Int.floorMod(modulus: Int): Int = ((this % modulus) + modulus) % modulus
