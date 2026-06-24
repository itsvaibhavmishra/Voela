package com.vaibhawmishra.voela.ui

// Shared short duration formatting, e.g. "8s" or "1m 45s".
fun formatDurationSeconds(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return if (s < 60) "${s}s" else "${s / 60}m ${s % 60}s"
}

// Clock form for a media length, e.g. "3:45" (or "1:02:03" for long tracks).
fun formatClock(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%d:%02d".format(m, s)
}

// Coarse "time ago" for list rows, e.g. "just now", "2h ago", "3d ago".
fun relativeTime(epochMs: Long): String {
    val diff = (System.currentTimeMillis() - epochMs).coerceAtLeast(0)
    val min = diff / 60_000
    val hr = diff / 3_600_000
    val day = diff / 86_400_000
    return when {
        min < 1 -> "just now"
        min < 60 -> "${min}m ago"
        hr < 24 -> "${hr}h ago"
        day < 7 -> "${day}d ago"
        else -> "${day / 7}w ago"
    }
}
