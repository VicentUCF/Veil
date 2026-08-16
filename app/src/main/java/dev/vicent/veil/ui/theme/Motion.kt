package dev.vicent.veil.ui.theme

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing

/** Shared motion timings for Veil's navigation surfaces. */
internal object VeilMotion {
    const val QUICK_DURATION_MILLIS = 120
    const val STANDARD_DURATION_MILLIS = 180
    const val EMPHASIZED_DURATION_MILLIS = 200

    val standardEasing: Easing = FastOutSlowInEasing
    val enterEasing: Easing = LinearOutSlowInEasing
    val exitEasing: Easing = FastOutLinearInEasing
}
