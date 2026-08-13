package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.ContinuityItem
import dev.vicent.veil.launcher.model.LauncherContextKind

sealed interface HomeSurfaceMode {
    data object Onboarding : HomeSurfaceMode
    data class Continuity(val item: ContinuityItem) : HomeSurfaceMode
    data class Apps(val kind: LauncherContextKind) : HomeSurfaceMode
    data object Tools : HomeSurfaceMode
}

object HomeSurfaceResolver {
    fun resolve(
        kind: LauncherContextKind,
        accessGranted: Boolean,
        onboardingDismissed: Boolean,
        currentContinuity: ContinuityItem?,
        mediaContinuity: ContinuityItem.Media?,
    ): HomeSurfaceMode {
        if (
            kind == LauncherContextKind.CURRENT &&
            !accessGranted &&
            !onboardingDismissed
        ) {
            return HomeSurfaceMode.Onboarding
        }
        val continuity = when (kind) {
            LauncherContextKind.CURRENT -> currentContinuity
            LauncherContextKind.MEDIA -> mediaContinuity
            else -> null
        }
        return when {
            continuity != null -> HomeSurfaceMode.Continuity(continuity)
            kind == LauncherContextKind.TOOLS -> HomeSurfaceMode.Tools
            else -> HomeSurfaceMode.Apps(kind)
        }
    }
}
