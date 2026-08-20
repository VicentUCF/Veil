package dev.vicent.veil.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vicent.veil.R
import dev.vicent.veil.ui.theme.LocalVeilPalette

@Composable
internal fun SettingsHeader(
    title: String? = null,
    onBack: () -> Unit,
) {
    val palette = LocalVeilPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(58.dp)
            .background(palette.fieldBackground.copy(alpha = 0.82f))
            .border(width = 0.dp, color = Color.Transparent),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(58.dp)
                .clickable(
                    role = Role.Button,
                    onClickLabel = stringResource(R.string.action_back),
                    onClick = onBack,
                ),
        ) {
            BasicText("<", style = workspaceMonoStyle(palette.accentActive, 14))
        }
        BasicText("> ${title ?: stringResource(R.string.settings_header_title)}", style = workspaceMonoStyle(palette.contentPrimary, 12))
    }
}

@Composable
internal fun SettingsSectionLabel(text: String) {
    val palette = LocalVeilPalette.current
    BasicText(
        text = text,
        style = TextStyle(
            color = palette.contentMuted,
            fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.system,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.5.sp,
        ),
        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 8.dp),
    )
}

@Composable
internal fun SettingsDescription(text: String) {
    BasicText(
        text = text,
        style = workspaceMonoStyle(LocalVeilPalette.current.contentSecondary, 10),
        modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp),
    )
}

@Composable
internal fun AccentChoiceRow(
    label: String,
    color: Color,
    selected: Boolean,
    enabled: Boolean,
    detail: String?,
    onClick: () -> Unit,
) {
    val palette = LocalVeilPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (detail == null) 52.dp else 62.dp)
            .semantics { this.selected = selected }
            .selectable(
                selected = selected,
                enabled = enabled,
                role = Role.RadioButton,
                onClick = onClick,
            )
            .padding(horizontal = 20.dp),
    ) {
        Canvas(modifier = Modifier.size(28.dp)) {
            drawCircle(color = color, radius = 9.dp.toPx())
            if (selected) {
                drawCircle(
                    color = palette.contentPrimary,
                    radius = 13.dp.toPx(),
                    style = Stroke(width = 1.dp.toPx()),
                )
            }
        }
        Column(modifier = Modifier.weight(1f).padding(start = 14.dp)) {
            BasicText(
                label,
                style = TextStyle(
                    color = if (enabled) palette.contentPrimary else palette.contentMuted,
                    fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            detail?.let { BasicText(it, style = workspaceMonoStyle(palette.contentMuted, 8)) }
        }
        if (selected) BasicText("✓", style = workspaceMonoStyle(palette.accentActive, 13))
    }
}

@Composable
internal fun SettingsActionRow(
    title: String,
    detail: String,
    status: String,
    onClick: () -> Unit,
    danger: Boolean = false,
) {
    val palette = LocalVeilPalette.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                role = Role.Button,
                onClickLabel = stringResource(R.string.action_status_title, status, title),
                onClick = onClick,
            )
            .padding(horizontal = 20.dp, vertical = 13.dp),
    ) {
        Column(modifier = Modifier.weight(1f)) {
            BasicText(
                title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = if (danger) palette.error else palette.contentPrimary,
                    fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.content,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            BasicText(
                detail,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = workspaceMonoStyle(palette.contentMuted, 8),
            )
        }
        BasicText(
            "[ ${status.lowercase()} ]",
            style = workspaceMonoStyle(if (danger) palette.error else palette.accentActive, 9),
        )
    }
}
