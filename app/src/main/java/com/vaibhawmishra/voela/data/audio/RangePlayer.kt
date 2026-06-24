package com.vaibhawmishra.voela.data.audio

import android.content.Context
import android.net.Uri
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Thin ExoPlayer wrapper shared by the playback screens (Trim, Split Audio, YouTube):
// play a [from, to] range that stops and rewinds at the end, with a position ticker.
// Pass to = Long.MAX_VALUE to play to the natural end. onUpdate reports (isPlaying,
// positionMs); onReady fires once the duration is known.
class RangePlayer(
    context: Context,
    private val scope: CoroutineScope,
    private val onUpdate: (isPlaying: Boolean, positionMs: Long) -> Unit,
    private val onReady: (durationMs: Long) -> Unit = {},
) {
    private val player = ExoPlayer.Builder(context).build()
    private var job: Job? = null
    private var rangeStart = 0L
    private var rangeEnd = Long.MAX_VALUE

    init {
        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                onUpdate(isPlaying, player.currentPosition.coerceAtLeast(0))
                if (isPlaying) startTicking() else job?.cancel()
            }

            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) onReady(player.duration.coerceAtLeast(0))
                if (state == Player.STATE_ENDED) {
                    player.pause()
                    player.seekTo(rangeStart)
                    onUpdate(false, rangeStart)
                }
            }
        })
    }

    val isPlaying: Boolean get() = player.isPlaying
    val currentPosition: Long get() = player.currentPosition.coerceAtLeast(0)

    fun setSource(source: String) {
        val uri = if (source.startsWith("content://")) Uri.parse(source) else Uri.fromFile(File(source))
        player.setMediaItem(MediaItem.fromUri(uri))
        player.prepare()
    }

    // Toggle: pause if playing, otherwise play the given range.
    fun toggle(fromMs: Long, toMs: Long = Long.MAX_VALUE) {
        if (player.isPlaying) player.pause() else play(fromMs, toMs)
    }

    fun play(fromMs: Long, toMs: Long = Long.MAX_VALUE) {
        rangeStart = fromMs
        rangeEnd = toMs
        if (player.currentPosition < fromMs || player.currentPosition >= toMs) player.seekTo(fromMs)
        player.play()
    }

    fun pause() = player.pause()

    fun seekTo(ms: Long) {
        player.seekTo(ms)
        onUpdate(player.isPlaying, ms)
    }

    private fun startTicking() {
        job?.cancel()
        job = scope.launch {
            while (true) {
                val pos = player.currentPosition.coerceAtLeast(0)
                if (rangeEnd != Long.MAX_VALUE && pos >= rangeEnd) {
                    player.pause()
                    player.seekTo(rangeStart)
                    onUpdate(false, rangeStart)
                    break
                }
                onUpdate(true, pos)
                delay(40)
            }
        }
    }

    fun release() {
        job?.cancel()
        player.release()
    }
}
