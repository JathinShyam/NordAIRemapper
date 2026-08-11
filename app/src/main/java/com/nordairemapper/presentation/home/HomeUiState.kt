package com.nordairemapper.presentation.home

import com.nordairemapper.domain.model.DetectionStrategy
import com.nordairemapper.domain.model.PressType
import com.nordairemapper.domain.model.RemapAction

data class HomeBanner(
    val title: String,
    val body: String,
    val primaryLabel: String,
    val primaryAction: HomeBannerAction,
)

enum class HomeBannerAction {
    OPEN_ACCESSIBILITY,
    OPEN_KEY_LEARNING,
    OPEN_DEVELOPER,
}

data class HomeUiState(
    val serviceEnabled: Boolean = true,
    val accessibilityEnabled: Boolean = false,
    val detectionStrategy: DetectionStrategy = DetectionStrategy.ACCESSIBILITY,
    val keyConfigured: Boolean = false,
    val readLogsGranted: Boolean = false,
    val actions: Map<PressType, RemapAction> = PressType.entries.associateWith { RemapAction.None },
    val conflictPressTypes: Set<PressType> = emptySet(),
    val banner: HomeBanner? = null,
)
