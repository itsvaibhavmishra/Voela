package com.vaibhawmishra.voela.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.max
import kotlin.random.Random

// Stable, natural-looking bar heights from a seed: full-amplitude random bars with
// light neighbour smoothing for organic clusters — no global envelope, so it reads
// as an even audio waveform rather than pinching in the middle
fun waveformBars(seed: Int, count: Int = 48): List<Float> {
    val random = Random(seed)
    val raw = FloatArray(count) { 0.25f + 0.75f * random.nextFloat() }
    return List(count) { i ->
        val prev = raw[(i - 1).coerceAtLeast(0)]
        val next = raw[(i + 1).coerceAtMost(count - 1)]
        ((raw[i] * 2f + prev + next) / 4f).coerceIn(0.2f, 1f)
    }
}

// Mirrored bar waveform — the core visual identity, reused at any size or color
@Composable
fun Waveform(
    bars: List<Float>,
    color: Color,
    modifier: Modifier = Modifier,
) {
    val brush = Brush.verticalGradient(
        0f to color.copy(alpha = 0.5f),
        0.5f to color,
        1f to color.copy(alpha = 0.5f),
    )
    Canvas(modifier) {
        val step = size.width / bars.size
        val barWidth = max(1.2f, step * 0.55f)
        bars.forEachIndexed { i, amp ->
            val barHeight = amp * size.height
            val left = i * step + (step - barWidth) / 2f
            val top = (size.height - barHeight) / 2f
            drawRoundRect(
                brush = brush,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }
    }
}

// Playback waveform: bars before the playhead use playedColor, the rest are dimmed.
// Tapping or dragging reports the seek position as a 0..1 fraction.
@Composable
fun PlayableWaveform(
    bars: List<Float>,
    progress: Float,
    playedColor: Color,
    pendingColor: Color,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier
            .pointerInput(Unit) {
                detectTapGestures { offset -> onSeek((offset.x / size.width).coerceIn(0f, 1f)) }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ -> onSeek((change.position.x / size.width).coerceIn(0f, 1f)) }
            },
    ) {
        if (bars.isEmpty()) return@Canvas
        val step = size.width / bars.size
        val barWidth = max(1.2f, step * 0.55f)

        fun drawBars(color: Color) {
            bars.forEachIndexed { i, amp ->
                val barHeight = amp * size.height
                val left = i * step + (step - barWidth) / 2f
                val top = (size.height - barHeight) / 2f
                drawRoundRect(color, Offset(left, top), Size(barWidth, barHeight), CornerRadius(barWidth / 2f))
            }
        }

        drawBars(pendingColor)
        // Clip the played overlay at the exact playhead pixel so it advances smoothly,
        // not a whole bar at a time.
        val playheadX = (progress.coerceIn(0f, 1f) * size.width)
        if (playheadX > 0f) {
            clipRect(right = playheadX) { drawBars(playedColor) }
        }
    }
}
