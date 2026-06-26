package com.vaibhawmishra.voela.ui.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.vaibhawmishra.voela.data.update.AppUpdate
import com.vaibhawmishra.voela.data.update.Release
import java.io.File
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class UpdatePhase { Hidden, Checking, UpToDate, Available, Downloading, NeedsPermission, Installing, Failed }

data class UpdateUiState(
    val phase: UpdatePhase = UpdatePhase.Hidden,
    val currentVersion: String = "",
    val latest: Release? = null,
    val downloadPercent: Int = 0,
)

class UpdateViewModel(app: Application) : AndroidViewModel(app) {

    private val _state = MutableStateFlow(UpdateUiState(currentVersion = AppUpdate.currentVersion(app)))
    val state: StateFlow<UpdateUiState> = _state.asStateFlow()

    private var downloaded: File? = null
    private var downloadJob: Job? = null

    // Home: throttled once-a-day check; only surfaces if newer and not dismissed.
    fun autoCheck() = viewModelScope.launch {
        val ctx = getApplication<Application>()
        val now = System.currentTimeMillis()
        if (now - AppUpdate.lastCheck(ctx) < AppUpdate.CHECK_INTERVAL_MS) return@launch
        AppUpdate.setLastCheck(ctx, now)
        val rel = AppUpdate.fetchLatest() ?: return@launch
        if (AppUpdate.isNewer(rel.version, _state.value.currentVersion) && rel.version != AppUpdate.dismissedVersion(ctx)) {
            _state.update { it.copy(phase = UpdatePhase.Available, latest = rel) }
        }
    }

    // Settings: manual check, always reports a result.
    fun checkNow() = viewModelScope.launch {
        _state.update { it.copy(phase = UpdatePhase.Checking) }
        val rel = AppUpdate.fetchLatest()
        _state.update {
            if (rel != null && AppUpdate.isNewer(rel.version, it.currentVersion))
                it.copy(phase = UpdatePhase.Available, latest = rel)
            else it.copy(phase = UpdatePhase.UpToDate, latest = rel)
        }
    }

    fun update() {
        val rel = _state.value.latest ?: return
        downloadJob = viewModelScope.launch {
            _state.update { it.copy(phase = UpdatePhase.Downloading, downloadPercent = 0) }
            val file = AppUpdate.download(getApplication(), rel) { p ->
                _state.update { s -> s.copy(downloadPercent = p) }
            }
            if (file == null) {
                _state.update { it.copy(phase = UpdatePhase.Failed) }
                return@launch
            }
            downloaded = file
            proceedInstall()
        }
    }

    // User backed out / closed the sheet while downloading: stop the stream and bin the
    // partial APK so we never hand a truncated file to the installer.
    fun cancelDownload() {
        downloadJob?.cancel()
        downloadJob = null
        downloaded = null
        AppUpdate.cleanup(getApplication())
        _state.update { it.copy(phase = UpdatePhase.Hidden, downloadPercent = 0) }
    }

    // After download, or after the user grants the install permission.
    fun proceedInstall() {
        val ctx = getApplication<Application>()
        val file = downloaded ?: return
        if (!AppUpdate.canInstall(ctx)) {
            _state.update { it.copy(phase = UpdatePhase.NeedsPermission) }
            return
        }
        _state.update { it.copy(phase = UpdatePhase.Installing) }
        AppUpdate.install(ctx, file)
    }

    fun openInstallSettings() = AppUpdate.requestInstallPermission(getApplication())

    fun later() {
        _state.value.latest?.let { AppUpdate.setDismissedVersion(getApplication(), it.version) }
        _state.update { it.copy(phase = UpdatePhase.Hidden) }
    }

    fun dismiss() = _state.update { it.copy(phase = UpdatePhase.Hidden) }

    companion object {
        val Factory = viewModelFactory { initializer { UpdateViewModel(this[APPLICATION_KEY]!!) } }
    }
}
