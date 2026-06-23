package com.vaibhawmishra.voela.data.audio

// Kotlin entry point to the native source-separation shim (sherpa-onnx C-API).
object SourceSeparator {

    init {
        System.loadLibrary("onnxruntime")
        System.loadLibrary("sherpa-onnx-c-api")
        System.loadLibrary("voela-sep")
    }

    // Create the engine once (returns a handle, 0 on failure); pass uvrModel = "" for Spleeter.
    external fun nativeCreate(vocalsModel: String, accompModel: String, uvrModel: String): Long

    // Separate one chunk → [vocals, accompaniment] interleaved; null on failure.
    external fun nativeProcess(
        handle: Long,
        interleaved: FloatArray,
        numChannels: Int,
        numFrames: Int,
        inRate: Int,
    ): Array<FloatArray>?

    external fun nativeDestroy(handle: Long)
}
