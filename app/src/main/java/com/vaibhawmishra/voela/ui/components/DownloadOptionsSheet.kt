package com.vaibhawmishra.voela.ui.components
import com.vaibhawmishra.voela.ui.theme.LocalAccent

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vaibhawmishra.voela.R
import com.vaibhawmishra.voela.ui.theme.Background
import com.vaibhawmishra.voela.ui.theme.DownloadGreen
import com.vaibhawmishra.voela.ui.theme.Outline
import com.vaibhawmishra.voela.ui.theme.Surface
import com.vaibhawmishra.voela.ui.theme.TextPrimary
import com.vaibhawmishra.voela.ui.theme.TextSecondary

// Reusable bottom sheet that lets the user pick a download format/quality
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadOptionsSheet(
    onDismiss: () -> Unit,
    onSelect: (DownloadOption) -> Unit,
    title: String = stringResource(R.string.download_title),
    subtitle: String = stringResource(R.string.download_subtitle),
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(),
        containerColor = Surface,
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
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
            Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(LocalAccent.current.base.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Outlined.MusicNote, null, tint = LocalAccent.current.base, modifier = Modifier.size(20.dp))
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(option.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(option.subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Icon(Icons.Outlined.FileDownload, stringResource(R.string.cd_download), tint = DownloadGreen, modifier = Modifier.size(22.dp))
    }
}
