package com.vaibhawmishra.voela.ui.split

import com.vaibhawmishra.voela.ui.trim.TrimFeature

data class SplitUiState(
    val feature: TrimFeature = TrimFeature.VOCALS,
    val progress: Int = 0,
    val isComplete: Boolean = false,
)
