package com.vaibhawmishra.voela.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
