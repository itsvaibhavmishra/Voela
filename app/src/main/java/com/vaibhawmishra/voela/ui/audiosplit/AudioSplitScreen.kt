package com.vaibhawmishra.voela.ui.audiosplit

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaibhawmishra.voela.R
import com.vaibhawmishra.voela.ui.components.AppHeader
import com.vaibhawmishra.voela.ui.components.PrimaryButton
import com.vaibhawmishra.voela.ui.components.Waveform
import com.vaibhawmishra.voela.ui.formatClock
import com.vaibhawmishra.voela.ui.formatDurationSeconds
import com.vaibhawmishra.voela.ui.theme.Background
import com.vaibhawmishra.voela.ui.theme.InstrumentalTeal
import com.vaibhawmishra.voela.ui.theme.Outline
import com.vaibhawmishra.voela.ui.theme.Surface
import com.vaibhawmishra.voela.ui.theme.SurfaceElevated
import com.vaibhawmishra.voela.ui.theme.TextPrimary
import com.vaibhawmishra.voela.ui.theme.TextSecondary
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val accent = InstrumentalTeal

@Composable
fun AudioSplitScreen(
    uiState: AudioSplitUiState,
    onBack: () -> Unit,
    onUnitChange: (SplitUnit) -> Unit,
    onStepUp: () -> Unit,
    onStepDown: () -> Unit,
    onPlayPause: () -> Unit,
    onPlayClip: (Int) -> Unit,
    onSplit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 20.dp),
    ) {
        AppHeader(onBack)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.audiosplit_title), style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(stringResource(R.string.audiosplit_subtitle), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.height(20.dp))

        // Clip length picker
        Text(stringResource(R.string.audiosplit_length), style = MaterialTheme.typography.titleSmall, color = TextSecondary)
        Spacer(Modifier.height(10.dp))
        UnitToggle(uiState.unit, onUnitChange)
        Spacer(Modifier.height(14.dp))
        Stepper(uiState.value, uiState.unit, onStepDown, onStepUp)
        Spacer(Modifier.height(20.dp))

        // Stats
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Stat(stringResource(R.string.audiosplit_total), formatClock(uiState.totalMs), Modifier.weight(1f))
            Stat(stringResource(R.string.audiosplit_parts), uiState.clips.size.toString(), Modifier.weight(1f))
            Stat(
                stringResource(R.string.audiosplit_last),
                uiState.clips.lastOrNull()?.let { formatDurationSeconds((it.durationMs / 1000).toInt()) } ?: "—",
                Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(24.dp))

        // Preview
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.audiosplit_preview), style = MaterialTheme.typography.titleSmall, color = TextSecondary)
            Spacer(Modifier.width(8.dp))
            Text(stringResource(R.string.audiosplit_tap_hint), style = MaterialTheme.typography.labelSmall, color = TextSecondary.copy(alpha = 0.7f))
        }
        Spacer(Modifier.height(10.dp))
        SplitPreview(uiState, onPlayClip)
        Spacer(Modifier.height(14.dp))
        PlayControl(uiState, onPlayPause)

        Spacer(Modifier.weight(1f))
        Text(
            stringResource(R.string.audiosplit_save_note),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        Spacer(Modifier.height(12.dp))
        PrimaryButton(text = stringResource(R.string.audiosplit_action), onClick = onSplit, enabled = uiState.clips.isNotEmpty())
        Spacer(Modifier.height(8.dp))
    }
}

@Composable
private fun UnitToggle(unit: SplitUnit, onChange: (SplitUnit) -> Unit) {
    Row(
        Modifier
            .clip(RoundedCornerShape(50))
            .background(SurfaceElevated)
            .padding(3.dp),
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        UnitChip(stringResource(R.string.audiosplit_seconds), unit == SplitUnit.SECONDS) { onChange(SplitUnit.SECONDS) }
        UnitChip(stringResource(R.string.audiosplit_minutes), unit == SplitUnit.MINUTES) { onChange(SplitUnit.MINUTES) }
    }
}

@Composable
private fun UnitChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = if (selected) Background else TextSecondary,
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (selected) accent else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 7.dp),
    )
}

