package com.nordairemapper.service.shizuku

/**
 * Runs inside the Shizuku server process. Each Unlock shell command is
 * executed with shell-level privileges — the same effect as typing it over
 * adb, without a computer.
 */
class ShizukuGrantService : IGrantService.Stub() {

    override fun runCommand(command: String?): Int {
        if (command.isNullOrBlank()) return -1
        val process = ProcessBuilder("sh", "-c", command)
            .redirectErrorStream(true)
            .start()
        // Drain output so the process cannot block on a full pipe.
        process.inputStream.readBytes()
        return process.waitFor()
    }

    override fun exit() {
        System.exit(0)
    }
}
