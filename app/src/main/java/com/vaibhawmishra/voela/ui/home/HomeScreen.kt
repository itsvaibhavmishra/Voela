package com.vaibhawmishra.voela.ui.home
import com.vaibhawmishra.voela.ui.theme.LocalAccent

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.SmartDisplay
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vaibhawmishra.voela.R
import com.vaibhawmishra.voela.ui.components.DeveloperFooter
import com.vaibhawmishra.voela.ui.components.VoelaLogo
import com.vaibhawmishra.voela.ui.components.TypeChip
import com.vaibhawmishra.voela.ui.components.TypeIconTile
import com.vaibhawmishra.voela.ui.theme.Background
import com.vaibhawmishra.voela.ui.theme.VoelaTheme
import com.vaibhawmishra.voela.ui.theme.InstrumentalTeal
import com.vaibhawmishra.voela.ui.theme.Outline
import com.vaibhawmishra.voela.ui.theme.Surface
import com.vaibhawmishra.voela.ui.theme.SurfaceElevated
import com.vaibhawmishra.voela.ui.theme.TextPrimary
import com.vaibhawmishra.voela.ui.theme.TextSecondary
import com.vaibhawmishra.voela.ui.theme.YouTubeRed
import com.vaibhawmishra.voela.ui.theme.VocalsAmber
import com.vaibhawmishra.voela.ui.theme.Warning

@Composable
fun HomeScreen(
    recents: List<RecentAudio>,
    modifier: Modifier = Modifier,
    onChooseFile: () -> Unit = {},
    onYouTubeUrl: () -> Unit = {},
    onOpenLibrary: () -> Unit = {},
    onOpenSettings: () -> Unit = {},
    onRecentClick: (RecentAudio) -> Unit = {},
    onRecentDelete: (RecentAudio) -> Unit = {},
) {
    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 20.dp),
    ) {
        TopBar(onOpenSettings, onOpenLibrary)
        Spacer(Modifier.height(6.dp))
        Text(
            stringResource(R.string.app_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(24.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            ActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.Folder,
                title = stringResource(R.string.choose_audio_file),
                description = stringResource(R.string.choose_audio_file_desc),
                onClick = onChooseFile,
            )
            ActionCard(
                modifier = Modifier.weight(1f),
                icon = Icons.Outlined.SmartDisplay,
                title = stringResource(R.string.youtube_url),
                description = stringResource(R.string.youtube_url_desc),
                onClick = onYouTubeUrl,
            )
        }
        Spacer(Modifier.height(44.dp))
        RecentsHeader(showViewAll = recents.isNotEmpty(), onViewAll = onOpenLibrary)
        Spacer(Modifier.height(12.dp))
        if (recents.isEmpty()) {
            EmptyRecents(Modifier.weight(1f))
        } else {
            LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                items(recents, key = { it.id }) { item ->
                    RecentRow(item, onClick = { onRecentClick(item) }, onDelete = { onRecentDelete(item) })
                }
            }
        }
        DeveloperFooter()
    }
}

@Composable
private fun TopBar(onOpenSettings: () -> Unit, onOpenLibrary: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        VoelaLogo(modifier = Modifier.height(42.dp))
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onOpenSettings) {
            Icon(Icons.Outlined.Settings, stringResource(R.string.cd_settings), tint = TextSecondary)
        }
        IconButton(onClick = onOpenLibrary) {
            Icon(Icons.Outlined.FolderOpen, stringResource(R.string.cd_library), tint = TextSecondary)
        }
    }
}

@Composable
private fun ActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier
            .height(200.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(Surface)
            .border(1.dp, Brush.verticalGradient(listOf(Outline, Color.Transparent)), RoundedCornerShape(22.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
    ) {
        Box(
            Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(SurfaceElevated)
                .border(1.dp, Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.12f), Color.Transparent)), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, null, tint = TextPrimary, modifier = Modifier.size(26.dp))
        }
        Spacer(Modifier.height(14.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
            color = TextPrimary,
        )
        Spacer(Modifier.height(3.dp))
        Text(description, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
            Box(
                Modifier.size(40.dp).clip(CircleShape).border(1.dp, Color.White.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = LocalAccent.current.base, modifier = Modifier.size(22.dp))
            }
        }
    }
}

