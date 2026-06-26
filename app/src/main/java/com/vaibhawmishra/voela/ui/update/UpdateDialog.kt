package com.vaibhawmishra.voela.ui.update

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaibhawmishra.voela.R
import com.vaibhawmishra.voela.ui.components.FlowingWaveform
import com.vaibhawmishra.voela.ui.components.MarkdownText
import com.vaibhawmishra.voela.ui.components.PrimaryButton
import com.vaibhawmishra.voela.ui.components.SmoothProgressBar
import com.vaibhawmishra.voela.ui.theme.LocalAccent
import com.vaibhawmishra.voela.ui.theme.Surface as SurfaceColor
import com.vaibhawmishra.voela.ui.theme.TextPrimary
import com.vaibhawmishra.voela.ui.theme.TextSecondary
import com.vaibhawmishra.voela.ui.theme.Warning

private fun sizeLabel(bytes: Long): String =
    if (bytes >= 1_000_000) "%.1f MB".format(bytes / 1_000_000.0) else "%d KB".format(bytes / 1000)

// We already head the notes with "What's new" and surface the changelog link separately,
// so drop GitHub's boilerplate "What's Changed" heading and "Full Changelog: <url>" footer.
private fun cleanNotes(s: String): String =
    s.lines().filterNot { line ->
        val t = line.trim().trimStart('#', '*', ' ').lowercase()
        t.startsWith("full changelog") || t.startsWith("what's changed") || t.startsWith("whats changed")
    }.joinToString("\n").trim()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UpdateDialog(
    state: UpdateUiState,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    onProceedInstall: () -> Unit,
    onOpenInstallSettings: () -> Unit,
    onCancelDownload: () -> Unit,
    onDismiss: () -> Unit,
) {
    val visible = state.phase in setOf(
        UpdatePhase.Available, UpdatePhase.Downloading, UpdatePhase.NeedsPermission,
        UpdatePhase.Installing, UpdatePhase.Failed,
    )
    if (!visible) return

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    // Closing the sheet mid-download cancels it (back, scrim, swipe, or the Cancel button);
    // otherwise it just hides.
    val onClose = { if (state.phase == UpdatePhase.Downloading) onCancelDownload() else onDismiss() }
    // Every phase shares one comfortable height so the sheet never resizes as the flow
    // advances; long changelogs scroll within it.
    val phaseHeight = (LocalConfiguration.current.screenHeightDp * 0.46f).coerceIn(360f, 460f).dp

    ModalBottomSheet(
        onDismissRequest = onClose,
        sheetState = sheetState,
        containerColor = SurfaceColor,
        dragHandle = { BottomSheetDefaults.DragHandle() },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(phaseHeight)
                .padding(horizontal = 24.dp)
                .padding(bottom = 8.dp),
        ) {
            when (state.phase) {
                UpdatePhase.Available -> AvailableContent(state, onUpdate, onLater)
                UpdatePhase.Downloading -> DownloadingContent(state, onCancelDownload)
                UpdatePhase.NeedsPermission -> PermissionContent(onProceedInstall, onOpenInstallSettings)
                UpdatePhase.Installing -> InstallingContent(onDismiss)
                UpdatePhase.Failed -> FailedContent(onUpdate, onDismiss)
                else -> Unit
            }
        }
    }
}

