package com.vaibhawmishra.voela.ui.settings

import android.content.Context
import android.content.Intent
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vaibhawmishra.voela.R
import com.vaibhawmishra.voela.ui.components.AppHeader
import com.vaibhawmishra.voela.ui.components.DeveloperFooter
import com.vaibhawmishra.voela.ui.theme.Outline
import com.vaibhawmishra.voela.ui.theme.Surface
import com.vaibhawmishra.voela.ui.theme.TextPrimary
import com.vaibhawmishra.voela.ui.theme.TextSecondary
import com.vaibhawmishra.voela.ui.theme.VocalsAmber

@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onOpenVocalFormat: () -> Unit,
    onOpenTheme: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 20.dp),
    ) {
        AppHeader(onBack)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
        Spacer(Modifier.height(24.dp))

        GroupLabel(stringResource(R.string.settings_group_appearance))
        SettingRow(
            icon = Icons.Outlined.Palette,
            iconTint = uiState.accent.base,
            title = stringResource(R.string.settings_theme_title),
            subtitle = stringResource(R.string.settings_theme_desc),
            value = uiState.accent.label,
            onClick = onOpenTheme,
        )

        Spacer(Modifier.height(24.dp))
        GroupLabel(stringResource(R.string.settings_group_splitting))
        SettingRow(
            icon = Icons.Outlined.Mic,
            iconTint = VocalsAmber,
            title = stringResource(R.string.settings_vocal_title),
            subtitle = stringResource(R.string.settings_vocal_desc),
            value = uiState.vocalFormat.label,
            onClick = onOpenVocalFormat,
        )

        Spacer(Modifier.height(24.dp))
        GroupLabel(stringResource(R.string.settings_group_storage))
        SettingRow(
            icon = Icons.Outlined.FolderOpen,
            iconTint = TextSecondary,
            title = stringResource(R.string.settings_open_folder_title),
            subtitle = stringResource(R.string.settings_open_folder_desc),
            trailing = Icons.AutoMirrored.Outlined.OpenInNew,
            onClick = {
                if (!openVoelaFolder(context)) {
                    Toast.makeText(context, R.string.settings_open_folder_failed, Toast.LENGTH_SHORT).show()
                }
            },
        )
        Spacer(Modifier.weight(1f))
        DeveloperFooter()
    }
}

@Composable
private fun GroupLabel(text: String) {
    Text(text, style = MaterialTheme.typography.titleSmall, color = TextSecondary)
    Spacer(Modifier.height(10.dp))
}

@Composable
private fun SettingRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    value: String? = null,
    trailing: ImageVector = Icons.AutoMirrored.Outlined.KeyboardArrowRight,
    onClick: () -> Unit,
) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Surface)
            .border(1.dp, Outline, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, null, tint = iconTint, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary)
            Spacer(Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        if (value != null) {
            Spacer(Modifier.width(10.dp))
            Text(value, style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = TextAlign.End)
        }
        Icon(trailing, null, tint = TextSecondary, modifier = Modifier.size(20.dp))
    }
}

// Best-effort: open the system file manager at Music/Voela. There is no single intent
// that works everywhere — most OEM file managers (OnePlus, Samsung, MIUI) handle a
// directory ACTION_VIEW, while some builds of DocumentsUI use the BROWSE action — so we
// try several, fall back to the Music root, then report failure to the caller.
private fun openVoelaFolder(context: Context): Boolean {
    val authority = "com.android.externalstorage.documents"
    val docIds = listOf("primary:Music/Voela", "primary:Music")
    val candidates = buildList {
        for (id in docIds) {
            val docUri = DocumentsContract.buildDocumentUri(authority, id)
            val treeUri = DocumentsContract.buildTreeDocumentUri(authority, id)
            add(Intent(Intent.ACTION_VIEW).setDataAndType(docUri, DocumentsContract.Document.MIME_TYPE_DIR))
            add(Intent("android.provider.action.BROWSE").setData(treeUri))
        }
    }
    for (intent in candidates) {
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        try {
            context.startActivity(intent)
            return true
        } catch (_: Exception) {
            // try the next strategy
        }
    }
    return false
}
