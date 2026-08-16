package dev.vicent.veil.launcher

import dev.vicent.veil.launcher.model.LauncherContextKind

object WorkspaceLayoutPolicy {
    fun showsContextDock(kind: LauncherContextKind): Boolean =
        kind != LauncherContextKind.CURRENT
}
