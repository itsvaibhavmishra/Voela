package com.vaibhawmishra.voela.ui.split

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.vaibhawmishra.voela.data.audio.EngineStats
import com.vaibhawmishra.voela.data.audio.VocalSeparation
import com.vaibhawmishra.voela.data.audio.VocalSeparationWorker
import com.vaibhawmishra.voela.ui.trim.TrimFeature
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

// Runs the on-device separation worker and drives the loading screen. The progress
// bar ramps over a minimum window so the screen never flashes; we only finish once
// the worker has actually produced the stems.
class SplitViewModel(
    application: Application,
    feature: TrimFeature,
    source: String,
    startMs: Long,
    endMs: Long,
    engine: String,
    title: String,
) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)
    private val engine = engine
    private val audioMs = (endMs - startMs).coerceAtLeast(0)
    private val startTimeMs = System.currentTimeMillis()

    private val _uiState = MutableStateFlow(
        SplitUiState(
            feature = feature,
            // seed with the device's learned estimate so a time appears immediately
            etaSeconds = (EngineStats.estimateMs(application, engine, audioMs) / 1000).toInt(),
        ),
    )
    val uiState: StateFlow<SplitUiState> = _uiState.asStateFlow()

    @Volatile private var workDone = false
    @Volatile private var minElapsed = false

    init {
        val data = workDataOf(
            VocalSeparation.KEY_SOURCE to source,
            VocalSeparation.KEY_START_MS to startMs,
            VocalSeparation.KEY_END_MS to endMs,
            VocalSeparation.KEY_ENGINE to engine,
            VocalSeparation.KEY_TITLE to title,
            VocalSeparation.KEY_FEATURE to feature.key,
        )
        workManager.enqueueUniqueWork(
            VocalSeparation.WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            OneTimeWorkRequestBuilder<VocalSeparationWorker>().setInputData(data).build(),
        )
        viewModelScope.launch {
            workManager.getWorkInfosForUniqueWorkFlow(VocalSeparation.WORK_NAME).collect { observe(it.firstOrNull()) }
        }
        // Never flash by: hold the screen for a minimum, even if work finishes sooner
        viewModelScope.launch {
            delay(MIN_DISPLAY_MS)
            minElapsed = true
            maybeComplete()
        }
        // Tick the "time left" down each second between the coarse per-chunk updates
        viewModelScope.launch {
            while (true) {
                delay(1_000)
                _uiState.update {
                    if (it.isComplete || it.failed || it.etaSeconds <= 0) it
                    else it.copy(etaSeconds = it.etaSeconds - 1)
                }
            }
        }
        // Creep the bar forward between the worker's coarse updates and through its silent
        // final phase (iSTFT, encode, save), so it never looks frozen near the top. Real
        // worker progress still wins via maxOf; this only fills the gaps.
        viewModelScope.launch {
            while (true) {
                delay(900)
                _uiState.update {
                    val ceiling = if (workDone) 99 else 97
                    if (it.isComplete || it.failed || it.progress >= ceiling) it
                    else it.copy(progress = it.progress + 1)
                }
            }
        }
    }

    fun cancel() = workManager.cancelUniqueWork(VocalSeparation.WORK_NAME)

    private fun observe(info: WorkInfo?) {
        when (info?.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING -> {
                val p = info.progress.getInt(VocalSeparation.KEY_PROGRESS, 0)
                _uiState.update {
                    val measured = measuredEta(p)
                    val eta = if (measured < 0) it.etaSeconds else smoothEta(it.etaSeconds, measured)
                    it.copy(progress = maxOf(it.progress, p).coerceAtMost(99), etaSeconds = eta)
                }
            }
            WorkInfo.State.SUCCEEDED -> {
                workDone = true
                val elapsed = info.outputData.getLong(VocalSeparation.KEY_ELAPSED_MS, 0)
                val libId = info.outputData.getString(VocalSeparation.KEY_LIBRARY_ID).orEmpty()
                if (elapsed > 0) EngineStats.record(getApplication(), engine, elapsed, audioMs)
                _uiState.update { it.copy(progress = maxOf(it.progress, 95), elapsedMs = elapsed, etaSeconds = 0, libraryId = libId) }
                maybeComplete()
            }
            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> _uiState.update { it.copy(failed = true) }
            else -> Unit
        }
    }

    // Measured time-left from real progress (-1 until there's enough to extrapolate).
    private fun measuredEta(progress: Int): Int {
        if (progress < 5) return -1
        val elapsed = System.currentTimeMillis() - startTimeMs
        return (elapsed * (95 - progress) / progress / 1000).toInt().coerceAtLeast(0)
    }

    // Drop to the truth immediately when we're ahead of estimate, but ease upward slowly
    // when behind, so the countdown never jumps up jarringly (just trends toward reality).
    private fun smoothEta(current: Int, measured: Int): Int {
        if (current <= 0) return measured
        return if (measured <= current) measured else current + ((measured - current) / 5).coerceAtLeast(1)
    }

    private fun maybeComplete() {
        if (workDone && minElapsed) _uiState.update { it.copy(progress = 100, isComplete = true) }
    }

    companion object {
        private const val MIN_DISPLAY_MS = 10_000L

        fun factory(feature: TrimFeature, source: String, startMs: Long, endMs: Long, engine: String, title: String) = viewModelFactory {
            initializer { SplitViewModel(this[APPLICATION_KEY]!!, feature, source, startMs, endMs, engine, title) }
        }
    }
}
