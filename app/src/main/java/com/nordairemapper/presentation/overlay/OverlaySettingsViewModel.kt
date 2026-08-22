package com.nordairemapper.presentation.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nordairemapper.domain.model.OverlayAnimation
import com.nordairemapper.domain.model.OverlayConfig
import com.nordairemapper.domain.model.OverlayIconSize
import com.nordairemapper.domain.model.OverlayLayoutStyle
import com.nordairemapper.domain.model.OverlayPosition
import com.nordairemapper.domain.model.OverlayVisualStyle
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.domain.model.AppSettings
import com.nordairemapper.domain.model.coerceFor
import com.nordairemapper.domain.repository.RemapConfigRepository
import com.nordairemapper.presentation.remap.InstalledAppInfo
import com.nordairemapper.presentation.remap.queryLaunchableApps
import android.content.Context
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OverlaySettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val remapConfigRepository: RemapConfigRepository,
) : ViewModel() {

    private val _installedApps = MutableStateFlow<List<InstalledAppInfo>>(emptyList())
    val installedApps: StateFlow<List<InstalledAppInfo>> = _installedApps.asStateFlow()

    private val _loadingApps = MutableStateFlow(false)
    val loadingApps: StateFlow<Boolean> = _loadingApps.asStateFlow()

    fun loadInstalledApps() {
        viewModelScope.launch {
            if (_loadingApps.value || _installedApps.value.isNotEmpty()) return@launch
            _loadingApps.value = true
            runCatching { queryLaunchableApps(context) }
                .onSuccess { _installedApps.value = it }
            _loadingApps.value = false
        }
    }

    val config: StateFlow<OverlayConfig> = remapConfigRepository.observeOverlayConfig()
        .map { it.copy(position = it.position.coerceFor(it.layoutStyle)) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverlayConfig())

    private fun update(transform: (OverlayConfig) -> OverlayConfig) {
        viewModelScope.launch {
            remapConfigRepository.setOverlayConfig(transform(config.value))
        }
    }

    fun setEnabled(enabled: Boolean) = update { it.copy(enabled = enabled) }

    fun setPosition(position: OverlayPosition) = update {
        it.copy(position = position.coerceFor(it.layoutStyle))
    }

    fun setOpacity(opacity: Float) = update { it.copy(opacity = opacity.coerceIn(0.3f, 1f)) }

    fun setIconSize(size: OverlayIconSize) = update { it.copy(iconSize = size) }

    fun setAnimation(animation: OverlayAnimation) = update { it.copy(animation = animation) }

    fun setLayoutStyle(style: OverlayLayoutStyle) = update {
        it.copy(
            layoutStyle = style,
            position = it.position.coerceFor(style),
        )
    }

    fun setVisualStyle(style: OverlayVisualStyle) = update { it.copy(visualStyle = style) }

    fun setAccentColor(argb: Int) = update { it.copy(accentColorArgb = argb) }

    fun setGlowEffects(enabled: Boolean) = update { it.copy(glowEffects = enabled) }

    fun setHoldDurationMs(ms: Long) = update {
        it.copy(holdDurationMs = ms.coerceIn(AppSettings.HOLD_DURATION_RANGE_MS))
    }

    fun setSlot(index: Int, action: RemapAction) = update { current ->
        val slots = current.slots.toMutableList()
        while (slots.size <= index) slots.add(RemapAction.None)
        slots[index] = action
        val next = slots.take(OverlayConfig.MAX_SLOTS)
        current.copy(
            slots = next,
            enabled = if (action !is RemapAction.None) true else current.enabled,
        )
    }

    fun clearSlot(index: Int) = setSlot(index, RemapAction.None)
}
