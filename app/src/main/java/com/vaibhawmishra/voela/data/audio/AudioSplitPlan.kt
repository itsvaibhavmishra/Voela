package com.vaibhawmishra.voela.data.audio

// One output clip's time range, relative to the start of the selection (0..totalMs).
data class ClipRange(val index: Int, val startMs: Long, val endMs: Long) {
    val durationMs: Long get() = endMs - startMs
}

// Splits a duration into equal-length clips. A small trailing remainder (<= 5s) is
// merged into the last clip instead of becoming a tiny sliver of its own.
object AudioSplitPlan {
    const val MERGE_THRESHOLD_MS = 5_000L

    fun parts(totalMs: Long, segmentMs: Long): List<ClipRange> {
        if (totalMs <= 0 || segmentMs <= 0) return emptyList()
        val full = (totalMs / segmentMs).toInt()
        if (full == 0) return listOf(ClipRange(0, 0, totalMs)) // segment longer than the whole

        val list = ArrayList<ClipRange>(full + 1)
        for (i in 0 until full) list.add(ClipRange(i, i * segmentMs, (i + 1) * segmentMs))
        val remainder = totalMs - full * segmentMs
        when {
            remainder <= 0 -> Unit
            remainder <= MERGE_THRESHOLD_MS -> {
                val last = list.removeAt(list.size - 1) // extend the last clip to the end
                list.add(ClipRange(last.index, last.startMs, totalMs))
            }
            else -> list.add(ClipRange(full, full * segmentMs, totalMs))
        }
        return list
    }
}
