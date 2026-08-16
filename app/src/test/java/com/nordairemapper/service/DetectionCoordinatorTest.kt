package com.nordairemapper.service

import com.nordairemapper.domain.model.DetectionStrategy
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectionCoordinatorTest {

    @Test
    fun autoAcceptsBothSources() {
        assertTrue(
            DetectionCoordinator.acceptsSource(
                DetectionStrategy.AUTO,
                DetectionStrategy.ACCESSIBILITY,
            ),
        )
        assertTrue(
            DetectionCoordinator.acceptsSource(
                DetectionStrategy.AUTO,
                DetectionStrategy.LOGCAT,
            ),
        )
    }

    @Test
    fun accessibilityAcceptsLogcatCompanion() {
        assertTrue(
            DetectionCoordinator.acceptsSource(
                DetectionStrategy.ACCESSIBILITY,
                DetectionStrategy.ACCESSIBILITY,
            ),
        )
        assertTrue(
            DetectionCoordinator.acceptsSource(
                DetectionStrategy.ACCESSIBILITY,
                DetectionStrategy.LOGCAT,
            ),
        )
    }

    @Test
    fun logcatRejectsAccessibility() {
        assertFalse(
            DetectionCoordinator.acceptsSource(
                DetectionStrategy.LOGCAT,
                DetectionStrategy.ACCESSIBILITY,
            ),
        )
        assertTrue(
            DetectionCoordinator.acceptsSource(
                DetectionStrategy.LOGCAT,
                DetectionStrategy.LOGCAT,
            ),
        )
    }

    @Test
    fun allStrategiesNeedLogcatWatcherOnNord() {
        DetectionStrategy.entries.forEach { strategy ->
            assertTrue(DetectionCoordinator.needsLogcatWatcher(strategy))
        }
    }
}
