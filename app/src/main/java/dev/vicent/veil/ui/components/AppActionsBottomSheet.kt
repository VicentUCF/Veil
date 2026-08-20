package dev.vicent.veil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vicent.veil.R
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.ui.theme.LocalVeilPalette

@Composable
fun AppActionsBottomSheet(
    app: LauncherApp,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
    contextLabel: String? = null,
    onReplaceInContext: (() -> Unit)? = null,
    onRemoveFromContext: (() -> Unit)? = null,
) {
    RofiDialog(
        title = stringResource(R.string.app_actions_title),
        onDismiss = onDismiss,
        actions = { RofiAction(stringResource(R.string.action_close), onDismiss) },
    ) {
        AppSheetHeader(app = app)
        SheetDivider()
        AppSheetAction(marker = ">", label = stringResource(R.string.action_open), onClick = onOpen)
        if (contextLabel != null && onReplaceInContext != null && onRemoveFromContext != null) {
            AppSheetAction(
                marker = "↺",
                label = stringResource(R.string.app_change_in_context, contextLabel),
                onClick = onReplaceInContext,
            )
            AppSheetAction(
                marker = "−",
                label = stringResource(R.string.app_remove_from_context, contextLabel),
                labelColor = LocalVeilPalette.current.error,
                onClick = onRemoveFromContext,
            )
            SheetDivider()
        }
        AppSheetAction(marker = "i", label = stringResource(R.string.app_info), onClick = onAppInfo)
        AppSheetAction(
            marker = "x",
            label = stringResource(R.string.app_uninstall),
            labelColor = LocalVeilPalette.current.error,
            onClick = onUninstall,
        )
    }
}

@Composable
private fun AppSheetHeader(app: LauncherApp) {
    val palette = LocalVeilPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LauncherAppIcon(app = app, size = 38.dp)
        Column(modifier = Modifier.padding(start = 18.dp)) {
            BasicText(
                text = app.label,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = palette.contentPrimary,
                    fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.system,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            BasicText(
                text = app.packageName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = palette.contentMuted,
                    fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.system,
                    fontSize = 11.sp,
                ),
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }
}

@Composable
private fun AppSheetAction(
    marker: String,
    label: String,
    onClick: () -> Unit,
    labelColor: Color = LocalVeilPalette.current.contentPrimary,
) {
    val palette = LocalVeilPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clickable(
                role = Role.Button,
                onClickLabel = label,
                onClick = onClick,
            )
            .padding(horizontal = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = marker,
            style = TextStyle(
                color = if (labelColor == palette.error) palette.error else palette.accentActive,
                fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.system,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
            ),
            modifier = Modifier.size(28.dp),
        )
        BasicText(
            text = label,
            style = TextStyle(
                color = labelColor,
                fontFamily = dev.vicent.veil.ui.theme.LocalVeilTypography.current.system,
                fontSize = 12.sp,
            ),
            modifier = Modifier.padding(start = 18.dp),
        )
    }
}

@Composable
private fun SheetDivider() {
    val palette = LocalVeilPalette.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(palette.divider),
    )
    Spacer(modifier = Modifier.height(6.dp))
}
