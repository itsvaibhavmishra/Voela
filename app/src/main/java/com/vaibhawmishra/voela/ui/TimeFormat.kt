package com.vaibhawmishra.voela.ui

// Shared short duration formatting, e.g. "8s" or "1m 45s".
fun formatDurationSeconds(seconds: Int): String {
    val s = seconds.coerceAtLeast(0)
    return if (s < 60) "${s}s" else "${s / 60}m ${s % 60}s"
}
