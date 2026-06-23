package com.vaibhawmishra.voela.ui.youtube

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vaibhawmishra.voela.data.youtube.AudioExtractor
import com.vaibhawmishra.voela.data.youtube.StubAudioExtractor
import com.vaibhawmishra.voela.ui.components.waveformBars
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class YouTubeViewModel(private val extractor: AudioExtractor) : ViewModel() {

    private val _uiState = MutableStateFlow(YouTubeUiState(recentLinks = initialRecentLinks))
    val uiState: StateFlow<YouTubeUiState> = _uiState.asStateFlow()

    private var extractionJob: Job? = null

    fun onUrlChange(url: String) = _uiState.update { it.copy(url = url) }

    fun onExtract() {
        val url = _uiState.value.url
        if (url.isBlank() || _uiState.value.status == ExtractionStatus.Processing) return
        extractionJob = viewModelScope.launch {
            _uiState.update { it.copy(status = ExtractionStatus.Processing, progress = 0, result = null) }
            val res = extractor.extract(url) { percent ->
                _uiState.update { it.copy(progress = percent) }
            }
            _uiState.update {
                it.copy(
                    status = ExtractionStatus.Done,
                    result = ExtractedAudio(res.title, res.localPath, waveformBars((res.title + res.localPath).hashCode())),
                )
            }
        }
    }

    fun onClearRecents() = _uiState.update { it.copy(recentLinks = emptyList()) }

    companion object {
        val Factory = viewModelFactory {
            initializer { YouTubeViewModel(StubAudioExtractor()) }
        }
    }
}
