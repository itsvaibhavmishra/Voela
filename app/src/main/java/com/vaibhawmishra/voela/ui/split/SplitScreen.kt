package com.vaibhawmishra.voela.ui.split
import com.vaibhawmishra.voela.ui.theme.LocalAccent

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaibhawmishra.voela.R
import com.vaibhawmishra.voela.ui.components.DeveloperFooter
import com.vaibhawmishra.voela.ui.components.FlowingWaveform
import com.vaibhawmishra.voela.ui.components.OnDeviceFooter
import com.vaibhawmishra.voela.ui.components.SmoothProgressBar
import com.vaibhawmishra.voela.ui.formatDurationSeconds
import com.vaibhawmishra.voela.ui.theme.TextPrimary
import com.vaibhawmishra.voela.ui.theme.TextSecondary
import com.vaibhawmishra.voela.ui.trim.TrimFeature

@Composable
fun SplitScreen(
    uiState: SplitUiState,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // No back button is shown; system back cancels the run
    BackHandler { onCancel() }

    val phase = when {
        uiState.progress < 12 -> 0
        uiState.progress < 92 -> 1
        else -> 2
    }
    val phaseLabel = stringResource(
        when (phase) {
            0 -> R.string.split_phase_preparing
            1 -> R.string.split_phase_separating
            else -> R.string.split_phase_finishing
        },
    )
    val title = stringResource(
        if (uiState.feature == TrimFeature.VOCALS) R.string.split_processing_vocals else R.string.split_processing_audio,
    )

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.weight(1f))

            FlowingWaveform(Modifier.fillMaxWidth().height(150.dp))
            Spacer(Modifier.height(36.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary, textAlign = TextAlign.Center)
            Spacer(Modifier.height(8.dp))
            Text(
                stringResource(R.string.split_processing_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(28.dp))

            Text(phaseLabel, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                for (i in 0..2) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(if (i == phase) LocalAccent.current.base else TextSecondary.copy(alpha = 0.35f)),
                    )
                }
            }
            Spacer(Modifier.height(22.dp))

            val animatedPercent by animateIntAsState(
                targetValue = uiState.progress,
                animationSpec = tween(durationMillis = 900, easing = FastOutSlowInEasing),
                label = "percent",
            )
            Text("$animatedPercent%", style = MaterialTheme.typography.headlineSmall, color = LocalAccent.current.base)
            Spacer(Modifier.height(14.dp))
            SmoothProgressBar(uiState.progress / 100f, Modifier.fillMaxWidth())
            if (!uiState.isComplete) {
                Spacer(Modifier.height(10.dp))
                Text(
                    if (uiState.etaSeconds > 0)
                        stringResource(R.string.split_time_left, formatDurationSeconds(uiState.etaSeconds))
                    else stringResource(R.string.split_almost_done),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }

            Spacer(Modifier.weight(1f))
            OnDeviceFooter()
            DeveloperFooter()
            Spacer(Modifier.height(8.dp))
        }
    }
}
