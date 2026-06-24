package com.vaibhawmishra.voela.data.audio

// On-device DTTNet vocal separation (STFT -> ONNX Runtime -> iSTFT in native code).
// Processes one fixed 261120-sample stereo chunk per call.
object DttSeparator {

    const val CHUNK = 261120

    init {
        System.loadLibrary("onnxruntime")
        System.loadLibrary("voela-dtt")
    }

    // Returns engine handle (0 on failure). modelPath = the DTTNet vocals .onnx.
    external fun nativeCreate(modelPath: String): Long

    // interleaved stereo float PCM of exactly 2*CHUNK samples -> vocals (2*CHUNK), or null.
    external fun nativeProcess(handle: Long, interleaved: FloatArray): FloatArray?

    external fun nativeDestroy(handle: Long)
}
