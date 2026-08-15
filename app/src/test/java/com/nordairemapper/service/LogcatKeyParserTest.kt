package com.nordairemapper.service

import org.junit.Assert.assertEquals
import org.junit.Test

class LogcatKeyParserTest {

    private val downClick =
        "D/KEYLOG_PhoneWindowManagerExtImpl( 3174): overrideInterceptKeyBeforeQueueing:" +
            "KeyEvent { action=ACTION_DOWN, keyCode=KEYCODE_ACTION_BUTTON_CLICK, scanCode=735 }"
    private val upClick =
        "D/KEYLOG_PhoneWindowManagerExtImpl( 3174): overrideInterceptKeyBeforeQueueing:" +
            "KeyEvent { action=ACTION_UP, keyCode=KEYCODE_ACTION_BUTTON_CLICK, scanCode=735 }"
    private val undefined =
        "I/KEYLOG_OplusKeyEventUtil( 3174): should not notify undefined keys in restrict listen mode"
    private val selfLog =
        "D/LogcatWatcher( 2257): edge=DOWN from: I/KEYLOG_OplusKeyEventUtil( 3174): should not notify undefined keys"

    @Test
    fun `undefined does not parse as DOWN`() {
        assertEquals(KeyAction.PULSE, LogcatKeyParser.parseKeyAction(undefined))
    }

    @Test
    fun `action button click parses down and up`() {
        assertEquals(KeyAction.DOWN, LogcatKeyParser.parseKeyAction(downClick))
        assertEquals(KeyAction.UP, LogcatKeyParser.parseKeyAction(upClick))
    }

    @Test
    fun `self logs are ignored`() {
        assertEquals(true, LogcatKeyParser.isSelfLog(selfLog))
        assertEquals(false, LogcatKeyParser.isSelfLog(undefined))
    }

    @Test
    fun `legacy pattern migrates to action button click`() {
        assertEquals(
            LogcatKeyParser.DEFAULT_LOGCAT_PATTERN,
            LogcatKeyParser.migratePattern(LogcatKeyParser.LEGACY_LOGCAT_PATTERN),
        )
        assertEquals("custom", LogcatKeyParser.migratePattern("custom"))
    }

    @Test
    fun `one tap with action-button pattern is one down and one up`() {
        val edges = feed(
            pattern = LogcatKeyParser.DEFAULT_LOGCAT_PATTERN,
            0L to downClick,
            1L to undefined,
            2L to undefined,
            80L to upClick,
            81L to undefined,
            82L to undefined,
            83L to selfLog,
        )
        assertEquals(listOf(KeyAction.DOWN, KeyAction.UP), edges)
    }

    @Test
    fun `one tap with legacy oplus pattern still coalesces four pulses`() {
        val edges = feed(
            pattern = LogcatKeyParser.LEGACY_LOGCAT_PATTERN,
            0L to undefined,
            1L to undefined,
            80L to undefined,
            81L to undefined,
        )
        assertEquals(listOf(KeyAction.DOWN, KeyAction.UP), edges)
    }

    @Test
    fun `hold is down then up a second later`() {
        val edges = feed(
            pattern = LogcatKeyParser.DEFAULT_LOGCAT_PATTERN,
            0L to downClick,
            1L to undefined,
            1000L to upClick,
            1001L to undefined,
        )
        assertEquals(listOf(KeyAction.DOWN, KeyAction.UP), edges)
    }

    @Test
    fun `double tap is two down-up pairs`() {
        val edges = feed(
            pattern = LogcatKeyParser.DEFAULT_LOGCAT_PATTERN,
            0L to downClick,
            60L to upClick,
            180L to downClick,
            240L to upClick,
        )
        assertEquals(
            listOf(KeyAction.DOWN, KeyAction.UP, KeyAction.DOWN, KeyAction.UP),
            edges,
        )
    }

    @Test
    fun `duplicate buffer copies of the same down are ignored`() {
        val edges = feed(
            pattern = LogcatKeyParser.DEFAULT_LOGCAT_PATTERN,
            0L to downClick,
            0L to downClick,
            90L to upClick,
            90L to upClick,
        )
        assertEquals(listOf(KeyAction.DOWN, KeyAction.UP), edges)
    }

    private fun feed(pattern: String, vararg lines: Pair<Long, String>): List<KeyAction> {
        val coalescer = LogcatKeyEdgeCoalescer()
        return lines.mapNotNull { (at, line) ->
            if (LogcatKeyParser.isSelfLog(line)) return@mapNotNull null
            if (!line.contains(pattern, ignoreCase = true)) return@mapNotNull null
            coalescer.accept(LogcatKeyParser.parseKeyAction(line), at)
        }
    }
}
