package com.nordairemapper.service.shizuku

import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread

/**
 * Runs inside the Shizuku server process. Each Unlock shell command is
 * executed with shell-level privileges — the same effect as typing it over
 * adb, without a computer.
 *
 * Commands are serialized (one binder thread must not exit() while another's
 * child is running) and time-bounded so a wedged child can't pin the server.
 */
class ShizukuGrantService : IGrantService.Stub() {

    private val lock = Any()
    private val active = AtomicInteger(0)

    override fun runCommand(command: String?): Int {
        if (command.isNullOrBlank()) return -1
        synchronized(lock) {
            active.incrementAndGet()
            try {
                val process = ProcessBuilder("sh", "-c", command)
                    .redirectErrorStream(true)
                    .start()
                // Drain output off the binder-critical path but bounded: a
                // wedged child must not pin the server thread forever.
                val drainer = thread(start = true) { process.inputStream.readBytes() }
                val finished = process.waitFor(COMMAND_TIMEOUT_MS, java.util.concurrent.TimeUnit.MILLISECONDS)
                if (!finished) {
                    process.destroyForcibly()
                    return COMMAND_TIMEOUT_EXIT
                }
                runCatching { drainer.join(500L) }
                return process.exitValue()
            } catch (t: Throwable) {
                return -2
            } finally {
                active.decrementAndGet()
            }
        }
    }

    override fun exit() {
        // Only die when quiescent — an in-flight grant must finish first.
        if (active.get() == 0) {
            System.exit(0)
        }
    }

    private companion object {
        const val COMMAND_TIMEOUT_MS = 10_000L
        const val COMMAND_TIMEOUT_EXIT = -3
    }
}
