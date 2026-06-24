package com.vaibhawmishra.voela.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vaibhawmishra.voela.data.library.LibraryStore
import com.vaibhawmishra.voela.ui.formatBytes
import com.vaibhawmishra.voela.ui.home.RecentAudio
import com.vaibhawmishra.voela.ui.home.toRecentAudio
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryEntry(val recent: RecentAudio, val sizeLabel: String)

data class LibraryUiState(
    val entries: List<LibraryEntry> = emptyList(),
    val totalLabel: String = "",
    val selectionMode: Boolean = false,
    val selected: Set<String> = emptySet(),
    val expiryDays: Int = LibraryStore.DEFAULT_EXPIRY_DAYS,
)

// Full list of kept actions with per-item + total storage, plus selection/delete.
class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val store = LibraryStore(application)
    private data class Sel(val mode: Boolean = false, val ids: Set<String> = emptySet())
    private val selection = MutableStateFlow(Sel())

    val uiState: StateFlow<LibraryUiState> =
        combine(store.items, selection, store.expiryDays) { items, sel, expiry ->
            val entries = items.map { LibraryEntry(it.toRecentAudio(), formatBytes(store.folderSize(it.id))) }
            val total = items.sumOf { store.folderSize(it.id) }
            val valid = sel.ids.intersect(items.map { it.id }.toSet())
            LibraryUiState(entries, formatBytes(total), sel.mode, valid, expiry)
        }.flowOn(Dispatchers.IO).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    fun startSelection() { selection.value = Sel(true, emptySet()) }
    fun enterSelection(id: String) { selection.value = Sel(true, setOf(id)) }

    fun toggle(id: String) {
        val ids = selection.value.ids
        val next = if (id in ids) ids - id else ids + id
        selection.value = Sel(next.isNotEmpty(), next)
    }

    fun selectAll() {
        selection.value = Sel(true, uiState.value.entries.map { it.recent.id }.toSet())
    }

    fun exitSelection() { selection.value = Sel() }

    fun deleteOne(id: String) = viewModelScope.launch { store.delete(id) }

    fun deleteSelected() {
        val ids = selection.value.ids
        exitSelection()
        viewModelScope.launch { ids.forEach { store.delete(it) } }
    }

    fun clearAll() {
        exitSelection()
        viewModelScope.launch { store.clear() }
    }

    fun setExpiry(days: Int) = viewModelScope.launch { store.setExpiryDays(days) }

    companion object {
        val Factory = viewModelFactory {
            initializer { LibraryViewModel(this[APPLICATION_KEY]!!) }
        }
    }
}
