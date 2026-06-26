package com.vaibhawmishra.voela.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vaibhawmishra.voela.data.settings.SettingsStore
import com.vaibhawmishra.voela.data.settings.StemFormat
import com.vaibhawmishra.voela.ui.theme.Accent
import com.vaibhawmishra.voela.ui.theme.DefaultAccent
import com.vaibhawmishra.voela.ui.theme.accentFor
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class SettingsUiState(
    val vocalFormat: StemFormat = StemFormat.M4A,
    val accent: Accent = DefaultAccent,
)

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val store = SettingsStore(application)

    val uiState: StateFlow<SettingsUiState> =
        combine(store.vocalFormat, store.accentColor) { format, accentKey ->
            SettingsUiState(format, accentFor(accentKey))
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SettingsUiState())

    fun setVocalFormat(format: StemFormat) = viewModelScope.launch { store.setVocalFormat(format) }

    fun setAccent(accent: Accent) = viewModelScope.launch { store.setAccentColor(accent.key) }

    companion object {
        val Factory = viewModelFactory {
            initializer { SettingsViewModel(this[APPLICATION_KEY]!!) }
        }
    }
}
