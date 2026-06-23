package com.vaibhawmishra.voela.ui.youtube

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Cancel
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.vaibhawmishra.voela.R
import com.vaibhawmishra.voela.ui.components.PrimaryButton
import com.vaibhawmishra.voela.ui.components.Waveform
import com.vaibhawmishra.voela.ui.components.waveformBars
import com.vaibhawmishra.voela.ui.theme.Background
import com.vaibhawmishra.voela.ui.theme.DownloadGreen
import com.vaibhawmishra.voela.ui.theme.Outline
import com.vaibhawmishra.voela.ui.theme.Purple
import com.vaibhawmishra.voela.ui.theme.Surface
import com.vaibhawmishra.voela.ui.theme.TextPrimary
import com.vaibhawmishra.voela.ui.theme.TextSecondary
import com.vaibhawmishra.voela.ui.theme.YouTubeRed

@Composable
fun YouTubeUrlScreen(
    uiState: YouTubeUiState,
    onBack: () -> Unit,
    onUrlChange: (String) -> Unit,
    onExtract: () -> Unit,
    onContinue: () -> Unit,
    onClearResult: () -> Unit,
    onClearRecents: () -> Unit,
    onOpenLink: (RecentLink) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showDownloadSheet by remember { mutableStateOf(false) }

    // Ask for notification permission (13+) so extraction progress/completion can show, then extract
    val context = LocalContext.current
    val notificationPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {}
    val extractAction = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        onExtract()
    }

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 20.dp),
    ) {
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Header(onBack)
            Spacer(Modifier.height(8.dp))
            Text(stringResource(R.string.youtube_title), style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.youtube_subtitle), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(20.dp))

            LinkCard(
                url = uiState.url,
                onUrlChange = onUrlChange,
                status = uiState.status,
                progress = uiState.progress,
                result = uiState.result,
                onExtract = extractAction,
                onContinue = onContinue,
                onClearResult = onClearResult,
                onDownload = { showDownloadSheet = true },
            )

            if (uiState.status == ExtractionStatus.Idle) {
                Spacer(Modifier.height(20.dp))
                HowItWorksCard()
            }

            Spacer(Modifier.height(24.dp))
            RecentLinksSection(recents = uiState.recentLinks, onClearAll = onClearRecents, onOpenLink = onOpenLink)
        }

        Spacer(Modifier.height(16.dp))
        FooterNote()
        Spacer(Modifier.height(8.dp))
    }

    if (showDownloadSheet) {
        DownloadOptionsSheet(onDismiss = { showDownloadSheet = false }, onSelect = { showDownloadSheet = false })
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(
            painter = painterResource(R.drawable.logo_voela),
            contentDescription = stringResource(R.string.cd_logo),
            modifier = Modifier.height(30.dp),
        )
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Outlined.ArrowBack, stringResource(R.string.cd_back), tint = TextPrimary)
        }
    }
}

@Composable
private fun LinkCard(
    url: String,
    onUrlChange: (String) -> Unit,
    status: ExtractionStatus,
    progress: Int,
    result: ExtractedAudio?,
    onExtract: () -> Unit,
    onContinue: () -> Unit,
    onClearResult: () -> Unit,
    onDownload: () -> Unit,
) {
    val editable = status != ExtractionStatus.Processing
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Surface)
            .border(1.dp, Outline, RoundedCornerShape(22.dp))
            .padding(18.dp),
    ) {
        Text(stringResource(R.string.youtube_link_label), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = url,
            onValueChange = onUrlChange,
            enabled = editable,
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text(stringResource(R.string.youtube_url_hint), style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Outlined.Link, null, tint = Purple, modifier = Modifier.size(20.dp)) },
            trailingIcon = {
                if (url.isNotEmpty() && editable) {
                    IconButton(onClick = { onUrlChange("") }) {
                        Icon(Icons.Outlined.Cancel, stringResource(R.string.cd_clear), tint = TextSecondary, modifier = Modifier.size(20.dp))
                    }
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Purple,
                unfocusedBorderColor = Outline,
                disabledBorderColor = Outline,
                focusedContainerColor = Background,
                unfocusedContainerColor = Background,
                disabledContainerColor = Background,
                cursorColor = Purple,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                disabledTextColor = TextSecondary,
                focusedPlaceholderColor = TextSecondary,
                unfocusedPlaceholderColor = TextSecondary,
            ),
        )
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(R.string.youtube_support_note),
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
        )
        Spacer(Modifier.height(16.dp))

        PrimaryButton(
            text = stringResource(R.string.extract_audio),
            onClick = onExtract,
            enabled = url.isNotBlank() && status != ExtractionStatus.Processing,
        )

        if (status == ExtractionStatus.Processing) {
            Spacer(Modifier.height(16.dp))
            ProcessingIndicator(progress)
        }
        if (status == ExtractionStatus.Done && result != null) {
            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    result.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(8.dp))
                IconButton(onClick = onClearResult, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Outlined.Close, stringResource(R.string.cd_remove_audio), tint = TextSecondary, modifier = Modifier.size(18.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
            ResultRow(result, onDownload = onDownload)
            Spacer(Modifier.height(16.dp))
            PrimaryButton(text = stringResource(R.string.continue_action), onClick = onContinue)
        }
    }
}

