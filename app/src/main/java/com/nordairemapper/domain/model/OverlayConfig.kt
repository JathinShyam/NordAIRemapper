package com.nordairemapper.domain.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Overlay placement.
 * - [GRID]: [TOP] / [MIDDLE] / [BOTTOM]
 * - [OverlayLayoutStyle.PILL_BAR]: [LEFT] / [RIGHT] vertical strip, or [BOTTOM]
 *   as a single horizontal scrollable row
 */
@Serializable(with = OverlayPositionSerializer::class)
enum class OverlayPosition {
    TOP,
    MIDDLE,
    BOTTOM,
    LEFT,
    RIGHT,
}

object OverlayPositionSerializer : KSerializer<OverlayPosition> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("OverlayPosition", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: OverlayPosition) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): OverlayPosition = when (decoder.decodeString()) {
        "TOP" -> OverlayPosition.TOP
        "MIDDLE" -> OverlayPosition.MIDDLE
        "BOTTOM", "BOTTOM_CENTER" -> OverlayPosition.BOTTOM
        "LEFT", "LEFT_EDGE" -> OverlayPosition.LEFT
        "RIGHT", "RIGHT_EDGE" -> OverlayPosition.RIGHT
        else -> OverlayPosition.MIDDLE
    }
}

/** Positions allowed for a layout style. */
fun OverlayPosition.isValidFor(style: OverlayLayoutStyle): Boolean = when (style) {
    OverlayLayoutStyle.GRID -> this == OverlayPosition.TOP ||
        this == OverlayPosition.MIDDLE ||
        this == OverlayPosition.BOTTOM
    OverlayLayoutStyle.PILL_BAR -> this == OverlayPosition.LEFT ||
        this == OverlayPosition.RIGHT ||
        this == OverlayPosition.BOTTOM
}

fun OverlayPosition.coerceFor(style: OverlayLayoutStyle): OverlayPosition =
    if (isValidFor(style)) this
    else when (style) {
        OverlayLayoutStyle.PILL_BAR -> OverlayPosition.LEFT
        OverlayLayoutStyle.GRID -> OverlayPosition.MIDDLE
    }

enum class OverlayIconSize { SMALL, MEDIUM, LARGE }

enum class OverlayAnimation { FADE, SCALE, SLIDE }

@Serializable(with = OverlayLayoutStyleSerializer::class)
enum class OverlayLayoutStyle { GRID, PILL_BAR }

object OverlayLayoutStyleSerializer : KSerializer<OverlayLayoutStyle> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("OverlayLayoutStyle", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: OverlayLayoutStyle) {
        encoder.encodeString(value.name)
    }

    override fun deserialize(decoder: Decoder): OverlayLayoutStyle = when (decoder.decodeString()) {
        "GRID", "RADIAL" -> OverlayLayoutStyle.GRID
        "PILL_BAR" -> OverlayLayoutStyle.PILL_BAR
        else -> OverlayLayoutStyle.PILL_BAR
    }
}

/** Visual language for the action popup / floating overlay chrome. */
enum class OverlayVisualStyle { ONEPLUS, STOCK }

@Serializable
data class OverlayConfig(
    /** When false, [RemapAction.ShowOverlay] will not open the floating menu. */
    val enabled: Boolean = true,
    val slots: List<RemapAction> = emptyList(),
    /** Default LEFT — pill bar (default layout) sits on the Plus Key edge. */
    val position: OverlayPosition = OverlayPosition.LEFT,
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
