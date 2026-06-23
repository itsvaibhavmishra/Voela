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
) : AndroidViewModel(application) {

    private val workManager = WorkManager.getInstance(application)

    private val _uiState = MutableStateFlow(SplitUiState(feature = feature))
    val uiState: StateFlow<SplitUiState> = _uiState.asStateFlow()

    @Volatile private var workDone = false
    @Volatile private var minElapsed = false

    init {
        val data = workDataOf(
            VocalSeparation.KEY_SOURCE to source,
            VocalSeparation.KEY_START_MS to startMs,
            VocalSeparation.KEY_END_MS to endMs,
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
    }

    fun cancel() = workManager.cancelUniqueWork(VocalSeparation.WORK_NAME)

    private fun observe(info: WorkInfo?) {
        when (info?.state) {
            WorkInfo.State.ENQUEUED, WorkInfo.State.RUNNING -> {
                val p = info.progress.getInt(VocalSeparation.KEY_PROGRESS, 0)
                _uiState.update { it.copy(progress = maxOf(it.progress, p).coerceAtMost(99)) }
            }
            WorkInfo.State.SUCCEEDED -> {
                workDone = true
                _uiState.update { it.copy(progress = maxOf(it.progress, 95)) }
                maybeComplete()
            }
            WorkInfo.State.FAILED, WorkInfo.State.CANCELLED -> _uiState.update { it.copy(failed = true) }
            else -> Unit
        }
    }

    private fun maybeComplete() {
        if (workDone && minElapsed) _uiState.update { it.copy(progress = 100, isComplete = true) }
    }

    companion object {
        private const val MIN_DISPLAY_MS = 10_000L

        fun factory(feature: TrimFeature, source: String, startMs: Long, endMs: Long) = viewModelFactory {
            initializer { SplitViewModel(this[APPLICATION_KEY]!!, feature, source, startMs, endMs) }
        }
    }
}
