package com.nordairemapper.domain.model

import kotlinx.serialization.Serializable

enum class OverlayPosition { LEFT_EDGE, RIGHT_EDGE, BOTTOM_CENTER }

enum class OverlayIconSize { SMALL, MEDIUM, LARGE }

enum class OverlayAnimation { FADE, SCALE, SLIDE }

enum class OverlayLayoutStyle { RADIAL, PILL_BAR }

@Serializable
data class OverlayConfig(
    val enabled: Boolean = false,
    val slots: List<RemapAction> = emptyList(),
    val position: OverlayPosition = OverlayPosition.RIGHT_EDGE,
    val opacity: Float = 1f,
    val iconSize: OverlayIconSize = OverlayIconSize.MEDIUM,
    val animation: OverlayAnimation = OverlayAnimation.SCALE,
    val layoutStyle: OverlayLayoutStyle = OverlayLayoutStyle.PILL_BAR,
) {
    companion object {
        const val MAX_SLOTS = 6
    }
}
