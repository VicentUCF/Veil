package dev.vicent.veil.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.vicent.veil.R
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.launcher.model.LauncherContextKind
import dev.vicent.veil.ui.components.AppActionsBottomSheet
import dev.vicent.veil.ui.components.RofiAction
import dev.vicent.veil.ui.components.RofiBody
import dev.vicent.veil.ui.components.RofiDialog

internal data class AppActionsTarget(
    val app: LauncherApp,
    val contextKind: LauncherContextKind? = null,
    val slotIndex: Int? = null,
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
                RofiAction("ahora no", ::closeContinuityDisclosure)
                RofiAction("revisar ajustes", {
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
            title = "tiempo local",
            onDismiss = onDisclosureDismissed,
            actions = {
                RofiAction("cancelar", onDisclosureDismissed)
                RofiAction("continuar", {
                    onDisclosureDismissed()
                    accessActions.onLocationPermissionRequested()
                })
            },
        ) {
            RofiBody(
                "Veil usará únicamente ubicación aproximada mientras Home esté visible. " +
                    "Las coordenadas aproximadas y tu IP se enviarán a Open‑Meteo; " +
                    "Veil guardará sólo el último resultado durante la caché.",
            )
        }
    }

    if (activeDisclosure == LauncherDisclosure.AUDIO_VISUALIZER) {
        RofiDialog(
            title = "espectro de audio",
            onDismiss = onDisclosureDismissed,
            actions = {
                RofiAction("ahora no", onDisclosureDismissed)
                RofiAction("activar", {
                    onDisclosureDismissed()
                    accessActions.onAudioVisualizerPermissionRequested()
                })
            },
        ) {
            RofiBody(
                "Android exige permiso de micrófono para analizar la mezcla de salida. " +
                    "Veil sólo recibe una señal FFT de baja calidad mientras MEDIA está visible; " +
                    "no graba, guarda ni transmite audio.",
            )
        }
    }

    appActionsTarget?.let { target ->
        val app = target.app
        AppActionsBottomSheet(
            app = app,
            onDismiss = onAppActionsDismissed,
            onOpen = {
                onAppActionsDismissed()
                appActions.onAppSelected(app)
            },
            onAppInfo = {
                onAppActionsDismissed()
                appActions.onAppInfoSelected(app)
            },
            onUninstall = {
                onAppActionsDismissed()
                appActions.onAppUninstallSelected(app)
            },
            contextLabel = target.contextKind?.name,
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
