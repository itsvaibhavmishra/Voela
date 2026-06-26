package com.vaibhawmishra.voela.data.audio

import android.content.Context

// Tracks how fast each engine runs on THIS device, as a real-time factor
// (processing time / audio duration). Seeded with rough defaults, then refined
// from every completed run so the time estimates self-calibrate to the hardware.
object EngineStats {
    private const val PREFS = "engine_stats"

    // Rough starting points until the device records its own (mid-range phone numbers).
    private fun default(engine: String) = if (engine == VocalSeparation.ENGINE_BEST) 1.4f else 0.2f

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    // Real-time factor for an engine: processing seconds per second of audio.
    fun rtf(context: Context, engine: String): Float =
        prefs(context).getFloat("rtf_$engine", default(engine))

    // Estimated processing time (ms) for `audioMs` of selection on the given engine.
    fun estimateMs(context: Context, engine: String, audioMs: Long): Long =
        (audioMs * rtf(context, engine)).toLong()

    // Fold a finished run into the stored factor (light smoothing dampens one-off spikes).
    fun record(context: Context, engine: String, processingMs: Long, audioMs: Long) {
        if (audioMs <= 0 || processingMs <= 0) return
        val measured = processingMs.toFloat() / audioMs
        val prev = prefs(context).getFloat("rtf_$engine", measured)
        val smoothed = prev * 0.4f + measured * 0.6f
        prefs(context).edit().putFloat("rtf_$engine", smoothed).apply()
    }
}