@Composable
private fun AvailableContent(state: UpdateUiState, onUpdate: () -> Unit, onLater: () -> Unit) {
    val accent = LocalAccent.current
    val uriHandler = LocalUriHandler.current
    val rel = state.latest
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AppIcon()
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.update_available_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
                    Spacer(Modifier.height(3.dp))
                    Text(
                        "Voela ${rel?.version}" + (rel?.sizeBytes?.takeIf { it > 0 }?.let { "  ·  ${sizeLabel(it)}" } ?: ""),
                        style = MaterialTheme.typography.labelLarge, color = accent.base,
                    )
                    Text(
                        stringResource(R.string.update_currently_on, state.currentVersion),
                        style = MaterialTheme.typography.bodySmall, color = TextSecondary,
                    )
                }
                Spacer(Modifier.width(36.dp)) // room for the close button
            }

            Spacer(Modifier.height(20.dp))
            Text(stringResource(R.string.whats_new_title), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
            Spacer(Modifier.height(10.dp))
            Box(Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState())) {
                MarkdownText(if (rel?.notes.isNullOrBlank()) "Bug fixes and improvements." else cleanNotes(rel!!.notes))
            }
            if (!rel?.pageUrl.isNullOrBlank()) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "${stringResource(R.string.update_view_release)} ↗",
                    style = MaterialTheme.typography.labelLarge, color = accent.base,
                    modifier = Modifier.clickable { uriHandler.openUri(rel!!.pageUrl) },
                )
            }

            Spacer(Modifier.height(16.dp))
            PrimaryButton(text = stringResource(R.string.update_action), onClick = onUpdate, modifier = Modifier.fillMaxWidth())
        }

        // Close = same as "Later": dismiss and don't nag again for this version.
        IconButton(onClick = onLater, modifier = Modifier.align(Alignment.TopEnd).size(32.dp)) {
            Icon(Icons.Outlined.Close, stringResource(R.string.update_later), tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
private fun DownloadingContent(state: UpdateUiState, onCancel: () -> Unit) {
    val accent = LocalAccent.current
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.update_downloading_title), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.update_downloading_sub, state.latest?.version ?: ""),
                style = MaterialTheme.typography.bodySmall, color = TextSecondary,
            )
            Spacer(Modifier.height(20.dp))
            FlowingWaveform(Modifier.fillMaxWidth().height(96.dp), threads = 5)
            Spacer(Modifier.height(18.dp))
            Text("${state.downloadPercent}%", style = MaterialTheme.typography.headlineSmall, color = accent.base)
            Spacer(Modifier.height(12.dp))
            SmoothProgressBar(state.downloadPercent / 100f, Modifier.fillMaxWidth())
            Spacer(Modifier.height(12.dp))
            Text(
                stringResource(R.string.update_keep_open),
                style = MaterialTheme.typography.bodySmall, color = TextSecondary, textAlign = TextAlign.Center,
            )
        }
        TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.BottomCenter)) {
            Text(stringResource(R.string.update_cancel), color = TextSecondary)
        }
    }
}

@Composable
private fun PermissionContent(onProceedInstall: () -> Unit, onOpenInstallSettings: () -> Unit) {
    CenteredMessage(
        title = stringResource(R.string.update_allow_install_title),
        titleColor = TextPrimary,
        body = stringResource(R.string.update_allow_install_body),
    ) {
        TextButton(onClick = onOpenInstallSettings) { Text(stringResource(R.string.update_open_settings), color = LocalAccent.current.base) }
        Spacer(Modifier.weight(1f))
        PrimaryButton(text = stringResource(R.string.update_install), onClick = onProceedInstall, modifier = Modifier.width(150.dp))
    }
}

@Composable
private fun InstallingContent(onDismiss: () -> Unit) {
    CenteredMessage(
        title = stringResource(R.string.update_installing_title),
        titleColor = TextPrimary,
        body = stringResource(R.string.update_installing_body),
    ) {
        Spacer(Modifier.weight(1f))
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_close), color = TextSecondary) }
    }
}

@Composable
private fun FailedContent(onUpdate: () -> Unit, onDismiss: () -> Unit) {
    CenteredMessage(
        title = stringResource(R.string.update_failed_title),
        titleColor = Warning,
        body = stringResource(R.string.update_failed_body),
    ) {
        TextButton(onClick = onDismiss) { Text(stringResource(R.string.update_close), color = TextSecondary) }
        Spacer(Modifier.weight(1f))
        PrimaryButton(text = stringResource(R.string.update_retry), onClick = onUpdate, modifier = Modifier.width(150.dp))
    }
}

// Shared scaffold for the short phases: title + body centred, actions pinned at the bottom.
@Composable
private fun CenteredMessage(
    title: String,
    titleColor: androidx.compose.ui.graphics.Color,
    body: String,
    actions: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            AppIcon()
            Spacer(Modifier.height(18.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, color = titleColor, textAlign = TextAlign.Center)
            Spacer(Modifier.height(10.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.Center)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, content = actions)
    }
}

@Composable
private fun AppIcon(size: androidx.compose.ui.unit.Dp = 56.dp) {
    Image(
        painter = painterResource(R.drawable.ic_app),
        contentDescription = null,
        modifier = Modifier.size(size).clip(RoundedCornerShape(percent = 24)),
    )
}
