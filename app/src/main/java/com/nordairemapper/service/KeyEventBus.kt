package com.nordairemapper.service

import com.nordairemapper.domain.model.DetectionStrategy
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

enum class KeyAction {
    DOWN,
    UP,

    /** A completed press reported without separate down/up (logcat source). */
    PULSE,
}

data class RawKeyEvent(
    val keyCode: Int,
    val scanCode: Int,
    val action: KeyAction,
    val timestampMs: Long,
    val source: DetectionStrategy,
)

/**
 * Shared stream of raw hardware key events. Detector sources (accessibility
 * service, logcat watcher) emit into it; the RemapEngine and the key-learning
 * debug screen collect from it.
 */
@Singleton
class KeyEventBus @Inject constructor() {

    private val _rawEvents = MutableSharedFlow<RawKeyEvent>(
        extraBufferCapacity = 64,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val rawEvents: SharedFlow<RawKeyEvent> = _rawEvents

    fun emit(event: RawKeyEvent) {
        _rawEvents.tryEmit(event)
    }
}
