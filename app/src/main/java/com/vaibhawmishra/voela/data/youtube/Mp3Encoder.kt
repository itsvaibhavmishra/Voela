package com.vaibhawmishra.voela.data.youtube

import java.io.File

// MP3 export. Android has no native MP3 encoder, so this is backed by LAME
// (added via the NDK). Placeholder until the LAME native build is wired in.
object Mp3Encoder {
    fun encode(input: File, output: File, bitrate: String?): Boolean = false
}
