package com.vaibhawmishra.voela.ui.youtube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.WorkInfo
import com.vaibhawmishra.voela.data.youtube.Extraction
import com.vaibhawmishra.voela.data.youtube.ExtractionRepository
import com.vaibhawmishra.voela.ui.components.waveformBars
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class YouTubeViewModel(private val repository: ExtractionRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(YouTubeUiState(recentLinks = initialRecentLinks))
    val uiState: StateFlow<YouTubeUiState> = _uiState.asStateFlow()

    // Re-attaches to a running/finished job after the app or process restarts
    init {
        viewModelScope.launch {
            repository.workInfo.collect(::applyWorkInfo)
        }
    }

    fun onUrlChange(url: String) = _uiState.update { it.copy(url = url) }

    fun onExtract() {
        val url = _uiState.value.url
        if (url.isBlank() || _uiState.value.status == ExtractionStatus.Processing) return
        _uiState.update { it.copy(status = ExtractionStatus.Processing, progress = 0, result = null) }
        repository.start(url)
    }

    fun onClearRecents() = _uiState.update { it.copy(recentLinks = emptyList()) }

    fun onClearResult() {
        repository.clearFinished()
        _uiState.update { it.copy(status = ExtractionStatus.Idle, progress = 0, result = null) }
    }

    private fun applyWorkInfo(info: WorkInfo?) {
        when (info?.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED ->
                _uiState.update { it.copy(status = ExtractionStatus.Processing, progress = 0) }

            WorkInfo.State.RUNNING ->
                _uiState.update { it.copy(status = ExtractionStatus.Processing, progress = info.progress.getInt(Extraction.KEY_PROGRESS, it.progress)) }

            WorkInfo.State.SUCCEEDED -> _uiState.update {
                val title = info.outputData.getString(Extraction.KEY_TITLE).orEmpty()
                val path = info.outputData.getString(Extraction.KEY_OUTPUT_PATH).orEmpty()
                it.copy(
                    status = ExtractionStatus.Done,
                    progress = 100,
                    result = ExtractedAudio(title, path, waveformBars((title + path).hashCode())),
                )
            }

            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED ->
                _uiState.update { it.copy(status = ExtractionStatus.Idle, progress = 0) }

            null -> Unit
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { YouTubeViewModel(ExtractionRepository(this[APPLICATION_KEY]!!)) }
        }
    }
}
