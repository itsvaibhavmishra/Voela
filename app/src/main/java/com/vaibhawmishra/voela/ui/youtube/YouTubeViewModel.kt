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
import com.vaibhawmishra.voela.data.audio.AudioSave
import com.vaibhawmishra.voela.data.audio.VoelaStorage
import com.vaibhawmishra.voela.data.audio.WaveformGenerator
import com.vaibhawmishra.voela.data.library.LibraryStore
import com.vaibhawmishra.voela.data.youtube.Extraction
import com.vaibhawmishra.voela.data.youtube.ExtractionRepository
import com.vaibhawmishra.voela.data.youtube.RecentLinksStore
import com.vaibhawmishra.voela.ui.components.DownloadOption
import com.vaibhawmishra.voela.ui.components.waveformBars
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class YouTubeViewModel(application: Application, libraryId: String = "") : AndroidViewModel(application) {

    private val repository = ExtractionRepository(application)
    private val recentLinksStore = RecentLinksStore(application)
    private val libraryStore = LibraryStore(application)
    private val player = ExoPlayer.Builder(application).build()
    @Volatile private var restoredLib = false

    private val _uiState = MutableStateFlow(YouTubeUiState())
    val uiState: StateFlow<YouTubeUiState> = _uiState.asStateFlow()

    private var positionJob: Job? = null
    private var saveInitiated = false
    private var extractInitiated = false

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
        viewModelScope.launch { recentLinksStore.recents.collect { links -> _uiState.update { it.copy(recentLinks = links) } } }
        if (libraryId.isNotBlank()) {
            restoredLib = true
            viewModelScope.launch { restore(libraryId) }
        }
    }

    // Reopen a kept extraction from the library: show it as a Done result, ready to play/continue.
    private suspend fun restore(libraryId: String) {
        val item = libraryStore.items.first().firstOrNull { it.id == libraryId } ?: return
        val file = libraryStore.extractionFile(item)
        if (!file.exists()) return
        libraryStore.touch(libraryId) // reopened — reset its expiry clock
        val path = file.absolutePath
        _uiState.update {
            it.copy(
                status = ExtractionStatus.Done,
                progress = 100,
                result = ExtractedAudio(item.title, path, waveformBars(path.hashCode())),
                isPlaying = false,
                positionMs = 0,
                durationMs = 0,
            )
        }
        player.setMediaItem(MediaItem.fromUri(Uri.fromFile(file)))
        player.prepare()
        loadWaveform(path)
    }

    fun onUrlChange(url: String) = _uiState.update { it.copy(url = url) }

    fun onExtract() {
        val url = _uiState.value.url
        if (url.isBlank() || _uiState.value.status == ExtractionStatus.Processing) return
        _uiState.update { it.copy(status = ExtractionStatus.Processing, progress = 0, result = null, savedLabel = null) }
        extractInitiated = true
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
            .putString(AudioSave.KEY_INPUT_PATH, result.localPath)
            .putString(AudioSave.KEY_MIME, option.mimeType)
            .putString(AudioSave.KEY_EXTENSION, option.extension)
            .putString(AudioSave.KEY_TITLE, result.title)
            .putString(AudioSave.KEY_SUBPATH, VoelaStorage.youtubeDownloads)
            .putString(AudioSave.KEY_SAVED_LABEL, getApplication<Application>().getString(R.string.saved_to_music))
            .apply { option.bitrate?.let { putString(AudioSave.KEY_BITRATE, it) } }
            .build()
        saveInitiated = true
        repository.startSave(data)
        _uiState.update { it.copy(isSaving = true, saveProgress = 0, savedLabel = null) }
    }

    fun onMessageShown() = _uiState.update { it.copy(message = null) }

    fun onOpenLink(link: RecentLink) = onUrlChange(link.url)

    fun onClearRecents() {
        viewModelScope.launch { recentLinksStore.clear() }
    }

    fun onClearResult() {
        player.stop()
        player.clearMediaItems()
        repository.clearFinished()
        _uiState.update { it.copy(status = ExtractionStatus.Idle, progress = 0, result = null, isPlaying = false, positionMs = 0, durationMs = 0, savedLabel = null) }
    }

    private fun applySaveInfo(info: WorkInfo?) {
        when (info?.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED ->
                _uiState.update { it.copy(isSaving = true, saveProgress = 0) }

            WorkInfo.State.RUNNING ->
                _uiState.update { it.copy(isSaving = true, saveProgress = info.progress.getInt(AudioSave.KEY_PROGRESS, it.saveProgress)) }

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
            val real = WaveformGenerator.generate(getApplication(), path)
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
        // When we've restored a kept extraction, ignore stale finished jobs so they don't
        // clobber the restored result — until the user starts a fresh extraction.
        if (restoredLib && !extractInitiated) return
        when (info?.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED ->
                _uiState.update { it.copy(status = ExtractionStatus.Processing, progress = 0) }

            WorkInfo.State.RUNNING ->
                _uiState.update { it.copy(status = ExtractionStatus.Processing, progress = info.progress.getInt(Extraction.KEY_PROGRESS, it.progress)) }

            WorkInfo.State.SUCCEEDED -> {
                val title = info.outputData.getString(Extraction.KEY_TITLE).orEmpty()
                val path = info.outputData.getString(Extraction.KEY_OUTPUT_PATH).orEmpty()
                val sourceUrl = info.outputData.getString(Extraction.KEY_SOURCE_URL).orEmpty()
                // Persist only a freshly-completed extraction — re-attaching to a finished job
                // (e.g. on app relaunch) must not resurrect a recent the user cleared
                if (extractInitiated && sourceUrl.isNotBlank()) {
                    extractInitiated = false
                    val durationSec = info.outputData.getInt(Extraction.KEY_DURATION, 0)
                    viewModelScope.launch { recentLinksStore.add(RecentLink(title, formatDuration(durationSec), sourceUrl)) }
                    if (path.isNotBlank()) {
                        viewModelScope.launch { libraryStore.addExtraction(File(path), title, durationSec.toLong() * 1000) }
                    }
                }
                // Show Done immediately with a placeholder; the real waveform is decoded async
                _uiState.update {
                    it.copy(
                        status = ExtractionStatus.Done,
                        progress = 100,
                        result = ExtractedAudio(title, path, waveformBars(path.hashCode()), sourceUrl = sourceUrl),
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

            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                extractInitiated = false
                _uiState.update { it.copy(status = ExtractionStatus.Idle, progress = 0) }
            }

            null -> Unit
        }
    }

    // mm:ss (or h:mm:ss for long videos); blank when unknown
    private fun formatDuration(seconds: Int): String {
        if (seconds <= 0) return ""
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    override fun onCleared() {
        positionJob?.cancel()
        player.release()
    }

    companion object {
        fun factory(libraryId: String = "") = viewModelFactory {
            initializer { YouTubeViewModel(this[APPLICATION_KEY]!!, libraryId) }
        }
    }
}
