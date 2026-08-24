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
    OPEN_ENABLE_DETECTION,
}

data class HomeUiState(
    val serviceEnabled: Boolean = true,
    val accessibilityEnabled: Boolean = false,
    val detectionStrategy: DetectionStrategy = DetectionStrategy.AUTO,
    val keyConfigured: Boolean = false,
    val readLogsGranted: Boolean = false,
    /** False ⇒ logcat tail never saw another app's line — detection may be blind. */
    val tailSawNonSelf: Boolean = true,
    /** POST_NOTIFICATIONS granted; false means every failure alert is silenced. */
    val notificationsEnabled: Boolean = true,
    val actions: Map<PressType, RemapAction> = PressType.entries.associateWith { RemapAction.None },
    val conflictPressTypes: Set<PressType> = emptySet(),
    val banner: HomeBanner? = null,
    val lastPlusKeySeenAtMs: Long = 0L,
    /** Ticking clock so "last used" text stays fresh while Home is visible. */
    val nowMs: Long = System.currentTimeMillis(),
)
