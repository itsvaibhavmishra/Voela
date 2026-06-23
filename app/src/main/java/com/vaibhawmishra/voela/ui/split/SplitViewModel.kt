package com.vaibhawmishra.voela.ui.split

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vaibhawmishra.voela.ui.trim.TrimFeature
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Drives the loading screen. For now it simulates progress so the screen is
// demonstrable; the real on-device separation (sherpa-onnx) will replace simulate()
// and report genuine progress through the same state.
class SplitViewModel(feature: TrimFeature) : ViewModel() {

    private val _uiState = MutableStateFlow(SplitUiState(feature = feature))
    val uiState: StateFlow<SplitUiState> = _uiState.asStateFlow()

    init {
        simulate()
    }

    private fun simulate() {
        viewModelScope.launch {
            // Ramp across the minimum display window so the loader is always shown a while.
            // The real engine will report genuine progress, still gated to MIN_DISPLAY_MS.
            val steps = 100
            repeat(steps) { i ->
                delay(MIN_DISPLAY_MS / steps)
                _uiState.update { it.copy(progress = i + 1) }
            }
            _uiState.update { it.copy(isComplete = true) }
        }
    }

    companion object {
        // Loader is shown for at least this long, even if processing finishes sooner
        private const val MIN_DISPLAY_MS = 10_000L

        fun factory(feature: TrimFeature) = viewModelFactory {
            initializer { SplitViewModel(feature) }
        }
    }
}
