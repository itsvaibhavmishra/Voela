package com.vaibhawmishra.voela.ui.result

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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.vaibhawmishra.voela.R
import com.vaibhawmishra.voela.data.audio.AudioMetadataReader
import com.vaibhawmishra.voela.data.audio.AudioSave
import com.vaibhawmishra.voela.data.audio.AudioSaveWorker
import com.vaibhawmishra.voela.data.audio.VoelaStorage
import com.vaibhawmishra.voela.data.audio.WaveformGenerator
import com.vaibhawmishra.voela.data.library.LibraryStore
import com.vaibhawmishra.voela.ui.components.DownloadOption
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ResultViewModel(application: Application, title: String, elapsedMs: Long, private val libraryId: String) : AndroidViewModel(application) {

    private val player = ExoPlayer.Builder(application).build()
    private val workManager = WorkManager.getInstance(application)
    private val stamp = SimpleDateFormat("HHmmss", Locale.US).format(Date())

    private val _uiState = MutableStateFlow(ResultUiState(title = title, elapsedMs = elapsedMs))
    val uiState: StateFlow<ResultUiState> = _uiState.asStateFlow()

    private var positionJob: Job? = null
    private var savingIndex = -1

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) startPositionUpdates() else positionJob?.cancel()
                _uiState.update {
                    it.copy(
                        isPlaying = isPlaying,
                        // Keep the selected stem on pause (so seek/position stay bound); clear it only when the track ends.
                        playingIndex = if (!isPlaying && player.playbackState == Player.STATE_ENDED) -1 else it.playingIndex,
                    )
                }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    player.pause()
                    player.seekTo(0)
                    _uiState.update { it.copy(playingIndex = -1, isPlaying = false, positionMs = 0) }
                }
            }
        })
        loadStems()
        // Reopened from the library — reset its expiry clock
        if (libraryId.isNotBlank()) viewModelScope.launch { LibraryStore(application).touch(libraryId) }
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(AudioSave.WORK_NAME).collect { applySaveInfo(it.firstOrNull()) }
        }
    }

    fun onPlayPause(index: Int) {
        val stem = _uiState.value.stems.getOrNull(index) ?: return
        if (_uiState.value.playingIndex == index && player.isPlaying) {
            player.pause()
            return
        }
        if (_uiState.value.playingIndex != index) {
            player.setMediaItem(MediaItem.fromUri(Uri.fromFile(File(stem.path))))
            player.prepare()
            _uiState.update { it.copy(playingIndex = index, positionMs = 0) }
        } else if (player.playbackState == Player.STATE_ENDED) {
            player.seekTo(0)
        }
        player.play()
    }

    fun onSeek(index: Int, fraction: Float) {
        if (_uiState.value.playingIndex != index) return
        val d = _uiState.value.stems[index].durationMs
        if (d > 0) {
            player.seekTo((d * fraction).toLong())
            _uiState.update { it.copy(positionMs = player.currentPosition.coerceAtLeast(0)) }
        }
    }

    fun onSave(index: Int, option: DownloadOption) {
        val state = _uiState.value
        val stem = state.stems.getOrNull(index) ?: return
        savingIndex = index
        val data = Data.Builder()
            .putString(AudioSave.KEY_INPUT_PATH, stem.path)
            .putString(AudioSave.KEY_MIME, option.mimeType)
            .putString(AudioSave.KEY_EXTENSION, option.extension)
            .putString(AudioSave.KEY_TITLE, "${state.title} - ${stem.label}")
            .putString(AudioSave.KEY_SUBPATH, VoelaStorage.vocalSplits(state.title, stamp))
            .putString(AudioSave.KEY_SAVED_LABEL, getApplication<Application>().getString(R.string.saved_to_vocal_splits))
            .apply { option.bitrate?.let { putString(AudioSave.KEY_BITRATE, it) } }
            .build()
        workManager.enqueueUniqueWork(
            AudioSave.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<AudioSaveWorker>().setInputData(data).build(),
        )
        setStem(index) { it.copy(isSaving = true, saved = false) }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    private fun applySaveInfo(info: WorkInfo?) {
        if (savingIndex < 0) return
        when (info?.state) {
            WorkInfo.State.SUCCEEDED -> {
                val saved = savingIndex
                savingIndex = -1
                setStem(saved) { it.copy(isSaving = false, saved = true) }
                _uiState.update { it.copy(message = getApplication<Application>().getString(R.string.saved_to_vocal_splits)) }
            }

            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                val failed = savingIndex
                savingIndex = -1
                setStem(failed) { it.copy(isSaving = false) }
                _uiState.update { it.copy(message = getApplication<Application>().getString(R.string.save_failed)) }
            }

            else -> Unit
        }
    }

    private fun loadStems() {
        val sub = if (libraryId.isBlank()) "separation" else "library/$libraryId"
        val dir = File(getApplication<Application>().getExternalFilesDir(null), sub)
        // Kept stems are stored in the user's chosen format; find whichever exists.
        fun stemFile(name: String): File =
            listOf("m4a", "mp3", "wav").map { File(dir, "$name.$it") }.firstOrNull { it.exists() }
                ?: File(dir, "$name.wav")
        val stems = listOf(
            StemUi("Vocals", stemFile("vocals").absolutePath),
            StemUi("Instrumental", stemFile("instrumental").absolutePath),
        ).filter { File(it.path).exists() }
        _uiState.update { it.copy(stems = stems) }
        stems.forEachIndexed { i, stem ->
            viewModelScope.launch {
                val meta = withContext(Dispatchers.IO) { AudioMetadataReader.read(getApplication(), stem.path) }
                val bars = WaveformGenerator.generate(getApplication(), stem.path)
                setStem(i) { it.copy(waveform = bars, durationMs = meta.durationMs) }
                if (i == 0) _uiState.update { it.copy(sampleRate = meta.sampleRateHz) }
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

    private inline fun setStem(index: Int, transform: (StemUi) -> StemUi) {
        _uiState.update { state ->
            state.copy(stems = state.stems.mapIndexed { i, s -> if (i == index) transform(s) else s })
        }
    }

    override fun onCleared() {
        positionJob?.cancel()
        player.release()
    }

    companion object {
        fun factory(title: String, elapsedMs: Long, libraryId: String = "") = viewModelFactory {
            initializer { ResultViewModel(this[APPLICATION_KEY]!!, title, elapsedMs, libraryId) }
        }
    }
}
