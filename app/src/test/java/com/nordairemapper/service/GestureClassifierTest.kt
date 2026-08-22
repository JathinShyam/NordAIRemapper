package com.nordairemapper.service

import com.nordairemapper.domain.model.Gesture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Pins the wait-then-decide contract (TRD §3.3) and documents burst behavior:
 * a triple press is DOUBLE followed by SINGLE after the window — each physical
 * press maps to exactly one gesture.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GestureClassifierTest {

    private fun timings() = GestureClassifier.Timings(
        doublePressWindowMs = 300L,
        longPressThresholdMs = 500L,
    )

    private fun kotlinx.coroutines.test.TestScope.classifier(
        onGesture: (Gesture) -> Unit,
    ): GestureClassifier =
        GestureClassifier(
            scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            timings = ::timings,
            onGesture = onGesture,
        )

    // NOTE: advanceTimeBy leaves tasks due exactly at the boundary un-run,
    // so every "fires at T" assertion advances 2ms past the deadline.

    @Test
    fun `single press fires only after double window expires`() = runTest {
        val fired = mutableListOf<Gesture>()
        val c = classifier { fired += it }
        c.onKeyDown()
        c.onKeyUp()
        advanceTimeBy(299)
        assertEquals(emptyList<Gesture>(), fired)
        advanceTimeBy(2)
        assertEquals(listOf(Gesture.SINGLE_PRESS), fired)
    }

    @Test
    fun `double press fires immediately on second up`() = runTest {
        val fired = mutableListOf<Gesture>()
        val c = classifier { fired += it }
        c.onKeyDown(); c.onKeyUp()
        c.onKeyDown(); c.onKeyUp()
        assertEquals(listOf(Gesture.DOUBLE_PRESS), fired)
    }

    @Test
    fun `long press fires at threshold and its up is ignored`() = runTest {
        val fired = mutableListOf<Gesture>()
        val c = classifier { fired += it }
        c.onKeyDown()
        advanceTimeBy(499)
        assertEquals(emptyList<Gesture>(), fired)
        advanceTimeBy(2)
        assertEquals(listOf(Gesture.LONG_PRESS), fired)
        c.onKeyUp()
        advanceTimeBy(1_000)
        assertEquals(listOf(Gesture.LONG_PRESS), fired)
    }

    @Test
    fun `second press held past long fires long and cancels pending single`() = runTest {
        val fired = mutableListOf<Gesture>()
        val c = classifier { fired += it }
        c.onKeyDown(); c.onKeyUp()          // tap 1 → single pending
        advanceTimeBy(100)
        c.onKeyDown()                        // tap 2 held; pending single cancelled
        advanceTimeBy(502)
        assertEquals(listOf(Gesture.LONG_PRESS), fired)
        c.onKeyUp()                          // long's matching UP is swallowed
        advanceTimeBy(400)
        assertEquals(listOf(Gesture.LONG_PRESS), fired)
    }

    @Test
    fun `triple press is double then single after window`() = runTest {
        val fired = mutableListOf<Gesture>()
        val c = classifier { fired += it }
        repeat(3) { c.onKeyDown(); c.onKeyUp() }
        assertEquals(listOf(Gesture.DOUBLE_PRESS), fired)
        advanceTimeBy(302)
        assertEquals(listOf(Gesture.DOUBLE_PRESS, Gesture.SINGLE_PRESS), fired)
    }

    @Test
    fun `duplicate down while already down is ignored`() = runTest {
        val fired = mutableListOf<Gesture>()
        val c = classifier { fired += it }
        c.onKeyDown()
        c.onKeyDown()
        c.onKeyUp()
        advanceTimeBy(302)
        assertEquals(listOf(Gesture.SINGLE_PRESS), fired)
    }

    @Test
    fun `pulse when idle becomes one completed tap`() = runTest {
        val fired = mutableListOf<Gesture>()
        val c = classifier { fired += it }
        c.onPulse()
        advanceTimeBy(302)
        assertEquals(listOf(Gesture.SINGLE_PRESS), fired)
    }

    @Test
    fun `up with no prior down does nothing`() = runTest {
        val fired = mutableListOf<Gesture>()
        val c = classifier { fired += it }
        c.onKeyUp()
        advanceTimeBy(1_000)
        assertEquals(emptyList<Gesture>(), fired)
    }
}
