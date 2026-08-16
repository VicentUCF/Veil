package dev.vicent.veil.ui.components

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import dev.vicent.veil.launcher.model.HomeTextTone
import dev.vicent.veil.launcher.model.HomeTextWeight

@Immutable
internal data class CurrentHomeAppearance(
    val primary: Color,
    val secondary: Color,
    val muted: Color,
    val contentWeight: FontWeight,
    val clockWeight: FontWeight,
    val quickButtonBackground: Color,
)

internal val LocalCurrentHomeAppearance = staticCompositionLocalOf {
    CurrentHomeAppearance(
        primary = Color(0xFFE8E9E7),
        secondary = Color(0xFFBEC2C3),
        muted = Color(0xFF747C81),
        contentWeight = FontWeight.Light,
        clockWeight = FontWeight.ExtraLight,
        quickButtonBackground = Color(0xFF0C1013),
    )
}

internal fun resolveCurrentHomeAppearance(
    tone: HomeTextTone,
    weight: HomeTextWeight,
): CurrentHomeAppearance {
    val colors = when (tone) {
        HomeTextTone.LIGHT -> Triple(
            Color(0xFFE8E9E7),
            Color(0xFFBEC2C3),
            Color(0xFF747C81),
        )
        HomeTextTone.DARK -> Triple(
            Color(0xFF171A1C),
            Color(0xFF30373A),
            Color(0xFF596166),
        )
    }
    val contentWeight = when (weight) {
        HomeTextWeight.LIGHT -> FontWeight.Light
        HomeTextWeight.REGULAR -> FontWeight.Normal
        HomeTextWeight.SEMIBOLD -> FontWeight.SemiBold
    }
    val clockWeight = when (weight) {
        HomeTextWeight.LIGHT -> FontWeight.ExtraLight
        HomeTextWeight.REGULAR -> FontWeight.Normal
        HomeTextWeight.SEMIBOLD -> FontWeight.SemiBold
    }
    return CurrentHomeAppearance(
        primary = colors.first,
        secondary = colors.second,
        muted = colors.third,
        contentWeight = contentWeight,
        clockWeight = clockWeight,
        quickButtonBackground = if (tone == HomeTextTone.LIGHT) {
            Color(0xFF0C1013)
        } else {
            Color(0xFFF1EEE8)
        },
    )
}

@Composable
internal fun homeSmallMonoStyle(color: Color) = TextStyle(
    color = color,
    fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.system,
    fontSize = 10.sp,
    fontWeight = LocalCurrentHomeAppearance.current.contentWeight,
    letterSpacing = 1.sp,
)
