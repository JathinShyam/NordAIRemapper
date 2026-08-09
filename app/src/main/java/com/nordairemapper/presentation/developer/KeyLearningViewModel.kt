package com.nordairemapper.presentation.developer

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nordairemapper.domain.model.KeyIdentity
import com.nordairemapper.domain.repository.SettingsRepository
import com.nordairemapper.service.AccessibilityUtils
import com.nordairemapper.service.KeyEventBus
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

@HiltViewModel
class KeyLearningViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keyEventBus: KeyEventBus,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _capturedEvents = MutableStateFlow<List<RawKeyEvent>>(emptyList())
    val capturedEvents: StateFlow<List<RawKeyEvent>> = _capturedEvents.asStateFlow()

    private val _serviceActive = MutableStateFlow(false)
    val serviceActive: StateFlow<Boolean> = _serviceActive.asStateFlow()

    val learnedIdentity: StateFlow<KeyIdentity> = settingsRepository.settings
        .map { it.keyIdentity }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), KeyIdentity.UNCONFIGURED)

    init {
        refreshServiceState()
        viewModelScope.launch {
            keyEventBus.rawEvents.collect { event ->
                _capturedEvents.update { (listOf(event) + it).take(MAX_EVENTS) }
            }
        }
    }

    fun refreshServiceState() {
        _serviceActive.value = AccessibilityUtils.isServiceEnabled(context)
    }

    fun openAccessibilitySettings() = AccessibilityUtils.openAccessibilitySettings(context)

    fun saveAsPlusKey(event: RawKeyEvent) {
        viewModelScope.launch {
            settingsRepository.setKeyIdentity(
                KeyIdentity(keyCode = event.keyCode, scanCode = event.scanCode)
            )
        }
    }

    fun clearEvents() {
        _capturedEvents.value = emptyList()
    }

    private companion object {
        const val MAX_EVENTS = 100
    }
}
