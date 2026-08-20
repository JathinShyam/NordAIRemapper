package com.nordairemapper.ui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nordairemapper.domain.model.OverlayConfig
import com.nordairemapper.domain.model.OverlayIconSize
import com.nordairemapper.domain.model.OverlayLayoutStyle
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.presentation.common.conflictKey
import com.nordairemapper.presentation.common.displayName
import com.nordairemapper.presentation.common.icon

private fun previewIconDp(size: OverlayIconSize) = when (size) {
    OverlayIconSize.SMALL -> 13.dp
    OverlayIconSize.MEDIUM -> 16.dp
    OverlayIconSize.LARGE -> 20.dp
}

private fun previewRingDp(size: OverlayIconSize) = when (size) {
    OverlayIconSize.SMALL -> 24.dp
    OverlayIconSize.MEDIUM -> 28.dp
    OverlayIconSize.LARGE -> 34.dp
}

/**
 * In-settings preview of the floating overlay panel.
 * Mirrors the real [FloatingOverlayService] layout and respects
 * [OverlayConfig.layoutStyle], [OverlayConfig.iconSize], and [OverlayConfig.opacity].
 */
@Composable
fun OverlayPreview(
    config: OverlayConfig,
    modifier: Modifier = Modifier,
) {
    val slots = config.slots.filter { it !is RemapAction.None }.take(OverlayConfig.MAX_SLOTS)
    val accent = Color(config.accentColorArgb)
    val alpha = config.opacity.coerceIn(0.3f, 1f)
    val slotKey = slots.joinToString("|") { it.conflictKey() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Color(0xFF141414).copy(alpha = alpha),
                RoundedCornerShape(22.dp),
            )
            .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 16.dp),
    ) {
        AnimatedContent(
            targetState = slotKey to config.layoutStyle,
            transitionSpec = {
                (fadeIn() + scaleIn(initialScale = 0.92f)) togetherWith
                    (fadeOut() + scaleOut(targetScale = 0.96f))
            },
            label = "overlayPreviewSlots",
            modifier = Modifier.fillMaxWidth(),
        ) { (key, layoutStyle) ->
            val animatedSlots = if (key.isEmpty()) {
                emptyList()
            } else {
                slots
            }
            if (animatedSlots.isEmpty()) {
                Text(
                    text = "No overlay slots configured",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    textAlign = TextAlign.Center,
                )
            } else {
                when (layoutStyle) {
                    OverlayLayoutStyle.GRID -> {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                animatedSlots.take(3).forEach { action ->
                                    PreviewTile(
                                        action = action,
                                        accent = accent,
                                        iconSize = config.iconSize,
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                repeat((3 - animatedSlots.take(3).size).coerceAtLeast(0)) {
                                    Box(modifier = Modifier.weight(1f))
                                }
                            }
                            if (animatedSlots.size > 3) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    animatedSlots.drop(3).forEach { action ->
                                        PreviewTile(
                                            action = action,
                                            accent = accent,
                                            iconSize = config.iconSize,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                    repeat((3 - animatedSlots.drop(3).size).coerceAtLeast(0)) {
                                        Box(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        }
                    }

                    OverlayLayoutStyle.PILL_BAR -> {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            val tileW = when (config.iconSize) {
                                OverlayIconSize.SMALL -> 48.dp
                                OverlayIconSize.MEDIUM -> 54.dp
                                OverlayIconSize.LARGE -> 60.dp
                            }
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                animatedSlots.take(3).forEach { action ->
                                    PreviewPillTile(
                                        action = action,
                                        accent = accent,
                                        iconSize = config.iconSize,
                                        tileWidth = tileW,
                                    )
                                }
                            }
                            if (animatedSlots.size > 3) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    verticalAlignment = Alignment.Top,
                                ) {
                                    animatedSlots.drop(3).forEach { action ->
                                        PreviewPillTile(
                                            action = action,
                                            accent = accent,
                                            iconSize = config.iconSize,
                                            tileWidth = tileW,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewTile(
    action: RemapAction,
    accent: Color,
    iconSize: OverlayIconSize,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color(0xFF171717), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(previewRingDp(iconSize))
                    .background(accent.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                    .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = action.icon(),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(previewIconDp(iconSize)),
                )
            }
            Text(
                text = action.displayName(),
                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun PreviewPillTile(
    action: RemapAction,
    accent: Color,
    iconSize: OverlayIconSize,
    tileWidth: Dp,
) {
    Column(
        modifier = Modifier.width(tileWidth),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(
            modifier = Modifier
                .size(previewRingDp(iconSize))
                .background(accent.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon(),
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(previewIconDp(iconSize)),
            )
        }
        Text(
            text = action.displayName(),
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
        )
    }
}
