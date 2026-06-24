package com.vaibhawmishra.voela.data.audio

import android.content.Context
import java.io.File

// Keys + locations for the vocal-separation WorkManager job.
object VocalSeparation {
    const val WORK_NAME = "vocal_separation"
    const val KEY_SOURCE = "source"
    const val KEY_START_MS = "start_ms"
    const val KEY_END_MS = "end_ms"
    const val KEY_ENGINE = "engine"
    const val KEY_PROGRESS = "progress"
    const val KEY_ELAPSED_MS = "elapsed_ms"
    const val KEY_ERROR = "error"

    const val ENGINE_FAST = "fast" // Spleeter
    const val ENGINE_BEST = "best" // DTTNet

    // Where the separated stems are written (and read by the result screen)
    fun outputDir(context: Context): File = File(context.getExternalFilesDir(null), "separation")

    // Model files, cached on device
    fun modelDir(context: Context): File = File(context.getExternalFilesDir(null), "models")
    fun vocalsModel(context: Context): File = File(modelDir(context), "vocals.fp16.onnx")
    fun accompModel(context: Context): File = File(modelDir(context), "accompaniment.fp16.onnx")
    fun dttModel(context: Context): File = File(modelDir(context), "dttnet_vocals.onnx")
}
