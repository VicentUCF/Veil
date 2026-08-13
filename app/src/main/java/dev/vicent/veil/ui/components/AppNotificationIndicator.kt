package dev.vicent.veil.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import dev.vicent.veil.ui.theme.LocalVeilPalette

@Composable
fun AppNotificationIndicator(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(120)),
        exit = fadeOut(tween(120)),
        modifier = modifier,
        label = "app notification indicator",
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .background(palette.accentActive, CircleShape)
                .border(1.dp, palette.indicatorOutline, CircleShape),
        )
    }
}
