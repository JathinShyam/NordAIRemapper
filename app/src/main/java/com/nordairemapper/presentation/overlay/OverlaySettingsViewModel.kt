package com.nordairemapper.presentation.overlay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nordairemapper.domain.model.OverlayAnimation
import com.nordairemapper.domain.model.OverlayConfig
import com.nordairemapper.domain.model.OverlayIconSize
import com.nordairemapper.domain.model.OverlayLayoutStyle
import com.nordairemapper.domain.model.OverlayPosition
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.domain.repository.RemapConfigRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OverlaySettingsViewModel @Inject constructor(
    private val remapConfigRepository: RemapConfigRepository,
) : ViewModel() {

    val config: StateFlow<OverlayConfig> = remapConfigRepository.observeOverlayConfig()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), OverlayConfig())

    private fun update(transform: (OverlayConfig) -> OverlayConfig) {
        viewModelScope.launch {
            remapConfigRepository.setOverlayConfig(transform(config.value))
        }
    }

    fun setEnabled(enabled: Boolean) = update { it.copy(enabled = enabled) }

    fun setPosition(position: OverlayPosition) = update { it.copy(position = position) }

    fun setOpacity(opacity: Float) = update { it.copy(opacity = opacity.coerceIn(0.3f, 1f)) }

    fun setIconSize(size: OverlayIconSize) = update { it.copy(iconSize = size) }

    fun setAnimation(animation: OverlayAnimation) = update { it.copy(animation = animation) }

    fun setLayoutStyle(style: OverlayLayoutStyle) = update { it.copy(layoutStyle = style) }

    fun setSlot(index: Int, action: RemapAction) = update { current ->
        val slots = current.slots.toMutableList()
        while (slots.size <= index) slots.add(RemapAction.None)
        slots[index] = action
        current.copy(slots = slots.take(OverlayConfig.MAX_SLOTS))
    }

    fun clearSlot(index: Int) = setSlot(index, RemapAction.None)
}