@Composable
private fun ProcessingIndicator(progress: Int) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Background)
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp, color = Purple)
        Spacer(Modifier.width(14.dp))
        Text(stringResource(R.string.extracting_audio), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        if (progress > 0) {
            Spacer(Modifier.weight(1f))
            Text("$progress%", style = MaterialTheme.typography.labelLarge, color = TextSecondary)
        }
    }
}

@Composable
private fun ResultRow(result: ExtractedAudio, onDownload: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Background)
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(Purple),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.PlayArrow, stringResource(R.string.cd_play), tint = Color.White, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.width(14.dp))
        Waveform(
            bars = result.waveform,
            color = Purple,
            modifier = Modifier.weight(1f).height(40.dp),
        )
        Spacer(Modifier.width(10.dp))
        IconButton(onClick = onDownload) {
            Icon(Icons.Outlined.FileDownload, stringResource(R.string.cd_download), tint = DownloadGreen, modifier = Modifier.size(22.dp))
        }
    }
}

@Composable
private fun HowItWorksCard() {
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(Surface)
            .border(1.dp, Outline, RoundedCornerShape(22.dp))
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
    ) {
        Text(stringResource(R.string.how_it_works), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        HowItWorksRow(Icons.Outlined.Link, stringResource(R.string.hiw_1_title), stringResource(R.string.hiw_1_desc))
        HowItWorksRow(Icons.Outlined.GraphicEq, stringResource(R.string.hiw_2_title), stringResource(R.string.hiw_2_desc))
        HowItWorksRow(Icons.Outlined.CheckCircle, stringResource(R.string.hiw_3_title), stringResource(R.string.hiw_3_desc))
    }
}

@Composable
private fun HowItWorksRow(icon: ImageVector, title: String, desc: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Purple.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = Purple, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(desc, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@Composable
private fun RecentLinksSection(
    recents: List<RecentLink>,
    onClearAll: () -> Unit,
    onOpenLink: (RecentLink) -> Unit,
) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.recent_links), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.weight(1f))
        if (recents.isNotEmpty()) {
            Text(
                stringResource(R.string.clear_all),
                style = MaterialTheme.typography.labelLarge,
                color = Purple,
                modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClearAll).padding(horizontal = 8.dp, vertical = 4.dp),
            )
        }
    }
    Spacer(Modifier.height(12.dp))
    if (recents.isEmpty()) {
        RecentLinksEmpty()
    } else {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            recents.take(3).forEach { RecentLinkRow(it, onOpenLink) }
        }
    }
}

@Composable
private fun RecentLinkRow(link: RecentLink, onOpenLink: (RecentLink) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Outline, RoundedCornerShape(16.dp))
            .clickable { onOpenLink(link) }
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(30.dp).clip(RoundedCornerShape(8.dp)).background(YouTubeRed),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(link.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Spacer(Modifier.height(2.dp))
            Text(link.duration, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Spacer(Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Outlined.OpenInNew, stringResource(R.string.cd_open_link), tint = Purple, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun RecentLinksEmpty() {
    Box(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Outline, RoundedCornerShape(16.dp))
            .padding(vertical = 28.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(stringResource(R.string.recent_links_empty), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
    }
}

@Composable
private fun FooterNote() {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Outline, RoundedCornerShape(16.dp))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Purple.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.VerifiedUser, null, tint = Purple, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column {
            Text(stringResource(R.string.tos_title), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(stringResource(R.string.tos_desc), style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DownloadOptionsSheet(onDismiss: () -> Unit, onSelect: (DownloadOption) -> Unit) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(stringResource(R.string.download_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(stringResource(R.string.download_subtitle), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            Spacer(Modifier.height(18.dp))
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                downloadOptions.forEach { DownloadOptionRow(it) { onSelect(it) } }
            }
        }
    }
}

@Composable
private fun DownloadOptionRow(option: DownloadOption, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Background)
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(Purple.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.MusicNote, null, tint = Purple, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text("${option.format} · ${option.quality}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(option.size, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Icon(Icons.Outlined.FileDownload, stringResource(R.string.cd_download), tint = DownloadGreen, modifier = Modifier.size(22.dp))
    }
}

// format is the audio file type shown to the user (MP3, M4A, WAV)
data class DownloadOption(val format: String, val quality: String, val size: String)

private val downloadOptions = listOf(
    DownloadOption("MP3", "320 kbps · High", "~9 MB"),
    DownloadOption("MP3", "192 kbps · Standard", "~5.4 MB"),
    DownloadOption("M4A", "256 kbps · AAC", "~7.2 MB"),
    DownloadOption("WAV", "Lossless", "~52 MB"),
)