@Composable
private fun RecentsHeader(showViewAll: Boolean, onViewAll: () -> Unit) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(stringResource(R.string.recents), style = MaterialTheme.typography.titleLarge, color = TextPrimary)
        Spacer(Modifier.weight(1f))
        if (showViewAll) {
            Row(
                Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onViewAll).padding(horizontal = 6.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(stringResource(R.string.view_all), style = MaterialTheme.typography.labelLarge, color = TextSecondary)
                Spacer(Modifier.width(4.dp))
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, null, tint = TextSecondary, modifier = Modifier.size(16.dp))
            }
        }
    }
}

@Composable
private fun RecentRow(item: RecentAudio, onClick: () -> Unit, onDelete: () -> Unit) {
    var menuExpanded by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Surface)
            .border(1.dp, Outline, RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TypeIconTile(item.type)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                item.title,
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            TypeChip(item.type)
            Text("${item.duration}  ·  ${item.timeAgo}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Spacer(Modifier.width(4.dp))
        Box {
            IconButton(onClick = { menuExpanded = true }) {
                Icon(
                    Icons.Outlined.MoreVert,
                    stringResource(R.string.cd_more_options),
                    tint = TextSecondary,
                    modifier = Modifier.size(20.dp),
                )
            }
            DropdownMenu(
                expanded = menuExpanded,
                onDismissRequest = { menuExpanded = false },
                offset = DpOffset(0.dp, 6.dp),
                shape = RoundedCornerShape(16.dp),
                containerColor = SurfaceElevated,
                border = BorderStroke(1.dp, Outline),
                tonalElevation = 0.dp,
                shadowElevation = 12.dp,
            ) {
                RecentMenuItem(stringResource(R.string.action_open), Icons.AutoMirrored.Outlined.OpenInNew, LocalAccent.current.base, TextPrimary) {
                    menuExpanded = false
                    onClick()
                }
                HorizontalDivider(Modifier.padding(horizontal = 12.dp), color = Outline)
                RecentMenuItem(stringResource(R.string.action_delete), Icons.Outlined.Delete, Warning, Warning) {
                    menuExpanded = false
                    onDelete()
                }
            }
        }
    }
}

@Composable
private fun RecentMenuItem(
    label: String,
    icon: ImageVector,
    iconTint: Color,
    textColor: Color,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = { Text(label, style = MaterialTheme.typography.bodyMedium, color = textColor) },
        leadingIcon = { Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp)) },
        onClick = onClick,
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
    )
}


@Composable
private fun EmptyRecents(modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(Icons.Outlined.GraphicEq, null, tint = TextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(40.dp))
        Spacer(Modifier.height(12.dp))
        Text(stringResource(R.string.recents_empty_title), style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(Modifier.height(4.dp))
        Text(
            stringResource(R.string.recents_empty_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center,
        )
    }
}

// Temporary sample data — replaced by a real data source when functionality lands
internal val sampleRecents = listOf(
    RecentAudio("1", "Faded - Alan Walker", "03:45", "2h ago", ProcessType.VOCAL_REMOVAL),
    RecentAudio("2", "Imagine Dragons - Believer", "04:18", "1d ago", ProcessType.AUDIO_SPLIT),
    RecentAudio("3", "The Weeknd - Blinding Lights", "03:02", "2d ago", ProcessType.VOCAL_REMOVAL),
    RecentAudio("4", "Coldplay - Yellow", "05:21", "3d ago", ProcessType.AUDIO_SPLIT),
)

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun HomeScreenPreview() {
    VoelaTheme { HomeScreen(recents = sampleRecents) }
}

@Preview(showBackground = true, backgroundColor = 0xFF0D0D0D)
@Composable
private fun HomeScreenEmptyPreview() {
    VoelaTheme { HomeScreen(recents = emptyList()) }
}
