package com.nordairemapper.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nordairemapper.domain.model.OverlayAnimation
import com.nordairemapper.domain.model.OverlayVisualStyle

private const val TAG = "VisualActionPopup"

/**
 * Plus Key geometry on Nord 5 — matches [PhoneDiagram]
 * (top 31.5 / 163.4, height 8.5 / 163.4).
 */
const val VISUAL_OVERLAY_KEY_TOP_FRACTION = 31.5f / 163.4f
const val VISUAL_OVERLAY_KEY_HEIGHT_FRACTION = 8.5f / 163.4f

/** Vertical center of the Plus Key in diagram space. */
const val VISUAL_OVERLAY_KEY_CENTER_FRACTION =
    VISUAL_OVERLAY_KEY_TOP_FRACTION + VISUAL_OVERLAY_KEY_HEIGHT_FRACTION * 0.5f

/**
 * Nord 5 full-screen overlay: diagram center reads low vs the physical key.
 * Fixed dp raise plus [VISUAL_OVERLAY_SCREEN_RAISE_FRACTION] of screen height.
 */
val VISUAL_OVERLAY_SCREEN_RAISE = 40.dp
/** Halfway between 40dp-only (~473px) and 40dp+5% (~333px) on Nord 5. */
const val VISUAL_OVERLAY_SCREEN_RAISE_FRACTION = 0.025f

/** Downward nudge after raise (~25px @ 560dpi: prior ~15px + ~10px). */
val VISUAL_OVERLAY_SCREEN_NUDGE_DOWN = 7.5.dp

/** Pill vertical anchor in px — center of popup aligned to Plus Key, then raised. */
fun computeVisualOverlayAnchorY(screenHeightPx: Float, density: Float): Float {
    val fixedRaisePx = VISUAL_OVERLAY_SCREEN_RAISE.value * density
    val fractionalRaisePx = screenHeightPx * VISUAL_OVERLAY_SCREEN_RAISE_FRACTION
    val nudgeDownPx = VISUAL_OVERLAY_SCREEN_NUDGE_DOWN.value * density
    return screenHeightPx * VISUAL_OVERLAY_KEY_CENTER_FRACTION -
        fixedRaisePx - fractionalRaisePx + nudgeDownPx
}

/**
 * Edge-aligned action popup shown when a remap fires (Visual Overlay).
 * Sits on the **left** screen edge at Plus Key height — same geometry as [PhoneDiagram].
 */
