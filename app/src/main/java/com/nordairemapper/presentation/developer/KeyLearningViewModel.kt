package com.nordairemapper.presentation.developer

import android.content.Context
import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nordairemapper.domain.model.DetectionStrategy
import com.nordairemapper.domain.model.KeyIdentity
import com.nordairemapper.domain.repository.SettingsRepository
import com.nordairemapper.service.AccessibilityUtils
import com.nordairemapper.service.KeyAction
import com.nordairemapper.service.KeyEventBus
import com.nordairemapper.service.LearningMode
import com.nordairemapper.service.RawKeyEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** One physical press (DOWN+UP merged, or a logcat PULSE). */
data class CapturedPress(
    val id: Long,
    val keyCode: Int,
    val scanCode: Int,
    val source: DetectionStrategy,
    val timestampMs: Long,
    val durationMs: Long?,
    val complete: Boolean,
) {
    val label: String
        get() = when {
            source == DetectionStrategy.LOGCAT -> "Plus Key (logcat)"
            keyCode == KeyEvent.KEYCODE_VOLUME_UP -> "Volume up"
            keyCode == KeyEvent.KEYCODE_VOLUME_DOWN -> "Volume down"
            keyCode == KeyEvent.KEYCODE_POWER -> "Power"
            keyCode == KeyEvent.KEYCODE_ASSIST || keyCode == KeyEvent.KEYCODE_VOICE_ASSIST -> "Assist"
            keyCode == KeyEvent.KEYCODE_UNKNOWN || keyCode == 0 -> "Unknown key"
            else -> KeyEvent.keyCodeToString(keyCode).removePrefix("KEYCODE_")
        }

    val isLogcatPlusKey: Boolean get() = source == DetectionStrategy.LOGCAT

    /**
     * Volume/power must keep passing through to the system; learning one as
     * the Plus Key would make remapping consume a hardware key users need.
     */
    val isSystemKey: Boolean get() = keyCode == KeyEvent.KEYCODE_VOLUME_UP ||
        keyCode == KeyEvent.KEYCODE_VOLUME_DOWN ||
        keyCode == KeyEvent.KEYCODE_POWER
}

@HiltViewModel
class KeyLearningViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keyEventBus: KeyEventBus,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private var nextId = 0L

    private val _capturedPresses = MutableStateFlow<List<CapturedPress>>(emptyList())
    val capturedPresses: StateFlow<List<CapturedPress>> = _capturedPresses.asStateFlow()

    private val _serviceActive = MutableStateFlow(false)
    val serviceActive: StateFlow<Boolean> = _serviceActive.asStateFlow()

    val learnedIdentity: StateFlow<KeyIdentity> = settingsRepository.settings
        .map { it.keyIdentity }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KeyIdentity.UNCONFIGURED)

    /** Volume keys arrived via Accessibility, but the Plus Key did not. */
    val plusKeyMissingHint: StateFlow<Boolean> = _capturedPresses
        .map { presses ->
            presses.isNotEmpty() &&
                presses.none { it.isLogcatPlusKey } &&
                presses.none { press ->
                    press.keyCode == KeyEvent.KEYCODE_UNKNOWN ||
                        press.keyCode == 0 ||
                        press.keyCode == KeyEvent.KEYCODE_ASSIST ||
                        press.keyCode == KeyEvent.KEYCODE_VOICE_ASSIST
                }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    val lastPlusKeySeenAtMs: StateFlow<Long> = settingsRepository.settings
        .map { it.lastPlusKeySeenAtMs }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0L)

    val logcatPlusKeySeen: StateFlow<Boolean> = _capturedPresses
        .map { presses -> presses.any { it.isLogcatPlusKey } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), false)

    init {
        // While Key setup is open, the engine must not dispatch actions for
        // presses (or consume keys) — learning is a setup flow. Token-based:
        // a recreated instance's end() can't disable a newer instance's gate.
        LearningMode.begin(this)
        refreshServiceState()
        viewModelScope.launch {
            keyEventBus.rawEvents.collect(::onRawEvent)
        }
    }

    override fun onCleared() {
        LearningMode.end(this)
        super.onCleared()
    }

    fun refreshServiceState() {
        _serviceActive.value = AccessibilityUtils.isServiceEnabled(context)
    }

    fun openAccessibilitySettings() = AccessibilityUtils.openAccessibilitySettings(context)

    fun saveAsPlusKey(press: CapturedPress) {
        viewModelScope.launch {
            settingsRepository.setKeyIdentity(
                KeyIdentity(keyCode = press.keyCode, scanCode = press.scanCode)
            )
        }
    }

    fun clearEvents() {
        _capturedPresses.value = emptyList()
    }

    private fun onRawEvent(event: RawKeyEvent) {
        _capturedPresses.update { current ->
            when (event.action) {
                KeyAction.DOWN -> {
                    val openLogcat = event.source == DetectionStrategy.LOGCAT &&
                        current.any { !it.complete && it.source == DetectionStrategy.LOGCAT }
                    if (openLogcat) {
                        current
                    } else {
                        val press = CapturedPress(
                            id = nextId++,
                            keyCode = event.keyCode,
                            scanCode = event.scanCode,
                            source = event.source,
                            timestampMs = event.timestampMs,
                            durationMs = null,
                            complete = false,
                        )
                        (listOf(press) + current).take(MAX_PRESSES)
                    }
                }
                KeyAction.UP -> {
                    val index = current.indexOfFirst {
                        !it.complete &&
                            it.keyCode == event.keyCode &&
                            it.scanCode == event.scanCode &&
                            it.source == event.source
                    }
                    if (index < 0) {
                        if (event.source == DetectionStrategy.LOGCAT) {
                            current
                        } else {
                            val press = CapturedPress(
                                id = nextId++,
                                keyCode = event.keyCode,
                                scanCode = event.scanCode,
                                source = event.source,
                                timestampMs = event.timestampMs,
                                durationMs = null,
                                complete = true,
                            )
                            (listOf(press) + current).take(MAX_PRESSES)
                        }
                    } else {
                        val down = current[index]
                        current.toMutableList().apply {
                            this[index] = down.copy(
                                complete = true,
                                durationMs = (event.timestampMs - down.timestampMs).coerceAtLeast(0),
                            )
                        }
                    }
                }
                KeyAction.PULSE -> {
                    if (event.source == DetectionStrategy.LOGCAT) {
                        val open = current.indexOfFirst {
                            !it.complete && it.source == DetectionStrategy.LOGCAT
                        }
                        if (open >= 0) {
                            val down = current[open]
                            current.toMutableList().apply {
                                this[open] = down.copy(
                                    complete = true,
                                    durationMs = (event.timestampMs - down.timestampMs).coerceAtLeast(0),
                                )
                            }
                        } else {
                            current
                        }
                    } else {
                        val press = CapturedPress(
                            id = nextId++,
                            keyCode = event.keyCode,
                            scanCode = event.scanCode,
                            source = event.source,
                            timestampMs = event.timestampMs,
                            durationMs = null,
                            complete = true,
                        )
                        (listOf(press) + current).take(MAX_PRESSES)
                    }
                }
            }
        }
    }

    private companion object {
        const val MAX_PRESSES = 50
    }
}
