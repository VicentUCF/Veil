package dev.vicent.veil.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.vicent.veil.ui.theme.LocalVeilPalette

@Composable
fun CozyTile(
    label: String,
    modifier: Modifier = Modifier,
    prominent: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalVeilPalette.current
    val shape = RoundedCornerShape(12.dp)
    val alpha = if (prominent) 0.82f else 0.72f
    val clickModifier = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClickLabel = label, onClick = onClick)
    } else {
        Modifier
    }
    Column(
        modifier = modifier
            .clip(shape)
            .background(Color(0xFF101418).copy(alpha = alpha))
            .border(BorderStroke(1.dp, palette.divider), shape)
            .then(clickModifier)
            .padding(16.dp),
    ) {
        BasicText(
            text = label.uppercase(),
            style = TextStyle(
                color = if (prominent) palette.accentActive else palette.contentMuted,
                fontFamily = FontFamily.Monospace,
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.4.sp,
            ),
        )
        Box(modifier = Modifier.fillMaxWidth().padding(top = 10.dp)) {
            Column(content = content)
        }
    }
}

internal fun workspaceTitleStyle(color: Color, prominent: Boolean = false) = TextStyle(
    color = color,
    fontFamily = FontFamily.SansSerif,
    fontSize = if (prominent) 22.sp else 16.sp,
    fontWeight = FontWeight.Medium,
    lineHeight = if (prominent) 27.sp else 21.sp,
)

internal fun workspaceBodyStyle(color: Color) = TextStyle(
    color = color,
    fontFamily = FontFamily.SansSerif,
    fontSize = 12.sp,
    lineHeight = 17.sp,
)

internal fun workspaceMonoStyle(color: Color, size: Int = 11) = TextStyle(
    color = color,
    fontFamily = FontFamily.Monospace,
    fontSize = size.sp,
    letterSpacing = 0.6.sp,
)
