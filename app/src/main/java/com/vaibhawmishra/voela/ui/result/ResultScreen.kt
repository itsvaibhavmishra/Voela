package com.vaibhawmishra.voela.ui.result

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
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaibhawmishra.voela.R
import com.vaibhawmishra.voela.ui.components.AppHeader
import com.vaibhawmishra.voela.ui.components.DownloadOption
import com.vaibhawmishra.voela.ui.components.DownloadOptionsSheet
import com.vaibhawmishra.voela.ui.components.PlayableWaveform
import com.vaibhawmishra.voela.ui.theme.DownloadGreen
import com.vaibhawmishra.voela.ui.theme.InstrumentalTeal
import com.vaibhawmishra.voela.ui.theme.Outline
import com.vaibhawmishra.voela.ui.theme.Purple
import com.vaibhawmishra.voela.ui.theme.PurpleGlow
import com.vaibhawmishra.voela.ui.theme.Surface
import com.vaibhawmishra.voela.ui.theme.TextPrimary
import com.vaibhawmishra.voela.ui.theme.TextSecondary

private fun clock(ms: Long): String {
    if (ms <= 0) return "0:00"
    val s = ms / 1000
    return "%d:%02d".format(s / 60, s % 60)
}

// "4s" or "1m 45s"
private fun formatElapsed(ms: Long): String {
    val s = ((ms + 500) / 1000).toInt()
    return if (s < 60) "${s}s" else "${s / 60}m ${s % 60}s"
}

@Composable
fun ResultScreen(
    uiState: ResultUiState,
    onBack: () -> Unit,
    onPlayPause: (Int) -> Unit,
    onSeek: (Int, Float) -> Unit,
    onSave: (Int, DownloadOption) -> Unit,
    onMessageShown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var sheetForIndex by remember { mutableIntStateOf(-1) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.message) {
        uiState.message?.let { snackbarHostState.showSnackbar(it); onMessageShown() }
    }

    Box(modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(
            Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.systemBars)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            AppHeader(onBack)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.result_title), style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.result_subtitle), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            if (uiState.elapsedMs > 0) {
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.CheckCircle, null, tint = DownloadGreen, modifier = Modifier.size(15.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        stringResource(R.string.result_completed_in, formatElapsed(uiState.elapsedMs)),
                        style = MaterialTheme.typography.labelMedium,
                        color = DownloadGreen,
                    )
                }
            }
            Spacer(Modifier.height(20.dp))

            SummaryCard(uiState)
            Spacer(Modifier.height(20.dp))

            uiState.stems.forEachIndexed { index, stem ->
                StemCard(
                    stem = stem,
                    accent = if (stem.label == "Vocals") Purple else InstrumentalTeal,
                    icon = if (stem.label == "Vocals") Icons.Outlined.Mic else Icons.Outlined.MusicNote,
                    durationMs = uiState.durationMs,
                    positionMs = uiState.positionFor(index),
                    progress = uiState.progressFor(index),
                    isPlaying = uiState.playingIndex == index,
                    onPlayPause = { onPlayPause(index) },
                    onSeek = { onSeek(index, it) },
                    onSave = { sheetForIndex = index },
                )
                Spacer(Modifier.height(16.dp))
            }

            Spacer(Modifier.height(4.dp))
            LibraryNote()
            Spacer(Modifier.height(8.dp))
        }
        SnackbarHost(
            snackbarHostState,
            Modifier.align(Alignment.BottomCenter).windowInsetsPadding(WindowInsets.systemBars).padding(16.dp),
        )
    }

    if (sheetForIndex >= 0) {
        val index = sheetForIndex
        DownloadOptionsSheet(
            onDismiss = { sheetForIndex = -1 },
            onSelect = { option -> sheetForIndex = -1; onSave(index, option) },
        )
    }
}

@Composable
private fun SummaryCard(uiState: ResultUiState) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Outline, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(uiState.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Schedule, null, tint = TextSecondary, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(5.dp))
                val rate = if (uiState.sampleRate > 0) "%.1f kHz".format(uiState.sampleRate / 1000.0) else ""
                Text(
                    listOf(clock(uiState.durationMs), rate, "${uiState.stems.size} stems").filter { it.isNotBlank() }.joinToString("  ·  "),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
            }
        }
    }
}

@Composable
private fun StemCard(
    stem: StemUi,
    accent: Color,
    icon: ImageVector,
    durationMs: Long,
    positionMs: Long,
    progress: Float,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onSave: () -> Unit,
) {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .border(1.dp, Outline, RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(22.dp))
            }
            Spacer(Modifier.width(12.dp))
            Text(stem.label, style = MaterialTheme.typography.titleMedium, color = TextPrimary, modifier = Modifier.weight(1f))
            when {
                stem.isSaving -> CircularProgressIndicator(Modifier.size(22.dp), strokeWidth = 2.dp, color = DownloadGreen)
                stem.saved -> Icon(Icons.Outlined.CheckCircle, null, tint = DownloadGreen, modifier = Modifier.size(24.dp))
                else -> IconButton(onClick = onSave) {
                    Icon(Icons.Outlined.FileDownload, stringResource(R.string.cd_download), tint = DownloadGreen, modifier = Modifier.size(24.dp))
                }
            }
        }
        Spacer(Modifier.height(14.dp))
        TimeRuler(durationMs)
        Spacer(Modifier.height(6.dp))
        PlayableWaveform(
            bars = stem.waveform,
            progress = progress,
            playedColor = if (stem.label == "Vocals") PurpleGlow else accent,
            pendingColor = accent.copy(alpha = 0.28f),
            onSeek = onSeek,
            modifier = Modifier.fillMaxWidth().height(56.dp),
        )
        Spacer(Modifier.height(12.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).background(accent).clickable(onClick = onPlayPause),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    stringResource(R.string.cd_play),
                    tint = Color.White,
                    modifier = Modifier.size(24.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(clock(positionMs), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
            Spacer(Modifier.weight(1f))
            Text(clock(durationMs), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
        }
    }
}

@Composable
private fun TimeRuler(durationMs: Long) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        for (i in 0..4) {
            Text(clock(durationMs * i / 4), style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
    }
}

@Composable
private fun LibraryNote() {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.CheckCircle, null, tint = TextSecondary, modifier = Modifier.size(13.dp))
        Spacer(Modifier.width(6.dp))
        Text(stringResource(R.string.result_library_title), style = MaterialTheme.typography.labelMedium, color = TextSecondary)
    }
}
