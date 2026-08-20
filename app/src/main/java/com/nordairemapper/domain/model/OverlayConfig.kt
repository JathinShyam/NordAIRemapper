package com.nordairemapper.domain.model

import kotlinx.serialization.Serializable

enum class OverlayPosition { LEFT_EDGE, RIGHT_EDGE, BOTTOM_CENTER }

enum class OverlayIconSize { SMALL, MEDIUM, LARGE }

enum class OverlayAnimation { FADE, SCALE, SLIDE }

enum class OverlayLayoutStyle { RADIAL, PILL_BAR }

/** Visual language for the action popup / floating overlay chrome. */
enum class OverlayVisualStyle { ONEPLUS, STOCK }

@Serializable
data class OverlayConfig(
    /** When false, [RemapAction.ShowOverlay] will not open the floating menu. */
    val enabled: Boolean = true,
    val slots: List<RemapAction> = emptyList(),
    val position: OverlayPosition = OverlayPosition.RIGHT_EDGE,
    val opacity: Float = 1f,
    val iconSize: OverlayIconSize = OverlayIconSize.MEDIUM,
    val animation: OverlayAnimation = OverlayAnimation.SCALE,
    val layoutStyle: OverlayLayoutStyle = OverlayLayoutStyle.PILL_BAR,
    val visualStyle: OverlayVisualStyle = OverlayVisualStyle.ONEPLUS,
    /** ARGB accent used by OnePlus-style overlay chrome. */
    val accentColorArgb: Int = DEFAULT_ACCENT_ARGB,
    val glowEffects: Boolean = true,
    /** How long the action popup stays visible (ms). */
    val holdDurationMs: Long = 700L,
) {
    companion object {
        const val MAX_SLOTS = 6
        const val DEFAULT_ACCENT_ARGB = 0xFF0AC6FF.toInt()
    }
}
