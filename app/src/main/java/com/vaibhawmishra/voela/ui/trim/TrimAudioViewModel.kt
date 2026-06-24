package com.vaibhawmishra.voela.ui.trim

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vaibhawmishra.voela.data.audio.AudioMetadataReader
import com.vaibhawmishra.voela.data.audio.EngineStats
import com.vaibhawmishra.voela.data.audio.RangePlayer
import com.vaibhawmishra.voela.data.audio.VocalSeparation
import com.vaibhawmishra.voela.data.audio.WaveformGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val MIN_RANGE_MS = 200L

class TrimAudioViewModel(
    application: Application,
    feature: TrimFeature,
    title: String,
    private val source: String,
) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(TrimAudioUiState(feature = feature, title = title))
    val uiState: StateFlow<TrimAudioUiState> = _uiState.asStateFlow()

    private val rangePlayer = RangePlayer(
        application,
        viewModelScope,
        onUpdate = { playing, pos -> _uiState.update { it.copy(isPlaying = playing, positionMs = pos) } },
        onReady = { dur -> setDuration(dur) },
    )

    init {
        rangePlayer.setSource(source)
        loadMetadata()
        loadWaveform()
    }

    fun onPlayPause() {
        val state = _uiState.value
        rangePlayer.toggle(state.startMs, state.endMs)
    }

    // From the waveform handles or the range slider — fractions of the total duration
    fun onRangeChange(startFraction: Float, endFraction: Float) {
        val dur = _uiState.value.durationMs
        if (dur <= 0) return
        val newStart = (startFraction * dur).toLong().coerceIn(0, dur)
        val startMoved = newStart != _uiState.value.startMs
        _uiState.update { it.copy(startMs = newStart, endMs = (endFraction * dur).toLong().coerceIn(0, dur)).withEstimate() }
        if (startMoved) seekToStart(newStart)
    }

    fun onStartStep(deltaMs: Long) {
        val newStart = (_uiState.value.startMs + deltaMs).coerceIn(0, _uiState.value.endMs - MIN_RANGE_MS)
        _uiState.update { it.copy(startMs = newStart).withEstimate() }
        seekToStart(newStart)
    }

    fun onEndStep(deltaMs: Long) = _uiState.update {
        it.copy(endMs = (it.endMs + deltaMs).coerceIn(it.startMs + MIN_RANGE_MS, it.durationMs)).withEstimate()
    }

    fun onEngineChange(engine: SeparationEngine) = _uiState.update { it.copy(engine = engine).withEstimate() }

    // Approx processing time for the current selection on the chosen engine, using the
    // device's learned real-time factor.
    private fun TrimAudioUiState.withEstimate(): TrimAudioUiState {
        val audioMs = (endMs - startMs).coerceAtLeast(0)
        val key = if (engine == SeparationEngine.BEST) VocalSeparation.ENGINE_BEST else VocalSeparation.ENGINE_FAST
        return copy(estimateSeconds = (EngineStats.estimateMs(getApplication(), key, audioMs) / 1000).toInt())
    }

    // Moving the start always repositions playback to it, so playback runs from the new start
    private fun seekToStart(startMs: Long) {
        rangePlayer.seekTo(startMs)
    }

    private fun setDuration(durationMs: Long) {
        if (durationMs <= 0) return
        _uiState.update {
            if (it.durationMs > 0) it
            else it.copy(durationMs = durationMs, endMs = if (it.endMs == 0L) durationMs else it.endMs).withEstimate()
        }
    }

    private fun loadMetadata() {
        viewModelScope.launch {
            val meta = withContext(Dispatchers.IO) { AudioMetadataReader.read(getApplication(), source) }
            _uiState.update {
                it.copy(
                    format = meta.format,
                    totalDuration = formatClock(meta.durationMs),
                    sampleRate = formatSampleRate(meta.sampleRateHz),
                    durationMs = if (it.durationMs > 0) it.durationMs else meta.durationMs,
                    endMs = if (it.endMs == 0L) meta.durationMs else it.endMs,
                ).withEstimate()
            }
        }
    }

    private fun loadWaveform() {
        viewModelScope.launch {
            val bars = WaveformGenerator.generate(getApplication(), source)
            _uiState.update { it.copy(waveform = bars) }
        }
    }

    override fun onCleared() {
        rangePlayer.release()
    }

    companion object {
        fun factory(feature: TrimFeature, title: String, source: String) = viewModelFactory {
            initializer { TrimAudioViewModel(this[APPLICATION_KEY]!!, feature, title, source) }
        }

        // m:ss for the ruler / summary
        fun formatClock(ms: Long): String {
            if (ms <= 0) return "0:00"
            val totalSec = ms / 1000
            return "%d:%02d".format(totalSec / 60, totalSec % 60)
        }

        // mm:ss.SS for the precise start/end fields
        fun formatPrecise(ms: Long): String {
            val totalCs = (ms / 10).coerceAtLeast(0)
            return "%02d:%02d.%02d".format(totalCs / 6000, (totalCs / 100) % 60, totalCs % 100)
        }

        private fun formatSampleRate(hz: Int): String {
            if (hz <= 0) return ""
            val khz = hz / 1000.0
            return if (khz % 1.0 == 0.0) "%.0f kHz".format(khz) else "%.1f kHz".format(khz)
        }
    }
}
