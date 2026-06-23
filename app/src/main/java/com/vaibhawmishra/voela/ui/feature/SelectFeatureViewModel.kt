package com.vaibhawmishra.voela.ui.feature

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
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SelectFeatureViewModel(
    application: Application,
    title: String,
    private val source: String,
) : AndroidViewModel(application) {

    private val player = ExoPlayer.Builder(application).build()

    private val _uiState = MutableStateFlow(SelectFeatureUiState(title = title))
    val uiState: StateFlow<SelectFeatureUiState> = _uiState.asStateFlow()

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                _uiState.update { it.copy(isPlaying = isPlaying) }
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    player.pause()
                    player.seekTo(0)
                }
            }
        })
        val uri = if (source.startsWith("content://")) Uri.parse(source) else Uri.fromFile(File(source))
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
        loadMetadata()
    }

    fun onPlayPause() {
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_ENDED) player.seekTo(0)
            player.play()
        }
    }

    private fun loadMetadata() {
        viewModelScope.launch {
            val meta = withContext(Dispatchers.IO) { AudioMetadataReader.read(getApplication(), source) }
            _uiState.update {
                it.copy(format = meta.format, duration = formatDuration(meta.durationMs), size = formatSize(meta.sizeBytes))
            }
        }
    }

    private fun formatDuration(ms: Long): String {
        if (ms <= 0) return ""
        val totalSec = ms / 1000
        val h = totalSec / 3600
        val m = (totalSec % 3600) / 60
        val s = totalSec % 60
        return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
    }

    private fun formatSize(bytes: Long): String {
        if (bytes <= 0) return ""
        val mb = bytes / 1_048_576.0
        return if (mb >= 1) "%.1f MB".format(mb) else "%.0f KB".format(bytes / 1024.0)
    }

    override fun onCleared() {
        player.release()
    }

    companion object {
        fun factory(title: String, source: String) = viewModelFactory {
            initializer { SelectFeatureViewModel(this[APPLICATION_KEY]!!, title, source) }
        }
    }
}
