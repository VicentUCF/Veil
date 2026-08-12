package dev.vicent.veil.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vicent.veil.launcher.model.LauncherApp
import dev.vicent.veil.ui.theme.LocalVeilPalette

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppActionsBottomSheet(
    app: LauncherApp,
    onDismiss: () -> Unit,
    onOpen: () -> Unit,
    onAppInfo: () -> Unit,
    onUninstall: () -> Unit,
) {
    val palette = LocalVeilPalette.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = palette.drawerBackground,
        contentColor = palette.contentPrimary,
        scrimColor = Color.Black.copy(alpha = 0.58f),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 3.dp)
                    .background(
                        color = palette.contentMuted,
                        shape = RoundedCornerShape(2.dp),
                    ),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(bottom = 12.dp),
        ) {
            AppSheetHeader(app = app)
            SheetDivider()
            AppSheetAction(
                marker = "↗",
                label = "Abrir",
                onClick = onOpen,
            )
            AppSheetAction(
                marker = "i",
                label = "Información de la aplicación",
                onClick = onAppInfo,
            )
            AppSheetAction(
                marker = "×",
                label = "Desinstalar",
                labelColor = palette.error,
                onClick = onUninstall,
            )
        }
    }
}

@Composable
private fun AppSheetHeader(app: LauncherApp) {
    val palette = LocalVeilPalette.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 18.dp),
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
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                ),
            )
            BasicText(
                text = app.packageName,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = TextStyle(
                    color = palette.contentMuted,
                    fontFamily = FontFamily.Monospace,
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
            .height(58.dp)
            .clickable(
                role = Role.Button,
                onClickLabel = label,
                onClick = onClick,
            )
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BasicText(
            text = marker,
            style = TextStyle(
                color = if (labelColor == palette.error) palette.error else palette.accentActive,
                fontFamily = FontFamily.Monospace,
                fontSize = 17.sp,
                fontWeight = FontWeight.Medium,
            ),
            modifier = Modifier.size(28.dp),
        )
        BasicText(
            text = label,
            style = TextStyle(
                color = labelColor,
                fontFamily = FontFamily.SansSerif,
                fontSize = 16.sp,
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
