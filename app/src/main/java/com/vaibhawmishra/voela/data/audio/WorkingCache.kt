package com.vaibhawmishra.voela.data.audio

import android.content.Context
import java.io.File

// Clears transient working files (the post-transcode temp dir) so a save that was
// killed mid-way can't leave a large blob behind. The extracted source file and
// the user's saved downloads are intentionally left untouched.
object WorkingCache {
    fun sweep(context: Context) {
        File(context.cacheDir, "save").deleteRecursively()
    }
}
