package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.LauncherContext
import dev.vicent.veil.launcher.model.WorkspaceCapability

object WorkspaceActivationPolicy {
    fun uses(context: LauncherContext?, capability: WorkspaceCapability): Boolean =
        capability in context?.capabilities.orEmpty()
}
