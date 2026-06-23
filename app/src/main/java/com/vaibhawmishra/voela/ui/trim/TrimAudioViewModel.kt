package com.vaibhawmishra.voela.ui.trim

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.vaibhawmishra.voela.data.audio.AudioMetadataReader
import com.vaibhawmishra.voela.data.audio.WaveformGenerator
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
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

    private val player = ExoPlayer.Builder(application).build()

    private val _uiState = MutableStateFlow(TrimAudioUiState(feature = feature, title = title))
    val uiState: StateFlow<TrimAudioUiState> = _uiState.asStateFlow()

    private var positionJob: Job? = null

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startPositionUpdates() else positionJob?.cancel()
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) setDuration(player.duration.coerceAtLeast(0))
            }
        })
        val uri = if (source.startsWith("content://")) Uri.parse(source) else Uri.fromFile(File(source))
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        loadMetadata()
        loadWaveform()
    }

    fun onPlayPause() {
        if (player.isPlaying) {
            player.pause()
            return
        }
        val state = _uiState.value
        if (player.currentPosition < state.startMs || player.currentPosition >= state.endMs) player.seekTo(state.startMs)
        player.play()
    }

    // From the waveform handles or the range slider — fractions of the total duration
    fun onRangeChange(startFraction: Float, endFraction: Float) {
        val dur = _uiState.value.durationMs
        if (dur <= 0) return
        val newStart = (startFraction * dur).toLong().coerceIn(0, dur)
        val startMoved = newStart != _uiState.value.startMs
        _uiState.update { it.copy(startMs = newStart, endMs = (endFraction * dur).toLong().coerceIn(0, dur)) }
        if (startMoved) seekToStart(newStart)
    }

    fun onStartStep(deltaMs: Long) {
        val newStart = (_uiState.value.startMs + deltaMs).coerceIn(0, _uiState.value.endMs - MIN_RANGE_MS)
        _uiState.update { it.copy(startMs = newStart) }
        seekToStart(newStart)
    }

    fun onEndStep(deltaMs: Long) = _uiState.update {
        it.copy(endMs = (it.endMs + deltaMs).coerceIn(it.startMs + MIN_RANGE_MS, it.durationMs))
    }

    // Moving the start always repositions playback to it, so playback runs from the new start
    private fun seekToStart(startMs: Long) {
        player.seekTo(startMs)
        _uiState.update { it.copy(positionMs = startMs) }
    }

    private fun setDuration(durationMs: Long) {
        if (durationMs <= 0) return
        _uiState.update {
            if (it.durationMs > 0) it
            else it.copy(durationMs = durationMs, endMs = if (it.endMs == 0L) durationMs else it.endMs)
        }
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (true) {
                val pos = player.currentPosition.coerceAtLeast(0)
                val state = _uiState.value
                // Stop at the end of the selected range and rewind to its start
                if (state.endMs > 0 && pos >= state.endMs) {
                    player.pause()
                    player.seekTo(state.startMs)
                    _uiState.update { it.copy(positionMs = state.startMs) }
                    break
                }
                _uiState.update { it.copy(positionMs = pos) }
                delay(40)
            }
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
                )
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
        positionJob?.cancel()
        player.release()
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
