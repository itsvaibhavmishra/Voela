package com.vaibhawmishra.voela.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.vaibhawmishra.voela.ui.theme.Purple
import com.vaibhawmishra.voela.ui.theme.PurpleDeep
import com.vaibhawmishra.voela.ui.theme.PurpleGlow
import com.vaibhawmishra.voela.ui.theme.SurfaceElevated

// A rounded, gradient-filled progress bar. The fill eases smoothly between the
// coarse per-chunk updates, and a soft highlight sweeps across it so the bar reads
// as "working" even while it waits for the next update.
@Composable
fun SmoothProgressBar(
    progress: Float, // 0f..1f
    modifier: Modifier = Modifier,
) {
    val target = progress.coerceIn(0f, 1f)
    val fill by animateFloatAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
        label = "progressFill",
    )
    val shimmer = rememberInfiniteTransition(label = "shimmer")
    val sweep by shimmer.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 1500, easing = LinearEasing)),
        label = "sweep",
    )

    Box(
        modifier
            .height(12.dp)
            .clip(RoundedCornerShape(percent = 50))
            .background(SurfaceElevated),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fill.coerceAtLeast(0.02f))
                .clip(RoundedCornerShape(percent = 50))
                .background(Brush.horizontalGradient(listOf(PurpleDeep, Purple, PurpleGlow)))
                .drawWithContent {
                    drawContent()
                    val band = size.width * 0.35f
                    val x = (size.width + band) * sweep - band
                    drawRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(Color.Transparent, Color.White.copy(alpha = 0.28f), Color.Transparent),
                            startX = x,
                            endX = x + band,
                        ),
                    )
                },
        )
    }
}
