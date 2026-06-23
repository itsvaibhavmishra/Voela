package com.vaibhawmishra.voela.data.youtube

import kotlinx.coroutines.delay

// Placeholder until the real yt-dlp worker lands in the next phase; mimics extraction timing
class StubAudioExtractor : AudioExtractor {
    override suspend fun extract(url: String, onProgress: (Int) -> Unit): ExtractedAudioResult {
        listOf(20, 45, 75, 100).forEach { percent ->
            delay(650)
            onProgress(percent)
        }
        return ExtractedAudioResult(title = "Extracted Audio", localPath = "")
    }
}
