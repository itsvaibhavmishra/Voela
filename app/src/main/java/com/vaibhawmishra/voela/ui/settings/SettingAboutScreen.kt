package com.vaibhawmishra.voela.ui.settings

import androidx.compose.foundation.Image
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.SystemUpdateAlt
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.vaibhawmishra.voela.R
import com.vaibhawmishra.voela.ui.components.AppHeader
import com.vaibhawmishra.voela.ui.components.DeveloperFooter
import com.vaibhawmishra.voela.ui.components.MarkdownText
import com.vaibhawmishra.voela.ui.theme.DownloadGreen
import com.vaibhawmishra.voela.ui.theme.LocalAccent
import com.vaibhawmishra.voela.ui.theme.Outline
import com.vaibhawmishra.voela.ui.theme.Surface
import com.vaibhawmishra.voela.ui.theme.TextPrimary
import com.vaibhawmishra.voela.ui.theme.TextSecondary
import com.vaibhawmishra.voela.ui.update.UpdateDialog
import com.vaibhawmishra.voela.ui.update.UpdatePhase
import com.vaibhawmishra.voela.ui.update.UpdateUiState

@Composable
fun SettingAboutScreen(
    updateState: UpdateUiState,
    onCheck: () -> Unit,
    onUpdate: () -> Unit,
    onLater: () -> Unit,
    onProceedInstall: () -> Unit,
    onOpenInstallSettings: () -> Unit,
    onCancelDownload: () -> Unit,
    onDismissUpdate: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val accent = LocalAccent.current

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 20.dp),
    ) {
        AppHeader(onBack)

        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(28.dp))

            // Hero: the app icon floating on a soft accent halo. No hard frames or lines.
            Box(
                Modifier.size(140.dp).background(
                    Brush.radialGradient(
                        listOf(accent.glow.copy(alpha = 0.16f), Color.Transparent),
                    ),
                ),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(R.drawable.ic_app),
                    contentDescription = stringResource(R.string.cd_logo),
                    modifier = Modifier.size(88.dp).clip(RoundedCornerShape(percent = 24)),
                )
            }

            Spacer(Modifier.height(14.dp))
            Text(stringResource(R.string.app_name), style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(Modifier.height(4.dp))
            Text(
                stringResource(R.string.app_tagline),
                style = MaterialTheme.typography.bodyMedium, color = accent.base,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                stringResource(R.string.about_version, updateState.currentVersion),
                style = MaterialTheme.typography.bodySmall, color = TextSecondary,
            )
            Spacer(Modifier.height(28.dp))

            // Check for updates
            val checking = updateState.phase == UpdatePhase.Checking
            val upToDate = updateState.phase == UpdatePhase.UpToDate
            AboutRow(
                icon = if (upToDate) Icons.Outlined.CheckCircle else Icons.Outlined.SystemUpdateAlt,
                iconTint = if (upToDate) DownloadGreen else accent.base,
                title = when {
                    checking -> stringResource(R.string.about_checking)
                    upToDate -> stringResource(R.string.about_uptodate)
                    else -> stringResource(R.string.about_check_updates)
                },
                trailing = { if (checking) CircularProgressIndicator(Modifier.size(18.dp), strokeWidth = 2.dp, color = TextSecondary) },
                onClick = { if (!checking) onCheck() },
            )
            Spacer(Modifier.height(12.dp))
            AboutRow(
                icon = Icons.AutoMirrored.Outlined.OpenInNew,
                iconTint = TextSecondary,
                title = stringResource(R.string.about_github),
                subtitle = stringResource(R.string.about_github_desc),
                onClick = { uriHandler.openUri(context.getString(R.string.repo_url)) },
            )

            // What's new in this build
            Spacer(Modifier.height(20.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Surface)
                    .border(1.dp, Outline, RoundedCornerShape(16.dp))
                    .padding(16.dp),
            ) {
                Text(stringResource(R.string.about_whats_new), style = MaterialTheme.typography.titleSmall, color = TextPrimary)
                Spacer(Modifier.height(10.dp))
                MarkdownText(stringResource(R.string.about_changelog))
            }
            Spacer(Modifier.height(20.dp))
        }

        DeveloperFooter()
    }

    UpdateDialog(
        state = updateState,
        onUpdate = onUpdate,
        onLater = onLater,
        onProceedInstall = onProceedInstall,
        onOpenInstallSettings = onOpenInstallSettings,
        onCancelDownload = onCancelDownload,
        onDismiss = onDismissUpdate,
    )
}

@Composable
private fun AboutRow(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String? = null,
    trailing: @Composable () -> Unit = {},
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
            if (subtitle != null) {
                Spacer(Modifier.height(2.dp))
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
            }
        }
        trailing()
    }
}
