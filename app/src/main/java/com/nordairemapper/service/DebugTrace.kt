package com.nordairemapper.service

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Durable diagnostic trace for flows whose logcat evidence rotates away too
 * fast on ColorOS (256KB ring). Append-only, size-capped per file.
 * Pull with: adb shell run-as com.nordairemapper cat files/<name>.log
 */
object DebugTrace {

    private const val MAX_BYTES = 256 * 1024L
    private val ts = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    fun log(context: Context, name: String, message: String) {
        runCatching {
            val file = File(context.filesDir, "$name.log")
            if (file.length() > MAX_BYTES) file.delete()
            file.appendText("${ts.format(Date())} $message\n")
        }
    }
}
