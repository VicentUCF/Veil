package dev.vicent.veil.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import dev.vicent.veil.R
import dev.vicent.veil.launcher.model.LauncherContextKind

@Composable
internal fun launcherContextLabel(kind: LauncherContextKind): String = stringResource(
    when (kind) {
        LauncherContextKind.CURRENT -> R.string.context_current
        LauncherContextKind.WORK -> R.string.context_work
        LauncherContextKind.FOCUS -> R.string.context_focus
        LauncherContextKind.MEDIA -> R.string.context_media
        LauncherContextKind.GAME -> R.string.context_game
        LauncherContextKind.TOOLS -> R.string.context_tools
        LauncherContextKind.ON_THE_GO -> R.string.context_on_the_go
    },
)
