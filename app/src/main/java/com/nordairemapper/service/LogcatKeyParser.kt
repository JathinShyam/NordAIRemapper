package com.nordairemapper.service

import com.nordairemapper.domain.model.AppSettings

/**
 * Turns noisy OnePlus KEYLOG lines into a single DOWN/UP pair per physical press.
 *
 * Nord 5 RCA (one Plus Key tap):
 * - `KEYCODE_ACTION_BUTTON_CLICK` / scanCode 735 with ACTION_DOWN then ACTION_UP
 * - `KEYLOG_OplusKeyEventUtil` "undefined keys" logged **twice on down and twice on up**
 * - Default `logcat` reads multiple buffers, so the same line can appear twice
 * - Logging the matched line re-triggers the watcher (feedback loop)
 *
 * Default match is [DEFAULT_LOGCAT_PATTERN] (`KEYCODE_ACTION_BUTTON_CLICK`) so we
 * see one explicit DOWN and one UP. The coalescer still collapses the legacy
 * OplusKeyEventUtil pulse burst and buffer duplicates.
 */
object LogcatKeyParser {

    const val DEFAULT_LOGCAT_PATTERN = AppSettings.DEFAULT_LOGCAT_PATTERN
    const val LEGACY_LOGCAT_PATTERN = AppSettings.LEGACY_LOGCAT_PATTERN

    fun isSelfLog(line: String): Boolean =
        line.contains("LogcatWatcher") || line.contains("RemapEngine")

    fun migratePattern(stored: String?): String = when (stored) {
        null, LEGACY_LOGCAT_PATTERN -> DEFAULT_LOGCAT_PATTERN
        else -> stored
    }

    fun parseKeyAction(line: String): KeyAction {
        val lower = line.lowercase()
        return when {
            "action_down" in lower -> KeyAction.DOWN
            "action_up" in lower -> KeyAction.UP
            Regex("""action\s*=\s*0\b""").containsMatchIn(lower) -> KeyAction.DOWN
            Regex("""action\s*=\s*1\b""").containsMatchIn(lower) -> KeyAction.UP
            Regex("""(?<![a-z])down(?![a-z])""").containsMatchIn(lower) -> KeyAction.DOWN
            Regex("""(?<![a-z])up(?![a-z])""").containsMatchIn(lower) -> KeyAction.UP
            else -> KeyAction.PULSE
        }
    }
}

/**
 * One physical press → one DOWN then one UP.
 *
 * After DOWN, extra DOWN/PULSE lines are ignored for [echoMs] (duplicate KEYLOG /
 * second buffer). An explicit ACTION_UP always ends the press. After UP, extra
 * pulses in [echoMs] are ignored so they do not start a fake second press.
 */
class LogcatKeyEdgeCoalescer(
    private val echoMs: Long = ECHO_MS,
) {
    private var pressed = false
    private var downAtMs = 0L
    private var lastEmit: KeyAction? = null
    private var lastEmitAtMs = 0L

    fun accept(parsed: KeyAction, nowMs: Long): KeyAction? {
        if (!pressed) {
            if (parsed == KeyAction.UP) return null
            if (lastEmit == KeyAction.UP && nowMs - lastEmitAtMs < echoMs) return null
            return emit(KeyAction.DOWN, nowMs)
        }
        if (parsed == KeyAction.DOWN) return null
        if (parsed == KeyAction.PULSE && nowMs - downAtMs < echoMs) return null
        if (parsed == KeyAction.UP || parsed == KeyAction.PULSE) {
            return emit(KeyAction.UP, nowMs)
        }
        return null
    }

    private fun emit(action: KeyAction, nowMs: Long): KeyAction {
        pressed = action == KeyAction.DOWN
        if (action == KeyAction.DOWN) downAtMs = nowMs
        lastEmit = action
        lastEmitAtMs = nowMs
        return action
    }

    companion object {
        const val ECHO_MS = 40L
    }
}
