package com.nordairemapper.presentation.common

/** Human-readable "how long ago" for the detection health line. */
fun relativeLastSeen(epochMs: Long, nowMs: Long = System.currentTimeMillis()): String {
    if (epochMs <= 0L) return "never"
    val deltaMs = (nowMs - epochMs).coerceAtLeast(0)
    return when {
        deltaMs < 10_000L -> "just now"
        deltaMs < 60_000L -> "${deltaMs / 1000L}s ago"
        deltaMs < 3_600_000L -> "${deltaMs / 60_000L}m ago"
        deltaMs < 86_400_000L -> "${deltaMs / 3_600_000L}hr ago"
        else -> "${deltaMs / 86_400_000L}d ago"
    }
}
