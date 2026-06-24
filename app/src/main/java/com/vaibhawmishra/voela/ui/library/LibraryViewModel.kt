package com.vaibhawmishra.voela.ui.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vaibhawmishra.voela.data.library.LibraryStore
import com.vaibhawmishra.voela.ui.home.RecentAudio
import com.vaibhawmishra.voela.ui.home.toRecentAudio
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

// Full list of kept actions (extractions + vocal splits), newest first.
class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val store = LibraryStore(application)

    val items: StateFlow<List<RecentAudio>> =
        store.items
            .map { list -> list.map { it.toRecentAudio() } }
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    companion object {
        val Factory = viewModelFactory {
            initializer { LibraryViewModel(this[APPLICATION_KEY]!!) }
        }
    }
}
