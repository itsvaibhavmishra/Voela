package com.vaibhawmishra.voela.ui.audiosplit

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
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.vaibhawmishra.voela.data.audio.AudioSplit
import com.vaibhawmishra.voela.data.audio.AudioSplitPlan
import com.vaibhawmishra.voela.data.audio.AudioSplitWorker
import com.vaibhawmishra.voela.data.audio.ClipRange
import com.vaibhawmishra.voela.data.audio.WaveformGenerator
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class SplitUnit { SECONDS, MINUTES }

data class AudioSplitUiState(
    val title: String = "",
    val totalMs: Long = 0,
    val value: Int = 60,
    val unit: SplitUnit = SplitUnit.SECONDS,
    val clips: List<ClipRange> = emptyList(),
    val waveform: List<Float> = emptyList(),
    val isPlaying: Boolean = false,
    val positionMs: Long = 0,   // within the selection (0..totalMs)
    val playingClip: Int = -1,  // -1 = whole selection, else the clip index
    val splitting: Boolean = false,
    val splitProgress: Int = 0,
    val savedCount: Int = 0,    // >0 once clips are saved (drives the confirmation)
    val error: String? = null,
) {
    val segmentMs: Long get() = if (unit == SplitUnit.MINUTES) value * 60_000L else value * 1_000L
}

class AudioSplitViewModel(
    application: Application,
    title: String,
    source: String,
    private val startMs: Long,
    private val endMs: Long,
) : AndroidViewModel(application) {

    private val player = ExoPlayer.Builder(application).build()
    private val workManager = WorkManager.getInstance(application)
    private val splitSource = source
    private var positionJob: Job? = null
    private var recomputeJob: Job? = null
    private var rangeStart = startMs
    private var rangeEnd = endMs
    @Volatile private var splitInitiated = false

    private val _uiState = MutableStateFlow(
        AudioSplitUiState(title = title, totalMs = (endMs - startMs).coerceAtLeast(0)).recompute(),
    )
    val uiState: StateFlow<AudioSplitUiState> = _uiState.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
                if (isPlaying) startPositionUpdates() else positionJob?.cancel()
            }
        })
        val uri = if (source.startsWith("content://")) Uri.parse(source) else Uri.fromFile(File(source))
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        viewModelScope.launch {
            val bars = WaveformGenerator.generate(getApplication(), source)
            _uiState.update { it.copy(waveform = bars) }
        }
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(AudioSplit.WORK_NAME).collect { observeSplit(it.firstOrNull()) }
        }
    }

    fun onSplit() {
        if (_uiState.value.splitting) return
        player.pause()
        splitInitiated = true
        _uiState.update { it.copy(splitting = true, splitProgress = 0, savedCount = 0, error = null) }
        val data = workDataOf(
            AudioSplit.KEY_SOURCE to splitSource,
            AudioSplit.KEY_START_MS to startMs,
            AudioSplit.KEY_END_MS to endMs,
            AudioSplit.KEY_SEGMENT_MS to _uiState.value.segmentMs,
            AudioSplit.KEY_TITLE to _uiState.value.title,
        )
        workManager.enqueueUniqueWork(
            AudioSplit.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<AudioSplitWorker>().setInputData(data).build(),
        )
    }

    fun onConsumeResult() = _uiState.update { it.copy(savedCount = 0, error = null) }

    private fun observeSplit(info: WorkInfo?) {
        if (!splitInitiated) return
        when (info?.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING ->
                _uiState.update { it.copy(splitting = true, splitProgress = info.progress.getInt(AudioSplit.KEY_PROGRESS, it.splitProgress)) }
            WorkInfo.State.SUCCEEDED -> {
                splitInitiated = false
                _uiState.update { it.copy(splitting = false, splitProgress = 100, savedCount = info.outputData.getInt(AudioSplit.KEY_COUNT, 0)) }
            }
            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> {
                splitInitiated = false
                _uiState.update { it.copy(splitting = false, error = info.outputData.getString(AudioSplit.KEY_ERROR) ?: "Split failed") }
            }
            else -> Unit
        }
    }

    // Big button: pause anything playing, otherwise play the whole selection.
    fun onPlayPause() {
        if (player.isPlaying) { player.pause(); return }
        playRange(startMs, endMs, clipIndex = -1)
    }

    // Tap a clip: pause it if it's the one playing, otherwise preview just that clip.
    fun onPlayClip(index: Int) {
        val clip = _uiState.value.clips.getOrNull(index) ?: return
        if (player.isPlaying && _uiState.value.playingClip == index) { player.pause(); return }
        playRange(startMs + clip.startMs, startMs + clip.endMs, index)
    }

    private fun playRange(from: Long, to: Long, clipIndex: Int) {
        rangeStart = from
        rangeEnd = to
        _uiState.update { it.copy(playingClip = clipIndex) }
        player.seekTo(from)
        player.play()
    }

    fun onUnitChange(unit: SplitUnit) {
        if (unit == _uiState.value.unit) return
        stopForRetarget()
        val value = if (unit == SplitUnit.MINUTES) 1 else 30
        _uiState.update { it.copy(unit = unit, value = value) }
        scheduleRecompute()
    }

    fun stepUp() = step(+1)
    fun stepDown() = step(-1)

    // Seconds step by 1, minutes by 1; clamped to sensible bounds.
    private fun step(dir: Int) {
        stopForRetarget()
        _uiState.update {
            val max = if (it.unit == SplitUnit.MINUTES) 60 else 600
            it.copy(value = (it.value + dir).coerceIn(1, max))
        }
        scheduleRecompute()
    }

    // The value updates live, but recomputing the clip breakdown (preview + stats) is
    // debounced so holding +/- doesn't thrash through hundreds of layouts per second.
    private fun scheduleRecompute() {
        recomputeJob?.cancel()
        recomputeJob = viewModelScope.launch {
            delay(180)
            _uiState.update { it.copy(clips = AudioSplitPlan.parts(it.totalMs, it.segmentMs)) }
        }
    }

    // Clip boundaries are about to change — stop any clip preview so the index stays valid.
    private fun stopForRetarget() {
        if (_uiState.value.playingClip != -1) {
            player.pause()
            _uiState.update { it.copy(playingClip = -1, positionMs = 0) }
        }
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = viewModelScope.launch {
            while (true) {
                val pos = player.currentPosition.coerceAtLeast(0)
                if (rangeEnd > 0 && pos >= rangeEnd) {
                    player.pause()
                    player.seekTo(rangeStart)
                    _uiState.update { it.copy(positionMs = (rangeStart - startMs).coerceIn(0, totalMs())) }
                    break
                }
                _uiState.update { it.copy(positionMs = (pos - startMs).coerceIn(0, totalMs())) }
                delay(40)
            }
        }
    }

    private fun totalMs() = (endMs - startMs).coerceAtLeast(0)

    private fun AudioSplitUiState.recompute(): AudioSplitUiState =
        copy(clips = AudioSplitPlan.parts(totalMs, segmentMs))

    override fun onCleared() {
        positionJob?.cancel()
        player.release()
    }

    companion object {
        fun factory(title: String, source: String, startMs: Long, endMs: Long) = viewModelFactory {
            initializer { AudioSplitViewModel(this[APPLICATION_KEY]!!, title, source, startMs, endMs) }
        }
    }
}
