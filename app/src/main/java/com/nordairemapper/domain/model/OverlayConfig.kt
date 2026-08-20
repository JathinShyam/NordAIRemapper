package com.nordairemapper.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/** Vertical placement of the floating overlay panel (grid or pill bar). */
@Serializable(with = OverlayPositionSerializer::class)
enum class OverlayPosition {
    TOP,
    MIDDLE,
    BOTTOM,
}

object OverlayPositionSerializer : KSerializer<OverlayPosition> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("OverlayPosition", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: OverlayPosition) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): OverlayPosition = when (decoder.decodeString()) {
        "TOP" -> OverlayPosition.TOP
        "MIDDLE", "LEFT_EDGE", "RIGHT_EDGE" -> OverlayPosition.MIDDLE
        "BOTTOM", "BOTTOM_CENTER" -> OverlayPosition.BOTTOM
        else -> OverlayPosition.MIDDLE
    }
}

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
    val position: OverlayPosition = OverlayPosition.MIDDLE,
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
