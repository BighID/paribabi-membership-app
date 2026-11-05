package bighid.romatazok.adherentapp.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bighid.romatazok.adherentapp.ui.events.GOLD
import bighid.romatazok.adherentapp.ui.events.NAVY_LIGHT
import bighid.romatazok.adherentapp.ui.events.ORANGE

/** How much of the ring the arc covers. */
private const val SWEEP = 300f

/**
 * The one wait of the app: a thin ring with rounded ends turning on a light track. On a light ground, orange then gold
 * then navy, the way the PIN's dots go; on an orange button ([onAccent]), white.
 */
@Composable
fun Loader(
    modifier: Modifier = Modifier,
    size: Dp = 24.dp,
    onAccent: Boolean = false,
) {
    val angle by rememberInfiniteTransition().animateFloat(0f, 360f, infiniteRepeatable(tween(1_000, easing = LinearEasing), RepeatMode.Restart))
    val track = if (onAccent) Color.White.copy(alpha = 0.25f) else NAVY_LIGHT.copy(alpha = 0.10f)
    // Sampled around the ring: the tail starts orange, the head arrives navy.
    val arc =
        if (onAccent) {
            Brush.sweepGradient(0f to Color.White.copy(alpha = 0.35f), SWEEP / 360f to Color.White, 1f to Color.White.copy(alpha = 0.35f))
        } else {
            Brush.sweepGradient(0f to ORANGE, 0.45f to GOLD, SWEEP / 360f to NAVY_LIGHT, 1f to ORANGE)
        }
    Canvas(modifier.size(size)) {
        val stroke = this.size.minDimension * 0.11f
        val inset = stroke / 2
        val box = Size(this.size.width - stroke, this.size.height - stroke)
        drawArc(track, 0f, 360f, useCenter = false, topLeft = Offset(inset, inset), size = box, style = Stroke(stroke))
        rotate(angle) {
            drawArc(arc, 0f, SWEEP, useCenter = false, topLeft = Offset(inset, inset), size = box, style = Stroke(stroke, cap = StrokeCap.Round))
        }
    }
}
