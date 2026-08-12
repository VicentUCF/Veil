package dev.vicent.veil.ui

import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import dev.vicent.veil.launcher.LauncherUiState
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.ui.components.AppCluster
import dev.vicent.veil.ui.components.TopBar
import kotlin.math.abs

@Composable
fun LauncherScreen(
    state: LauncherUiState,
    onContextSelected: (Int) -> Unit,
    onContextStep: (Int) -> Unit,
    onAppSelected: (LauncherApp) -> Unit,
    modifier: Modifier = Modifier,
) {
    val activeContext = state.contexts.getOrNull(state.activeContextIndex)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(state.contexts.size, state.activeContextIndex) {
                var dragDistance = 0f
                val threshold = 72.dp.toPx()

                detectHorizontalDragGestures(
                    onDragStart = { dragDistance = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        dragDistance += dragAmount
                        if (abs(dragDistance) >= threshold) change.consume()
                    },
                    onDragEnd = {
                        when {
                            dragDistance <= -threshold -> onContextStep(1)
                            dragDistance >= threshold -> onContextStep(-1)
                        }
                        dragDistance = 0f
                    },
                    onDragCancel = { dragDistance = 0f },
                )
            },
    ) {
        TopBar(
            contexts = state.contexts.map { it.definition },
            activeContextIndex = state.activeContextIndex,
            onContextSelected = onContextSelected,
            modifier = Modifier.align(Alignment.TopCenter),
        )

        if (activeContext != null && !state.isLoading) {
            val horizontalPadding = if (maxWidth < 360.dp) 24.dp else 32.dp
            val bottomPadding = (maxHeight * 0.07f).coerceIn(40.dp, 88.dp)

            AppCluster(
                context = activeContext,
                onAppSelected = onAppSelected,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .windowInsetsPadding(WindowInsets.navigationBars.only(WindowInsetsSides.Bottom))
                    .padding(start = horizontalPadding, bottom = bottomPadding),
            )
        }
    }
}
