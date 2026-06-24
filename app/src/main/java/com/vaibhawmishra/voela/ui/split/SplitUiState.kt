package com.vaibhawmishra.voela.ui.split

import com.vaibhawmishra.voela.ui.trim.TrimFeature

data class SplitUiState(
    val feature: TrimFeature = TrimFeature.VOCALS,
    val progress: Int = 0,
    val isComplete: Boolean = false,
    val failed: Boolean = false,
    val elapsedMs: Long = 0,
    val etaSeconds: Int = -1, // estimated time remaining; -1 = unknown (hide)
)