@Composable
fun VisualActionPopupLayer(
    icon: ImageVector,
    accent: Color,
    visualStyle: OverlayVisualStyle,
    glowEffects: Boolean,
    animation: OverlayAnimation,
    modifier: Modifier = Modifier,
    caption: String? = null,
    /** Real launcher icon for Launch App; falls back to [icon] when null. */
    appIcon: ImageBitmap? = null,
    /** When true, sizes are scaled down for the in-settings phone mock. */
    previewScale: Float = 1f,
) {
    val appear = remember { Animatable(0f) }
    LaunchedEffect(animation) {
        appear.snapTo(0f)
        appear.animateTo(1f, animationSpec = tween(220))
    }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val screenHeightPx = maxHeight.value * density.density
        val anchorY = computeVisualOverlayAnchorY(screenHeightPx, density.density)
        val anchorDp = (anchorY / density.density).dp

        SideEffect {
            android.util.Log.d(
                TAG,
                "anchorY=${anchorY.toInt()}px screen=${screenHeightPx.toInt()}px " +
                    "centerFrac=$VISUAL_OVERLAY_KEY_CENTER_FRACTION raise=$VISUAL_OVERLAY_SCREEN_RAISE",
            )
        }

        val edgeInset = (4.dp * previewScale).coerceAtLeast(2.dp)
        val pillWidth = (52.dp * previewScale).coerceAtLeast(40.dp)
        val pillHeight = (68.dp * previewScale).coerceAtLeast(52.dp)
        val iconSize = (28.dp * previewScale).coerceAtLeast(20.dp)
        val glowWidth = (10.dp * previewScale).coerceAtLeast(6.dp)

        if (glowEffects) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val keyY = computeVisualOverlayAnchorY(size.height, density.density)
                        // Vertical accent stripe on the left edge at Plus Key height.
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    accent.copy(alpha = 0.85f),
                                    accent.copy(alpha = 0.95f),
                                    accent.copy(alpha = 0.85f),
                                    Color.Transparent,
                                ),
                                startY = keyY - size.height * 0.12f,
                                endY = keyY + size.height * 0.12f,
                            ),
                            topLeft = Offset(0f, 0f),
                            size = androidx.compose.ui.geometry.Size(
                                glowWidth.toPx(),
                                size.height,
                            ),
                        )
                        // Soft bloom at the Plus Key contact point (left edge).
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    accent.copy(alpha = 0.55f),
                                    accent.copy(alpha = 0.18f),
                                    Color.Transparent,
                                ),
                                center = Offset(glowWidth.toPx(), keyY),
                                radius = size.minDimension * 0.14f,
                            ),
                            radius = size.minDimension * 0.14f,
                            center = Offset(glowWidth.toPx(), keyY),
                        )
                    },
            )
        }

        Box(
            modifier = Modifier
                .align(Alignment.TopStart)
                .offset(
                    x = edgeInset,
                    y = anchorDp - pillHeight / 2,
                )
                .graphicsLayer {
                    val t = appear.value
                    alpha = t
                    when (animation) {
                        OverlayAnimation.FADE -> Unit
                        OverlayAnimation.SCALE -> {
                            scaleX = 0.82f + 0.18f * t
                            scaleY = 0.82f + 0.18f * t
                        }
                        OverlayAnimation.SLIDE -> {
                            translationX = -(1f - t) * 24f
                        }
                    }
                },
        ) {
            VisualActionPopupPill(
                icon = icon,
                caption = caption,
                accent = accent,
                visualStyle = visualStyle,
                glowEffects = glowEffects,
                appIcon = appIcon,
                pillWidth = pillWidth,
                pillHeight = pillHeight,
                iconSize = iconSize,
            )
        }
    }
}

@Composable
fun VisualActionPopupPill(
    icon: ImageVector,
    accent: Color,
    visualStyle: OverlayVisualStyle,
    glowEffects: Boolean,
    modifier: Modifier = Modifier,
    caption: String? = null,
    appIcon: ImageBitmap? = null,
    pillWidth: Dp = 52.dp,
    pillHeight: Dp = 68.dp,
    iconSize: Dp = 28.dp,
) {
    val onePlus = visualStyle == OverlayVisualStyle.ONEPLUS
    val shape = if (onePlus) RoundedCornerShape(26.dp) else CircleShape
    val surface = if (onePlus) Color(0xFF0A0A0A) else Color(0xFFECECEC)
    val iconTint = if (onePlus) accent else Color(0xFF424242)
    val hasCaption = !caption.isNullOrBlank()
    val contentHeight = if (onePlus && hasCaption) pillHeight else pillWidth
    val glyphSize = if (hasCaption) iconSize * 0.85f else iconSize

    Box(
        modifier = modifier
            .size(width = pillWidth, height = contentHeight)
            .then(
                if (glowEffects && onePlus) {
                    Modifier.border(1.5.dp, accent.copy(alpha = 0.9f), shape)
                } else if (!onePlus) {
                    Modifier.border(1.dp, Color(0xFFBDBDBD), shape)
                } else {
                    Modifier
                },
            )
            .background(surface.copy(alpha = 0.96f), shape)
            .padding(horizontal = 8.dp, vertical = if (hasCaption) 8.dp else 10.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (appIcon != null) {
                Image(
                    bitmap = appIcon,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .size(glyphSize)
                        .clip(RoundedCornerShape(6.dp)),
                )
            } else {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconTint,
                    modifier = Modifier.size(glyphSize),
                )
            }
            if (hasCaption) {
                Text(
                    text = caption,
                    color = iconTint,
                    style = MaterialTheme.typography.labelSmall.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 9.sp,
                        letterSpacing = 0.2.sp,
                    ),
                )
            }
        }
    }
}
