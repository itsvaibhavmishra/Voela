package com.vaibhawmishra.voela.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import com.vaibhawmishra.voela.ui.theme.InstrumentalTeal
import com.vaibhawmishra.voela.ui.theme.Purple
import com.vaibhawmishra.voela.ui.theme.PurpleDeep
import com.vaibhawmishra.voela.ui.theme.PurpleGlow
import com.vaibhawmishra.voela.ui.theme.ShareBlue
import kotlin.math.PI
import kotlin.math.sin

private const val TWO_PI = (2.0 * PI).toFloat()

// A flowing "voice thread" loader: several sine threads that converge to a point at
// each edge, swell at the centre, and continuously flow. Each thread carries a constant
// phase offset (so they fan out near both edges, not just the end) and alternates flow
// direction, which weaves them together. Drawn twice — a soft wide stroke for the glow
// and a bright thin one on top — so it shines without a (API 31+) blur. Cheap: a handful
// of sampled paths per frame.
@Composable
fun FlowingWaveform(
    modifier: Modifier = Modifier,
    threads: Int = 6,
    colors: List<Color> = listOf(PurpleGlow, ShareBlue, Purple, InstrumentalTeal, PurpleDeep),
) {
    val transition = rememberInfiniteTransition(label = "flow")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = TWO_PI,
        animationSpec = infiniteRepeatable(tween(4200, easing = LinearEasing), RepeatMode.Restart),
        label = "phase",
    )
    // Gentle breathing of the overall amplitude so the swell feels alive
    val swell by transition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1900, easing = LinearEasing), RepeatMode.Reverse),
        label = "swell",
    )

    Canvas(modifier) {
        val w = size.width
        val cy = size.height / 2f
        val samples = 160
        for (t in 0 until threads) {
            val path = Path()
            val freq = 2.0f + t * 0.5f
            val amp = size.height * 0.34f * (1f - t * 0.10f) * swell
            val spatial = t * 1.15f // constant per-thread phase → fans out near both edges, incl. the start
            val travel = if (t % 2 == 0) phase else -phase // alternate flow direction so threads weave
            for (s in 0..samples) {
                val frac = s.toFloat() / samples
                val x = w * frac
                val envelope = sin(PI * frac).toFloat() // 0 at both edges → points, max at centre
                val y = cy + amp * envelope * sin(freq * frac * TWO_PI + spatial + travel)
                if (s == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            val color = colors[t % colors.size]
            drawPath(path, color.copy(alpha = 0.16f), style = Stroke(width = 11f, cap = StrokeCap.Round))
            drawPath(path, color, style = Stroke(width = 2.5f, cap = StrokeCap.Round))
        }
        // Soft core highlight where the threads bunch up
        drawCircle(PurpleGlow.copy(alpha = 0.12f), radius = size.height * 0.14f, center = Offset(w / 2f, cy))
    }
}
