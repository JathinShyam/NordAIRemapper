package com.nordairemapper.service

import java.io.BufferedReader
import java.io.InputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Verifies that logd actually delivers OTHER apps' logs to our uid.
 *
 * On OxygenOS/ColorOS 15+, `pm grant READ_LOGS` succeeds while logd still
 * filters cross-app lines unless the OEM "USB debugging (Security settings)"
 * authorization is enabled — and that toggle resets itself on every reboot,
 * so detection dies silently after each boot while every Android-level check
 * looks green. This probe replicates exactly what [LogcatWatcherService]'s
 * spawned `logcat` sees: healthy devices see other pids' chatter within
 * milliseconds; enforced ones see only our own pid forever.
 */
object LogVisibilityProbe {

    enum class Result { VISIBLE, BLIND }

    private val selfPid = android.os.Process.myPid()
    private val PID_REGEX = Regex("\\(\\s*(\\d+)\\)")

    /**
     * Streams a few seconds of main buffer; VISIBLE on the first line from a
     * different pid, BLIND on timeout. Must run while the screen is on —
     * call it from foreground UI flows only.
     */
    suspend fun probe(windowMs: Long = 4_000L): Result = withContext(Dispatchers.IO) {
        val process = ProcessBuilder("logcat", "-b", "main", "-T", "1", "-v", "brief")
            .redirectErrorStream(true)
            .start()
        try {
            val sawOtherPid = withTimeoutOrNull(windowMs) {
                BufferedReader(InputStreamReader(process.inputStream)).useLines { lines ->
                    lines.any { line ->
                        PID_REGEX.find(line)?.groupValues?.get(1)?.toIntOrNull() != selfPid
                    }
                }
            }
            if (sawOtherPid == true) Result.VISIBLE else Result.BLIND
        } finally {
            runCatching { process.destroy() }
        }
    }
}
