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

    fun onKeyDown() {
        decideJob?.cancel()
        decideJob = null
        longPressFired = false
        longPressJob?.cancel()
        longPressJob = scope.launch {
            delay(timings().longPressThresholdMs)
            longPressFired = true
            reset()
            onGesture(Gesture.LONG_PRESS)
        }
    }

    fun onKeyUp() {
        longPressJob?.cancel()
        longPressJob = null
        if (longPressFired) {
            longPressFired = false
            return
        }
        registerCompletedPress()
    }

    /** For sources that only report completed presses (logcat pulses). */
    fun onPulse() {
        registerCompletedPress()
    }

    private fun registerCompletedPress() {
        tapCount++
        if (tapCount >= 2) {
            reset()
            onGesture(Gesture.DOUBLE_PRESS)
        } else {
            decideJob = scope.launch {
                delay(timings().doublePressWindowMs)
                reset()
                onGesture(Gesture.SINGLE_PRESS)
            }
        }
    }

    private fun reset() {
        tapCount = 0
        decideJob?.cancel()
        decideJob = null
    }
}
