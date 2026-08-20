package dev.vicent.veil.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.vicent.veil.R
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.ui.components.AppActionsBottomSheet
import dev.vicent.veil.ui.components.launcherContextLabel
import dev.vicent.veil.ui.components.RofiAction
import dev.vicent.veil.ui.components.RofiBody
import dev.vicent.veil.ui.components.RofiDialog

internal data class AppActionsTarget(
    val app: LauncherApp,
    val contextKind: LauncherContextKind? = null,
    val slotIndex: Int? = null,
    val searchQuery: String? = null,
)

internal enum class LauncherDisclosure {
    CONTINUITY,
    LOCATION,
    AUDIO_VISUALIZER,
}

@Composable
internal fun LauncherOverlays(
    activeDisclosure: LauncherDisclosure?,
    showAutomaticContinuityDisclosure: Boolean,
    appActionsTarget: AppActionsTarget?,
    navigationActions: LauncherNavigationActions,
    appActions: LauncherAppActions,
    accessActions: LauncherAccessActions,
    onDisclosureDismissed: () -> Unit,
    onAppActionsDismissed: () -> Unit,
) {
    if (
        activeDisclosure == LauncherDisclosure.CONTINUITY ||
        showAutomaticContinuityDisclosure
    ) {
        fun closeContinuityDisclosure() {
            onDisclosureDismissed()
            if (showAutomaticContinuityDisclosure) {
                accessActions.onContinuityOnboardingDismissed()
            }
        }
        RofiDialog(
            title = stringResource(R.string.continuity_onboarding_title),
            onDismiss = ::closeContinuityDisclosure,
            actions = {
                RofiAction(stringResource(R.string.action_not_now), ::closeContinuityDisclosure)
                RofiAction(stringResource(R.string.action_review_settings), {
                    closeContinuityDisclosure()
                    accessActions.onContinuityAccessRequested()
                })
            },
        ) {
            RofiBody(stringResource(R.string.continuity_onboarding_body))
        }
    }

    if (activeDisclosure == LauncherDisclosure.LOCATION) {
        RofiDialog(
            title = stringResource(R.string.location_disclosure_title),
            onDismiss = onDisclosureDismissed,
            actions = {
                RofiAction(stringResource(R.string.action_cancel), onDisclosureDismissed)
                RofiAction(stringResource(R.string.action_continue), {
                    onDisclosureDismissed()
                    accessActions.onLocationPermissionRequested()
                })
            },
        ) {
            RofiBody(stringResource(R.string.location_disclosure_body))
        }
    }

    if (activeDisclosure == LauncherDisclosure.AUDIO_VISUALIZER) {
        RofiDialog(
            title = stringResource(R.string.audio_disclosure_title),
            onDismiss = onDisclosureDismissed,
            actions = {
                RofiAction(stringResource(R.string.action_not_now), onDisclosureDismissed)
                RofiAction(stringResource(R.string.action_activate), {
                    onDisclosureDismissed()
                    accessActions.onAudioVisualizerPermissionRequested()
                })
            },
        ) {
            RofiBody(stringResource(R.string.audio_disclosure_body))
        }
    }

    appActionsTarget?.let { target ->
        val app = target.app
        AppActionsBottomSheet(
            app = app,
            onDismiss = onAppActionsDismissed,
            onOpen = {
                onAppActionsDismissed()
                val query = target.searchQuery
                if (query.isNullOrBlank()) {
                    appActions.onAppSelected(app)
                } else {
                    appActions.onSearchAppSelected(app, query)
                }
            },
            onAppInfo = {
                onAppActionsDismissed()
                appActions.onAppInfoSelected(app)
            },
            onUninstall = {
                onAppActionsDismissed()
                appActions.onAppUninstallSelected(app)
            },
            contextLabel = target.contextKind?.let { launcherContextLabel(it) },
            onReplaceInContext = target.contextKind?.let { kind ->
                target.slotIndex?.let { slotIndex ->
                    {
                        onAppActionsDismissed()
                        navigationActions.onOpenContextSlotPicker(kind, slotIndex)
                    }
                }
            },
            onRemoveFromContext = target.contextKind?.let { kind ->
                target.slotIndex?.let { slotIndex ->
                    {
                        onAppActionsDismissed()
                        appActions.onContextSlotCleared(kind, slotIndex)
                    }
                }
            },
        )
    }
}
