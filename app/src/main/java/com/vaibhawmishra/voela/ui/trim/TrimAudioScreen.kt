package com.vaibhawmishra.voela.ui.trim
import com.vaibhawmishra.voela.ui.theme.LocalAccent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaibhawmishra.voela.R
import com.vaibhawmishra.voela.ui.formatDurationSeconds
import com.vaibhawmishra.voela.ui.components.AppHeader
import com.vaibhawmishra.voela.ui.components.PrimaryButton
import com.vaibhawmishra.voela.ui.components.TrimWaveform
import com.vaibhawmishra.voela.ui.components.Waveform
import com.vaibhawmishra.voela.ui.theme.Background
import com.vaibhawmishra.voela.ui.theme.Outline
import com.vaibhawmishra.voela.ui.theme.Surface
import com.vaibhawmishra.voela.ui.theme.SurfaceElevated
import com.vaibhawmishra.voela.ui.theme.TextPrimary
import com.vaibhawmishra.voela.ui.theme.TextSecondary

private const val STEP_MS = 1000L

@Composable
fun TrimAudioScreen(
    uiState: TrimAudioUiState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onRangeChange: (Float, Float) -> Unit,
    onStartStep: (Long) -> Unit,
    onEndStep: (Long) -> Unit,
    onEngineChange: (SeparationEngine) -> Unit,
    onProceed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actionLabel = stringResource(
        if (uiState.feature == TrimFeature.VOCALS) R.string.split_vocals else R.string.split_audio,
    )
    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 20.dp),
        ) {
            Column(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                AppHeader(onBack)
                Spacer(Modifier.height(8.dp))
                Text(stringResource(R.string.trim_title), style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.trim_subtitle), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(20.dp))

                AudioCard(uiState)
                Spacer(Modifier.height(40.dp))

                TimeRuler(uiState.durationMs)
                Spacer(Modifier.height(6.dp))
                TrimWaveform(
                    bars = uiState.waveform,
                    startFraction = uiState.startFraction,
                    endFraction = uiState.endFraction,
                    progressFraction = uiState.progressFraction,
                    selectedColor = LocalAccent.current.glow,
                    mutedColor = LocalAccent.current.base.copy(alpha = 0.28f),
                    handleColor = LocalAccent.current.base,
                    onRangeChange = onRangeChange,
                    modifier = Modifier.fillMaxWidth().height(120.dp),
                )
                Spacer(Modifier.height(16.dp))
                PlayButton(uiState.isPlaying, onPlayPause)
                Spacer(Modifier.height(12.dp))

                Text(stringResource(R.string.trim_select_range), style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                RangeSlider(
                    value = uiState.startFraction..uiState.endFraction,
                    onValueChange = { onRangeChange(it.start, it.endInclusive) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = LocalAccent.current.base,
                        activeTrackColor = LocalAccent.current.base,
                        inactiveTrackColor = SurfaceElevated,
                    ),
                )
                Spacer(Modifier.height(16.dp))

                Row(verticalAlignment = Alignment.Bottom) {
                    TimeField(
                        label = stringResource(R.string.trim_start_time),
                        value = TrimAudioViewModel.formatPrecise(uiState.startMs),
                        onUp = { onStartStep(STEP_MS) },
                        onDown = { onStartStep(-STEP_MS) },
                        modifier = Modifier.weight(1f),
                    )
                    Box(Modifier.height(56.dp).width(40.dp), contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.Link, stringResource(R.string.cd_linked), tint = LocalAccent.current.base, modifier = Modifier.size(18.dp))
                    }
                    TimeField(
                        label = stringResource(R.string.trim_end_time),
                        value = TrimAudioViewModel.formatPrecise(uiState.endMs),
                        onUp = { onEndStep(STEP_MS) },
                        onDown = { onEndStep(-STEP_MS) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(14.dp))
                InfoNote()
                Spacer(Modifier.height(24.dp))

                // Engine choice only applies to Split Vocals (separation); Split Audio just cuts.
                if (uiState.feature == TrimFeature.VOCALS) {
                    Text(stringResource(R.string.trim_quality), style = MaterialTheme.typography.titleSmall, color = TextSecondary)
                    Spacer(Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        EngineOption(
                            title = stringResource(R.string.engine_fast),
                            description = stringResource(R.string.engine_fast_desc),
                            selected = uiState.engine == SeparationEngine.FAST,
                            enabled = true,
                            onClick = { onEngineChange(SeparationEngine.FAST) },
                            modifier = Modifier.weight(1f),
                        )
                        EngineOption(
                            title = stringResource(R.string.engine_best),
                            description = stringResource(R.string.engine_best_desc),
                            selected = uiState.engine == SeparationEngine.BEST,
                            enabled = true,
                            onClick = { onEngineChange(SeparationEngine.BEST) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }

            if (uiState.feature == TrimFeature.VOCALS && uiState.estimateSeconds > 0) {
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(Icons.Outlined.Schedule, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.trim_estimate, formatDurationSeconds(uiState.estimateSeconds)),
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary,
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            PrimaryButton(text = actionLabel, onClick = onProceed, enabled = uiState.endMs > uiState.startMs)
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun AudioCard(uiState: TrimAudioUiState) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Outline, RoundedCornerShape(16.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(width = 64.dp, height = 44.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Background),
            contentAlignment = Alignment.Center,
        ) {
            if (uiState.waveform.isNotEmpty()) {
                Waveform(uiState.waveform, LocalAccent.current.base, Modifier.fillMaxSize().padding(horizontal = 8.dp, vertical = 12.dp))
            }
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                uiState.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                Text(
                    listOf(uiState.totalDuration, uiState.format, uiState.sampleRate).filter { it.isNotBlank() }.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun TimeRuler(durationMs: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in 0..4) {
            Text(
                TrimAudioViewModel.formatClock(durationMs * i / 4),
                style = MaterialTheme.typography.labelSmall,
                color = TextSecondary,
            )
        }
    }
}

@Composable
private fun PlayButton(isPlaying: Boolean, onPlayPause: () -> Unit) {
    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(56.dp).clip(CircleShape).background(LocalAccent.current.base).clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                stringResource(R.string.cd_play),
                tint = LocalAccent.current.onAccent,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun TimeField(
    label: String,
    value: String,
    onUp: () -> Unit,
    onDown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Spacer(Modifier.height(6.dp))
        Row(
            Modifier
                .fillMaxWidth()
                .height(56.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Background)
                .border(1.dp, Outline, RoundedCornerShape(14.dp))
                .padding(start = 14.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
            Column {
                Icon(
                    Icons.Outlined.KeyboardArrowUp,
                    stringResource(R.string.cd_step_up),
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)).clickable(onClick = onUp),
                )
                Icon(
                    Icons.Outlined.KeyboardArrowDown,
                    stringResource(R.string.cd_step_down),
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp).clip(RoundedCornerShape(6.dp)).clickable(onClick = onDown),
                )
            }
        }
    }
}

@Composable
private fun EngineOption(
    title: String,
    description: String,
    selected: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (selected) LocalAccent.current.base.copy(alpha = 0.12f) else Background)
            .border(1.dp, if (selected) LocalAccent.current.base else Outline, RoundedCornerShape(14.dp))
            .then(if (enabled) Modifier.clickable(onClick = onClick) else Modifier)
            .alpha(if (enabled) 1f else 0.55f)
            .padding(14.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = if (enabled) TextPrimary else TextSecondary,
                modifier = Modifier.weight(1f),
            )
            if (selected) Icon(Icons.Outlined.CheckCircle, null, tint = LocalAccent.current.base, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.height(3.dp))
        Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
    }
}

@Composable
private fun InfoNote() {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Outlined.Info, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
        Spacer(Modifier.width(8.dp))
        Text(
            stringResource(R.string.trim_linked_note),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
    }
}
