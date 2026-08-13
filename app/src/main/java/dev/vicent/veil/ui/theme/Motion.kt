package dev.vicent.veil.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing

/** Shared motion timings for Veil's navigation surfaces. */
internal object VeilMotion {
    const val QuickDurationMillis = 120
    const val StandardDurationMillis = 180
    const val EmphasizedDurationMillis = 200

    val standardEasing: Easing = FastOutSlowInEasing
    val enterEasing: Easing = LinearOutSlowInEasing
    val exitEasing: Easing = FastOutLinearInEasing
}
