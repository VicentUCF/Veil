package dev.vicent.veil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import dev.vicent.veil.ui.theme.LocalVeilPalette

/** Veil's Rofi/Alacritty-inspired modal surface. */
@Composable
fun RofiDialog(
    title: String,
    onDismiss: () -> Unit,
    actions: @Composable RowScope.() -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalVeilPalette.current
    val shape = RoundedCornerShape(7.dp)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.48f))
                .imePadding()
                .padding(horizontal = 20.dp, vertical = 48.dp),
        ) {
            Column(
                modifier = modifier
                    .fillMaxWidth()
                    .widthIn(max = 520.dp)
                    .heightIn(max = 720.dp)
                    .clip(shape)
                    .background(palette.dialogBackground.copy(alpha = 0.98f))
                    .border(1.dp, palette.divider, shape),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.fieldBackground.copy(alpha = 0.82f))
                        .border(width = 0.dp, color = Color.Transparent)
                        .padding(horizontal = 16.dp, vertical = 13.dp),
                ) {
                    BasicText(">", style = workspaceMonoStyle(palette.accentActive, 12))
                    BasicText(
                        title.lowercase().replace(' ', '_'),
                        style = workspaceMonoStyle(palette.contentPrimary, 12),
                        modifier = Modifier.padding(start = 8.dp),
                    )
                }
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .fillMaxWidth()
                        .padding(16.dp),
                    content = content,
                )
                Row(
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(palette.fieldBackground.copy(alpha = 0.55f))
                        .padding(horizontal = 10.dp, vertical = 7.dp),
                    content = actions,
                )
            }
        }
    }
}

@Composable
fun RofiAction(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
    accessibilityLabel: String = label,
) {
    val palette = LocalVeilPalette.current
    BasicText(
        text = "[ ${label.lowercase()} ]",
        style = workspaceMonoStyle(
            when {
                !enabled -> palette.contentMuted
                danger -> palette.error
                else -> palette.accentActive
            },
            10,
        ),
        modifier = modifier
            .clip(RoundedCornerShape(3.dp))
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClickLabel = accessibilityLabel,
                onClick = onClick,
            )
            .padding(horizontal = 7.dp, vertical = 7.dp),
    )
}

@Composable
fun RofiBody(text: String, modifier: Modifier = Modifier) {
    BasicText(
        text = text,
        style = workspaceMonoStyle(LocalVeilPalette.current.contentSecondary, 10),
        modifier = modifier,
    )
}
