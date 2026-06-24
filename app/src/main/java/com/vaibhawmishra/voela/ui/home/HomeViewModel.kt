package com.vaibhawmishra.voela.ui.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vaibhawmishra.voela.data.library.LibraryStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Feeds Home's Recents from the kept-actions library (newest few).
class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val store = LibraryStore(application)

    val recents: StateFlow<List<RecentAudio>> =
        store.items
            .map { items -> items.take(RECENTS_MAX).map { it.toRecentAudio() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        // Clean up items past their expiry window on app start
        viewModelScope.launch { store.sweepExpired() }
    }

    fun delete(id: String) = viewModelScope.launch { store.delete(id) }

    companion object {
        private const val RECENTS_MAX = 4

        val Factory = viewModelFactory {
            initializer { HomeViewModel(this[APPLICATION_KEY]!!) }
        }
    }
}
