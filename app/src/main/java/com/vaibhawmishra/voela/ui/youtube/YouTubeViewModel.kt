package com.vaibhawmishra.voela.ui.youtube

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
import androidx.work.Data
import androidx.work.WorkInfo
import com.vaibhawmishra.voela.R
import com.vaibhawmishra.voela.data.youtube.Extraction
import com.vaibhawmishra.voela.data.youtube.ExtractionRepository
import com.vaibhawmishra.voela.data.youtube.WaveformGenerator
import com.vaibhawmishra.voela.ui.components.waveformBars
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class YouTubeViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ExtractionRepository(application)
    private val player = ExoPlayer.Builder(application).build()

    private val _uiState = MutableStateFlow(YouTubeUiState(recentLinks = initialRecentLinks))
    val uiState: StateFlow<YouTubeUiState> = _uiState.asStateFlow()

    private var positionJob: Job? = null
    private var saveInitiated = false

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startPositionUpdates() else positionJob?.cancel()
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_READY -> _uiState.update { it.copy(durationMs = player.duration.coerceAtLeast(0)) }
                    Player.STATE_ENDED -> {
                        player.pause()
                        player.seekTo(0)
                        _uiState.update { it.copy(positionMs = 0) }
                    }
                }
            }
        })
        viewModelScope.launch { repository.workInfo.collect(::applyWorkInfo) }
        viewModelScope.launch { repository.saveWorkInfo.collect(::applySaveInfo) }
    }

    fun onUrlChange(url: String) = _uiState.update { it.copy(url = url) }

    fun onExtract() {
        val url = _uiState.value.url
        if (url.isBlank() || _uiState.value.status == ExtractionStatus.Processing) return
        _uiState.update { it.copy(status = ExtractionStatus.Processing, progress = 0, result = null, savedLabel = null) }
        repository.start(url)
    }

    fun onPlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
            player.play()
        }
    }

    fun onSeek(fraction: Float) {
        val duration = player.duration
        if (duration > 0) {
            player.seekTo((duration * fraction).toLong())
            _uiState.update { it.copy(positionMs = player.currentPosition.coerceAtLeast(0)) }
        }
    }

    fun onDownload(option: DownloadOption) {
        val result = _uiState.value.result ?: return
        if (result.localPath.isBlank()) return
        val data = Data.Builder()
            .putString(Extraction.KEY_INPUT_PATH, result.localPath)
            .putString(Extraction.KEY_CODEC, option.codec)
            .putString(Extraction.KEY_MIME, option.mimeType)
            .putString(Extraction.KEY_EXTENSION, option.extension)
            .putString(Extraction.KEY_TITLE, result.title)
            .apply { option.bitrate?.let { putString(Extraction.KEY_BITRATE, it) } }
            .build()
        saveInitiated = true
        repository.startSave(data)
        _uiState.update { it.copy(isSaving = true, savedLabel = null) }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun onClearRecents() = _uiState.update { it.copy(recentLinks = emptyList()) }

    fun onClearResult() {
        player.stop()
        player.clearMediaItems()
        repository.clearFinished()
        _uiState.update { it.copy(status = ExtractionStatus.Idle, progress = 0, result = null, isPlaying = false, positionMs = 0, durationMs = 0, savedLabel = null) }
    }

    private fun applySaveInfo(info: WorkInfo?) {
        when (info?.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED, WorkInfo.State.RUNNING ->
                _uiState.update { it.copy(isSaving = true) }

            WorkInfo.State.SUCCEEDED -> if (saveInitiated) {
                saveInitiated = false
                val label = getApplication<Application>().getString(R.string.saved_to_music)
                _uiState.update { it.copy(isSaving = false, message = label, savedLabel = label) }
            } else {
                _uiState.update { it.copy(isSaving = false) }
            }

            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> if (saveInitiated) {
                saveInitiated = false
                _uiState.update { it.copy(isSaving = false, message = getApplication<Application>().getString(R.string.save_failed)) }
            } else {
                _uiState.update { it.copy(isSaving = false) }
            }

            null -> Unit
        }
    }

    // Decode the real waveform off the worker so Done isn't blocked; swap it in when ready
    private fun loadWaveform(path: String) {
        viewModelScope.launch {
            val real = WaveformGenerator.generate(path)
            _uiState.update {
                if (it.result?.localPath == path) it.copy(result = it.result.copy(waveform = real)) else it
            }
        }
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (true) {
                _uiState.update { it.copy(positionMs = player.currentPosition.coerceAtLeast(0)) }
                delay(40)
            }
        }
    }

    private fun applyWorkInfo(info: WorkInfo?) {
        when (info?.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED ->
                _uiState.update { it.copy(status = ExtractionStatus.Processing, progress = 0) }

            WorkInfo.State.RUNNING ->
                _uiState.update { it.copy(status = ExtractionStatus.Processing, progress = info.progress.getInt(Extraction.KEY_PROGRESS, it.progress)) }

            WorkInfo.State.SUCCEEDED -> {
                val title = info.outputData.getString(Extraction.KEY_TITLE).orEmpty()
                val path = info.outputData.getString(Extraction.KEY_OUTPUT_PATH).orEmpty()
                // Show Done immediately with a placeholder; the real waveform is decoded async
                _uiState.update {
                    it.copy(
                        status = ExtractionStatus.Done,
                        progress = 100,
                        result = ExtractedAudio(title, path, waveformBars(path.hashCode()), sourceUrl = info.outputData.getString(Extraction.KEY_SOURCE_URL).orEmpty()),
                        isPlaying = false,
                        positionMs = 0,
                        durationMs = 0,
                    )
                }
                if (path.isNotBlank()) {
                    player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(path))))
                    player.prepare()
                    loadWaveform(path)
                }
            }

            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED ->
                _uiState.update { it.copy(status = ExtractionStatus.Idle, progress = 0) }

            null -> Unit
        }
    }

    override fun onCleared() {
        positionJob?.cancel()
        player.release()
    }

    companion object {
        val Factory = viewModelFactory {
            initializer { YouTubeViewModel(this[APPLICATION_KEY]!!) }
        }
    }
}
