package com.nordairemapper.service

import com.nordairemapper.domain.model.Gesture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Wait-then-decide gesture classification. A single press only fires after the
 * double-press window expires without a second press; holding past the
 * long-press threshold fires immediately.
 *
 * Not thread-safe: all entry points must be called from the same dispatcher
 * as [scope] (main thread in practice).
 */
class GestureClassifier(
    private val scope: CoroutineScope,
    private val timings: () -> Timings,
    private val onGesture: (Gesture) -> Unit,
) {
    data class Timings(
        val doublePressWindowMs: Long,
        val longPressThresholdMs: Long,
    )

    private var tapCount = 0
    private var longPressJob: Job? = null
    private var decideJob: Job? = null
    private var longPressFired = false
    private var isDown = false

    fun onKeyDown() {
        if (isDown) return
        isDown = true
        decideJob?.cancel()
        decideJob = null
        longPressFired = false
        longPressJob?.cancel()
        longPressJob = scope.launch {
            delay(timings().longPressThresholdMs)
            longPressFired = true
            isDown = false
            resetTaps()
            onGesture(Gesture.LONG_PRESS)
        }
    }

    fun onKeyUp() {
        if (!isDown && !longPressFired) return
        isDown = false
        longPressJob?.cancel()
        longPressJob = null
        if (longPressFired) {
            longPressFired = false
            return
        }
        registerCompletedPress()
    }

    /**
     * Logcat lines with no down/up hint. Prefer converting those to down/up
     * in the watcher; this remains a fallback completed-tap.
     */
    fun onPulse() {
        if (isDown) {
            onKeyUp()
        } else {
            onKeyDown()
            onKeyUp()
        }
    }

    private fun registerCompletedPress() {
        tapCount++
        if (tapCount >= 2) {
            resetTaps()
            onGesture(Gesture.DOUBLE_PRESS)
        } else {
            decideJob = scope.launch {
                delay(timings().doublePressWindowMs)
                resetTaps()
                onGesture(Gesture.SINGLE_PRESS)
            }
        }
    }

    private fun resetTaps() {
        tapCount = 0
        decideJob?.cancel()
        decideJob = null
    }
}
