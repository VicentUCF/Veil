package dev.vicent.veil.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

/** Official Veil monogram, drawn as geometry so it stays crisp at every density. */
@Composable
fun VeilGlyph(
    color: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Canvas(
        modifier = modifier.then(
            if (contentDescription != null) {
                Modifier.semantics { this.contentDescription = contentDescription }
            } else {
                Modifier
            },
        ),
    ) {
        drawVeilGlyph(color = color, topLeft = Offset.Zero, glyphSize = size)
    }
}

/** Official VEIL wordmark without the monogram. */
@Composable
fun VeilWordmark(
    color: Color,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Canvas(
        modifier = modifier.then(
            if (contentDescription != null) {
                Modifier.semantics { this.contentDescription = contentDescription }
            } else {
                Modifier
            },
        ),
    ) {
        drawVeilWordmark(color = color, topLeft = Offset.Zero, wordSize = size)
    }
}

private fun DrawScope.drawVeilWordmark(
    color: Color,
    topLeft: Offset,
    wordSize: Size,
) {
    val strokeWidth = (wordSize.height * .265f).coerceAtLeast(1f)
    fun point(x: Float, y: Float) = Offset(
        x = topLeft.x + wordSize.width * x,
        y = topLeft.y + wordSize.height * y,
    )

    // V
    drawLine(color, point(.00f, .02f), point(.10f, .98f), strokeWidth, StrokeCap.Square)
    drawLine(color, point(.10f, .98f), point(.20f, .02f), strokeWidth, StrokeCap.Square)
    // E, intentionally built from three bars as in the identity system.
    drawLine(color, point(.31f, .02f), point(.45f, .02f), strokeWidth, StrokeCap.Square)
    drawLine(color, point(.31f, .50f), point(.45f, .50f), strokeWidth, StrokeCap.Square)
    drawLine(color, point(.31f, .98f), point(.45f, .98f), strokeWidth, StrokeCap.Square)
    // I
    drawLine(color, point(.59f, .02f), point(.59f, .98f), strokeWidth, StrokeCap.Square)
    // L
    drawLine(color, point(.75f, .02f), point(.75f, .98f), strokeWidth, StrokeCap.Square)
    drawLine(color, point(.75f, .98f), point(.96f, .98f), strokeWidth, StrokeCap.Square)
}

private fun DrawScope.drawVeilGlyph(
    color: Color,
    topLeft: Offset,
    glyphSize: Size,
) {
    fun x(value: Float) = topLeft.x + glyphSize.width * value
    fun y(value: Float) = topLeft.y + glyphSize.height * value

    val leftWing = Path().apply {
        moveTo(x(.04f), y(.05f))
        lineTo(x(.38f), y(.05f))
        lineTo(x(.42f), y(.12f))
        lineTo(x(.17f), y(.12f))
        lineTo(x(.40f), y(.76f))
        lineTo(x(.40f), y(.89f))
        lineTo(x(.34f), y(.79f))
        close()
    }
    val rightWing = Path().apply {
        moveTo(x(.96f), y(.05f))
        lineTo(x(.62f), y(.05f))
        lineTo(x(.58f), y(.12f))
        lineTo(x(.83f), y(.12f))
        lineTo(x(.60f), y(.76f))
        lineTo(x(.60f), y(.89f))
        lineTo(x(.66f), y(.79f))
        close()
    }
    val centreBlade = Path().apply {
        moveTo(x(.47f), y(.16f))
        lineTo(x(.53f), y(.16f))
        lineTo(x(.53f), y(.91f))
        lineTo(x(.50f), y(.98f))
        lineTo(x(.47f), y(.91f))
        close()
    }

    drawPath(leftWing, color)
    drawPath(rightWing, color)
    drawPath(centreBlade, color)
}