@Composable
private fun Stepper(value: Int, unit: SplitUnit, onDown: () -> Unit, onUp: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        StepButton(Icons.Outlined.Remove, onDown)
        Row(verticalAlignment = Alignment.Bottom) {
            Text(value.toString(), style = MaterialTheme.typography.displaySmall, color = TextPrimary)
            Spacer(Modifier.width(6.dp))
            Text(
                stringResource(if (unit == SplitUnit.MINUTES) R.string.audiosplit_min else R.string.audiosplit_sec),
                style = MaterialTheme.typography.titleMedium,
                color = accent,
                modifier = Modifier.padding(bottom = 6.dp),
            )
        }
        StepButton(Icons.Outlined.Add, onUp)
    }
}

// A round button that fires once on tap, and on press-and-hold repeats with acceleration.
@Composable
private fun StepButton(icon: ImageVector, onStep: () -> Unit) {
    val scope = rememberCoroutineScope()
    Box(
        Modifier
            .size(44.dp)
            .clip(CircleShape)
            .background(SurfaceElevated)
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onStep() // immediate first step
                        val job = scope.launch {
                            delay(350) // hold threshold before auto-repeat
                            var interval = 130L
                            while (true) {
                                onStep()
                                delay(interval)
                                interval = (interval * 82 / 100).coerceAtLeast(30) // speed up
                            }
                        }
                        try { awaitRelease() } finally { job.cancel() }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
    }
}

@Composable
private fun PlayControl(uiState: AudioSplitUiState, onPlayPause: () -> Unit) {
    val clip = uiState.clips.getOrNull(uiState.playingClip)
    val label = if (clip == null) stringResource(R.string.audiosplit_whole)
                else stringResource(R.string.audiosplit_clip_n, uiState.playingClip + 1)
    val current = if (clip == null) uiState.positionMs else (uiState.positionMs - clip.startMs).coerceAtLeast(0)
    val total = clip?.durationMs ?: uiState.totalMs

    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center, modifier = Modifier.fillMaxWidth()) {
        Box(
            Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(accent)
                .clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                null,
                tint = Background,
                modifier = Modifier.size(26.dp),
            )
        }
        Spacer(Modifier.width(12.dp))
        Text(
            "$label  ·  ${formatClock(current)} / ${formatClock(total)}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
    }
}

@Composable
private fun Stat(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Outline, RoundedCornerShape(16.dp))
            .padding(vertical = 14.dp, horizontal = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(value, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(label, style = MaterialTheme.typography.labelSmall, color = TextSecondary, textAlign = TextAlign.Center)
    }
}

@Composable
private fun SplitPreview(uiState: AudioSplitUiState, onPlayClip: (Int) -> Unit) {
    val total = uiState.totalMs.coerceAtLeast(1)
    Box(
        Modifier
            .fillMaxWidth()
            .height(96.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(Background)
            .border(1.dp, Outline, RoundedCornerShape(16.dp))
            .padding(2.dp)
            .drawWithContent {
                drawContent()
                // Divider at each clip boundary
                uiState.clips.dropLast(1).forEach { clip ->
                    val x = size.width * (clip.endMs.toFloat() / total)
                    drawLine(accent, Offset(x, 0f), Offset(x, size.height), strokeWidth = 2.dp.toPx())
                }
                // Playback progress line
                if (uiState.isPlaying || uiState.positionMs > 0) {
                    val px = size.width * (uiState.positionMs.toFloat() / total)
                    drawLine(Color.White, Offset(px, 0f), Offset(px, size.height), strokeWidth = 1.5.dp.toPx())
                }
            },
    ) {
        if (uiState.waveform.isNotEmpty()) {
            Waveform(uiState.waveform, accent.copy(alpha = 0.5f), Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 12.dp))
        }
        // Tappable, weighted region per clip — tap to preview that clip; active one is tinted
        Row(Modifier.fillMaxSize()) {
            uiState.clips.forEachIndexed { i, clip ->
                Box(
                    Modifier
                        .weight(clip.durationMs.toFloat().coerceAtLeast(1f))
                        .fillMaxHeight()
                        .background(if (uiState.playingClip == i) accent.copy(alpha = 0.18f) else Color.Transparent)
                        .clickable { onPlayClip(i) },
                )
            }
        }
    }
}
