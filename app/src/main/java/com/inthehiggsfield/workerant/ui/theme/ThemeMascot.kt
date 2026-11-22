package com.inthehiggsfield.workerant.ui.theme

import androidx.compose.animation.core.withInfiniteAnimationFrameMillis
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawTransform
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * One shape in a mascot drawing. [path] uses SVG path syntax on a 48x48 canvas.
 * Colors: 0 = none, 1-5 = the current theme's primary/secondary/tertiary/onBackground/background,
 * otherwise a fixed 0xAARRGGBB color. [phase] (0..1) offsets the animation so copies don't move in sync.
 */
class MascotLayer(
    val path: String,
    val fill: Long = 0L,
    val stroke: Long = 0L,
    val strokeWidth: Float = 0f,
    val anims: List<MascotAnim> = emptyList(),
    val phase: Float = 0f,
    val evenOdd: Boolean = false,
    val alpha: Float = 1f,
)

/** Looping animations. Periods are in milliseconds; pivots are in 48x48 canvas coordinates. */
sealed interface MascotAnim {
    /** Full rotation (gears, reels, a revolver around a finger). */
    data class Spin(val periodMs: Int, val px: Float, val py: Float, val clockwise: Boolean) : MascotAnim
    /** Rock back and forth by ±[degrees] (flowers, pendulums, needles). */
    data class Sway(val degrees: Float, val periodMs: Int, val px: Float, val py: Float) : MascotAnim
    /** Float up and down. */
    data class Bob(val dy: Float, val periodMs: Int) : MascotAnim
    /** Grow and shrink (heartbeat, glow, flames). */
    data class Pulse(val min: Float, val max: Float, val periodMs: Int, val px: Float, val py: Float) : MascotAnim
    /** Uneven brightness, like a candle or neon sign. */
    data class Flicker(val periodMs: Int) : MascotAnim
    /** Drift upward while fading in then out (steam, bubbles). */
    data class Rise(val distance: Float, val periodMs: Int) : MascotAnim
    /** Drift downward while fading and swaying (leaves, petals, snow). */
    data class Fall(val distance: Float, val periodMs: Int, val swayDegrees: Float, val px: Float, val py: Float) : MascotAnim
    /** Sparkle: shrink to nothing and pop back. */
    data class Twinkle(val periodMs: Int, val px: Float, val py: Float) : MascotAnim
    /** Squash horizontally around x = [px] (butterfly and bat wings). */
    data class Flap(val periodMs: Int, val px: Float) : MascotAnim
}

/** The current theme's little animated mascot (Worker Ant for the basic color themes). */
@Composable
fun ThemeMascot(theme: AppTheme, modifier: Modifier = Modifier, size: Dp = 64.dp) {
    val layers = remember(theme) { mascotFor(theme) }
    val paths = remember(layers) {
        layers.map { layer ->
            PathParser().parsePathString(layer.path).toPath().apply {
                if (layer.evenOdd) fillType = PathFillType.EvenOdd
            }
        }
    }
    val colors = MaterialTheme.colorScheme

    // Shared animation clock in milliseconds. Wraps every hour so float precision stays fine.
    var time by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(Unit) {
        val start = withInfiniteAnimationFrameMillis { it }
        while (true) {
            withInfiniteAnimationFrameMillis { time = ((it - start) % 3_600_000L).toFloat() }
        }
    }

    Canvas(modifier.size(size)) {
        val s = this.size.minDimension / 48f
        scale(s, s, pivot = Offset.Zero) {
            layers.forEachIndexed { i, layer ->
                val alpha = (layer.alpha * layer.anims.fold(1f) { acc, a -> acc * a.alphaAt(time, layer.phase) })
                    .coerceIn(0f, 1f)
                if (alpha <= 0.01f) return@forEachIndexed
                withTransform({ layer.anims.forEach { it.applyAt(this, time, layer.phase) } }) {
                    if (layer.fill != 0L) drawPath(paths[i], resolve(layer.fill, colors), alpha)
                    if (layer.stroke != 0L) drawPath(
                        paths[i], resolve(layer.stroke, colors), alpha,
                        style = Stroke(layer.strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
                    )
                }
            }
        }
    }
}

private fun resolve(c: Long, colors: ColorScheme): Color = when (c) {
    1L -> colors.primary
    2L -> colors.secondary
    3L -> colors.tertiary
    4L -> colors.onBackground
    5L -> colors.background
    else -> Color(c)
}

/** 0..1 position within the animation's cycle. */
private fun cycle(time: Float, periodMs: Int, phase: Float): Float = ((time / periodMs) + phase) % 1f

private fun wave(c: Float): Float = sin(2f * PI.toFloat() * c)

private fun MascotAnim.applyAt(t: DrawTransform, time: Float, phase: Float) {
    when (this) {
        is MascotAnim.Spin -> {
            val c = cycle(time, periodMs, phase)
            t.rotate(if (clockwise) 360f * c else -360f * c, Offset(px, py))
        }
        is MascotAnim.Sway -> t.rotate(degrees * wave(cycle(time, periodMs, phase)), Offset(px, py))
        is MascotAnim.Bob -> t.translate(top = dy * wave(cycle(time, periodMs, phase)))
        is MascotAnim.Pulse -> {
            val k = min + (max - min) * (0.5f + 0.5f * wave(cycle(time, periodMs, phase)))
            t.scale(k, k, Offset(px, py))
        }
        is MascotAnim.Rise -> t.translate(top = -distance * cycle(time, periodMs, phase))
        is MascotAnim.Fall -> {
            val c = cycle(time, periodMs, phase)
            t.translate(top = distance * c)
            t.rotate(swayDegrees * wave(c * 2f), Offset(px, py))
        }
        is MascotAnim.Twinkle -> {
            val k = 0.3f + 0.7f * (0.5f + 0.5f * wave(cycle(time, periodMs, phase)))
            t.scale(k, k, Offset(px, py))
        }
        is MascotAnim.Flap -> {
            val k = 0.35f + 0.65f * abs(cos(PI.toFloat() * cycle(time, periodMs, phase)))
            t.scale(k, 1f, Offset(px, 24f))
        }
        is MascotAnim.Flicker -> Unit
    }
}

private fun MascotAnim.alphaAt(time: Float, phase: Float): Float = when (this) {
    is MascotAnim.Flicker -> {
        val c = cycle(time, periodMs, phase)
        (0.78f + 0.14f * wave(c) + 0.08f * wave(c * 3.7f % 1f))
    }
    is MascotAnim.Rise -> sin(PI.toFloat() * cycle(time, periodMs, phase))
    is MascotAnim.Fall -> sin(PI.toFloat() * cycle(time, periodMs, phase))
    is MascotAnim.Twinkle -> 0.4f + 0.6f * (0.5f + 0.5f * wave(cycle(time, periodMs, phase)))
    else -> 1f
}
