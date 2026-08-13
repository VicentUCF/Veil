package dev.vicent.veil.ui.components

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.text.BasicText
import dev.vicent.veil.launcher.model.LauncherContext
import dev.vicent.veil.ui.theme.LocalVeilPalette
import java.util.Date
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun TopBar(
    contexts: List<LauncherContext>,
    activeContextIndex: Int,
    onContextSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalVeilPalette.current
    val time by rememberSystemTime()

    Row(
        modifier = modifier
            .fillMaxWidth()
            .windowInsetsPadding(
                WindowInsets.displayCutout.only(
                    WindowInsetsSides.Horizontal,
                ),
            )
            .height(30.dp)
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            contexts.forEachIndexed { index, context ->
                ContextIndicator(
                    kind = context.kind,
                    label = context.label,
                    isActive = index == activeContextIndex,
                    onClick = { onContextSelected(index) },
                )
            }
        }

        BasicText(
            text = time,
            style = TextStyle(
                color = palette.contentPrimary,
                fontFamily = FontFamily.Monospace,
                fontSize = 12.sp,
                letterSpacing = 0.5.sp,
            ),
        )
    }
}

@Composable
private fun rememberSystemTime(): androidx.compose.runtime.State<String> {
    val context = LocalContext.current
    val timeFormatter = DateFormat.getTimeFormat(context)

    return produceState(initialValue = timeFormatter.format(Date()), timeFormatter) {
        while (isActive) {
            delay(15_000)
            value = timeFormatter.format(Date())
        }
    }
}
