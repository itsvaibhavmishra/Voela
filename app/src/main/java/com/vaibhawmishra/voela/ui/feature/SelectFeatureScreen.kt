package com.vaibhawmishra.voela.ui.feature
import com.vaibhawmishra.voela.ui.theme.LocalAccent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.ContentCut
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaibhawmishra.voela.R
import com.vaibhawmishra.voela.ui.components.AppHeader
import com.vaibhawmishra.voela.ui.components.DeveloperFooter
import com.vaibhawmishra.voela.ui.components.OnDeviceFooter
import com.vaibhawmishra.voela.ui.theme.InstrumentalTeal
import com.vaibhawmishra.voela.ui.theme.Outline
import com.vaibhawmishra.voela.ui.theme.Surface
import com.vaibhawmishra.voela.ui.theme.TextPrimary
import com.vaibhawmishra.voela.ui.theme.TextSecondary
import com.vaibhawmishra.voela.ui.theme.VocalsAmber

@Composable
fun SelectFeatureScreen(
    uiState: SelectFeatureUiState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSplitVocals: () -> Unit,
    onSplitAudio: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
                Spacer(Modifier.height(12.dp))
                Text(stringResource(R.string.feature_title), style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.feature_subtitle), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                Spacer(Modifier.height(28.dp))

                AudioDetailsCard(uiState, onPlayPause)
                Spacer(Modifier.height(40.dp))

                FeatureCard(
                    icon = Icons.Outlined.Mic,
                    accent = VocalsAmber,
                    title = stringResource(R.string.split_vocals),
                    description = stringResource(R.string.split_vocals_desc),
                    onClick = onSplitVocals,
                )
                Spacer(Modifier.height(16.dp))
                FeatureCard(
                    icon = Icons.Outlined.ContentCut,
                    accent = InstrumentalTeal,
                    title = stringResource(R.string.split_audio),
                    description = stringResource(R.string.split_audio_desc),
                    onClick = onSplitAudio,
                )
            }

            Spacer(Modifier.height(20.dp))
            OnDeviceFooter()
            DeveloperFooter()
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun AudioDetailsCard(uiState: SelectFeatureUiState, onPlayPause: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .border(1.dp, Outline, RoundedCornerShape(18.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(46.dp).clip(CircleShape).background(LocalAccent.current.base).clickable(onClick = onPlayPause),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                if (uiState.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                stringResource(R.string.cd_play),
                tint = LocalAccent.current.onAccent,
                modifier = Modifier.size(26.dp),
            )
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
                Text(metaLine(uiState), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
    }
}

// Join the available facts with middots, skipping any not yet loaded
private fun metaLine(s: SelectFeatureUiState): String =
    listOf(s.duration, s.format, s.size).filter { it.isNotBlank() }.joinToString("  ·  ")

@Composable
private fun FeatureCard(
    icon: ImageVector,
    accent: Color,
    title: String,
    description: String,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Surface)
            .border(1.dp, Outline, RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 32.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(56.dp).clip(RoundedCornerShape(16.dp)).background(accent.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = accent, modifier = Modifier.size(28.dp))
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold), color = TextPrimary)
            Spacer(Modifier.height(5.dp))
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Spacer(Modifier.width(12.dp))
        Box(
            Modifier.size(34.dp).clip(RoundedCornerShape(10.dp)).background(LocalAccent.current.base.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = LocalAccent.current.base, modifier = Modifier.size(18.dp))
        }
    }
}

