package com.vaibhawmishra.voela.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs
import kotlin.math.max

private const val MIN_GAP = 0.02f

// Waveform with a draggable trim selection: bars inside [start, end] are highlighted,
// the rest are muted, and two handle bars mark the bounds. Grab the nearer handle to drag.
@Composable
fun TrimWaveform(
    bars: List<Float>,
    startFraction: Float,
    endFraction: Float,
    progressFraction: Float,
    selectedColor: Color,
    mutedColor: Color,
    handleColor: Color,
    onRangeChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val start by rememberUpdatedState(startFraction)
    val end by rememberUpdatedState(endFraction)
    var dragging by remember { mutableIntStateOf(0) } // 1 = start handle, 2 = end handle

    fun apply(fraction: Float) {
        if (dragging == 1) onRangeChange(fraction.coerceIn(0f, end - MIN_GAP), end)
        else if (dragging == 2) onRangeChange(start, fraction.coerceIn(start + MIN_GAP, 1f))
    }

    Canvas(
        modifier.pointerInput(Unit) {
            detectDragGestures(
                onDragStart = { offset ->
                    val f = (offset.x / size.width).coerceIn(0f, 1f)
                    dragging = if (abs(f - start) <= abs(f - end)) 1 else 2
                    apply(f)
                },
                onDragEnd = { dragging = 0 },
                onDragCancel = { dragging = 0 },
            ) { change, _ ->
                apply((change.position.x / size.width).coerceIn(0f, 1f))
            }
        },
    ) {
        if (bars.isEmpty()) return@Canvas
        val w = size.width
        val h = size.height
        val startX = start * w
        val endX = end * w
        val step = w / bars.size
        val barWidth = max(1.5f, step * 0.55f)

        bars.forEachIndexed { i, amp ->
            val cx = i * step + step / 2f
            val barHeight = amp * h
            val left = i * step + (step - barWidth) / 2f
            val top = (h - barHeight) / 2f
            drawRoundRect(
                color = if (cx in startX..endX) selectedColor else mutedColor,
                topLeft = Offset(left, top),
                size = Size(barWidth, barHeight),
                cornerRadius = CornerRadius(barWidth / 2f),
            )
        }

        val px = progressFraction.coerceIn(0f, 1f) * w
        if (px > 0f) {
            drawRect(Color.White.copy(alpha = 0.85f), topLeft = Offset(px - 1f, 0f), size = Size(2f, h))
        }

        val handleW = 6f
        listOf(startX, endX).forEach { x ->
            drawRoundRect(
                color = handleColor,
                topLeft = Offset((x - handleW / 2f).coerceIn(0f, w - handleW), 0f),
                size = Size(handleW, h),
                cornerRadius = CornerRadius(handleW / 2f),
            )
        }
    }
}
