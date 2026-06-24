package com.vaibhawmishra.voela.ui.library

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowDropDown
import androidx.compose.material.icons.outlined.AutoDelete
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vaibhawmishra.voela.R
import com.vaibhawmishra.voela.ui.components.AppHeader
import com.vaibhawmishra.voela.ui.components.TypeChip
import com.vaibhawmishra.voela.ui.components.TypeIconTile
import com.vaibhawmishra.voela.ui.home.RecentAudio
import com.vaibhawmishra.voela.ui.theme.Outline
import com.vaibhawmishra.voela.ui.theme.Purple
import com.vaibhawmishra.voela.ui.theme.Surface
import com.vaibhawmishra.voela.ui.theme.SurfaceElevated
import com.vaibhawmishra.voela.ui.theme.TextPrimary
import com.vaibhawmishra.voela.ui.theme.TextSecondary
import com.vaibhawmishra.voela.ui.theme.Warning

@Composable
fun LibraryScreen(
    uiState: LibraryUiState,
    onBack: () -> Unit,
    onItemClick: (RecentAudio) -> Unit,
    onStartSelection: () -> Unit,
    onEnterSelection: (String) -> Unit,
    onToggle: (String) -> Unit,
    onSelectAll: () -> Unit,
    onExitSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onClearAll: () -> Unit,
    onSetExpiry: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    var confirm by remember { mutableStateOf<Confirm?>(null) }
    if (uiState.selectionMode) BackHandler(onBack = onExitSelection)

    Column(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.systemBars)
            .padding(horizontal = 20.dp),
    ) {
        AppHeader(if (uiState.selectionMode) onExitSelection else onBack)
        Spacer(Modifier.height(8.dp))

        if (uiState.selectionMode) {
            SelectionBar(
                count = uiState.selected.size,
                onSelectAll = onSelectAll,
                onDelete = { if (uiState.selected.isNotEmpty()) confirm = Confirm.DeleteSelected },
            )
        } else {
            Text(stringResource(R.string.library_title), style = MaterialTheme.typography.headlineMedium, color = TextPrimary)
            Spacer(Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    stringResource(R.string.library_storage, uiState.totalLabel, uiState.entries.size),
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                )
                Spacer(Modifier.weight(1f))
                if (uiState.entries.isNotEmpty()) {
                    HeaderAction(stringResource(R.string.action_select), Purple, onStartSelection)
                    Spacer(Modifier.width(6.dp))
                    HeaderAction(stringResource(R.string.action_clear_all), Warning) { confirm = Confirm.ClearAll }
                }
            }
            Spacer(Modifier.height(16.dp))
            AutoClearControl(uiState.expiryDays, onSetExpiry)
        }
        Spacer(Modifier.height(20.dp))

        if (uiState.entries.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.library_empty), style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(uiState.entries, key = { it.recent.id }) { entry ->
                    LibraryRow(
                        entry = entry,
                        selectionMode = uiState.selectionMode,
                        selected = entry.recent.id in uiState.selected,
                        onClick = { if (uiState.selectionMode) onToggle(entry.recent.id) else onItemClick(entry.recent) },
                        onLongClick = { if (!uiState.selectionMode) onEnterSelection(entry.recent.id) },
                    )
                }
            }
        }
    }

    confirm?.let { kind ->
        val (title, body, action) = when (kind) {
            Confirm.ClearAll -> Triple(R.string.library_clear_title, R.string.library_clear_body, onClearAll)
            Confirm.DeleteSelected -> Triple(R.string.library_delete_title, R.string.library_delete_body, onDeleteSelected)
        }
        AlertDialog(
            onDismissRequest = { confirm = null },
            containerColor = Surface,
            title = { Text(stringResource(title), color = TextPrimary) },
            text = { Text(stringResource(body), color = TextSecondary) },
            confirmButton = {
                TextButton(onClick = { action(); confirm = null }) {
                    Text(stringResource(R.string.action_delete), color = Warning)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirm = null }) {
                    Text(stringResource(R.string.action_cancel), color = TextSecondary)
                }
            },
        )
    }
}

private enum class Confirm { ClearAll, DeleteSelected }

private val EXPIRY_OPTIONS = listOf(0, 1, 7, 30)

@Composable
private fun expiryLabel(days: Int): String = when (days) {
    0 -> stringResource(R.string.expiry_never)
    1 -> stringResource(R.string.expiry_one_day)
    else -> stringResource(R.string.expiry_n_days, days)
}

@Composable
private fun AutoClearControl(days: Int, onSet: (Int) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Surface)
            .border(1.dp, Outline, RoundedCornerShape(14.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.AutoDelete, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(12.dp))
        Text(stringResource(R.string.library_autoclear), style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
        Spacer(Modifier.weight(1f))
        Box {
            Row(
                Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SurfaceElevated)
                    .clickable { open = true }
                    .padding(start = 14.dp, end = 8.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(expiryLabel(days), style = MaterialTheme.typography.labelLarge, color = Purple)
                Spacer(Modifier.width(2.dp))
                Icon(Icons.Outlined.ArrowDropDown, null, tint = Purple, modifier = Modifier.size(18.dp))
            }
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                shape = RoundedCornerShape(14.dp),
                containerColor = SurfaceElevated,
            ) {
                EXPIRY_OPTIONS.forEach { d ->
                    DropdownMenuItem(
                        text = { Text(expiryLabel(d), color = if (d == days) Purple else TextPrimary) },
                        onClick = { onSet(d); open = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun HeaderAction(label: String, color: androidx.compose.ui.graphics.Color, onClick: () -> Unit) {
    Text(
        label,
        style = MaterialTheme.typography.labelLarge,
        color = color,
        modifier = Modifier.clip(RoundedCornerShape(50)).clickable(onClick = onClick).padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun SelectionBar(count: Int, onSelectAll: () -> Unit, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            stringResource(R.string.library_selected, count),
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
        )
        Spacer(Modifier.weight(1f))
        HeaderAction(stringResource(R.string.action_select_all), Purple, onSelectAll)
        Spacer(Modifier.width(4.dp))
        IconButton(onClick = onDelete) {
            Icon(Icons.Outlined.Delete, stringResource(R.string.action_delete), tint = Warning)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun LibraryRow(
    entry: LibraryEntry,
    selectionMode: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val item = entry.recent
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .border(1.dp, if (selected) Purple else Outline, RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (selectionMode) {
            Icon(
                if (selected) Icons.Outlined.CheckCircle else Icons.Outlined.RadioButtonUnchecked,
                null,
                tint = if (selected) Purple else TextSecondary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
        }
        TypeIconTile(item.type)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(item.title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
            TypeChip(item.type)
            Text("${item.duration}  ·  ${item.timeAgo}", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
        }
        Spacer(Modifier.width(8.dp))
        Text(entry.sizeLabel, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
    }
}
