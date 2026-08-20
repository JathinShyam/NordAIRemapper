package com.nordairemapper.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import com.nordairemapper.domain.model.OverlayConfig
import com.nordairemapper.domain.model.OverlayIconSize
import com.nordairemapper.domain.model.OverlayLayoutStyle
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.presentation.common.displayName
import com.nordairemapper.presentation.common.icon
import androidx.compose.ui.graphics.Color
import com.nordairemapper.ui.theme.SurfaceDark

@Composable
fun OverlayPreview(
    config: OverlayConfig,
    modifier: Modifier = Modifier,
) {
    val slots = config.slots.filter { it !is RemapAction.None }.take(OverlayConfig.MAX_SLOTS)
    val iconDp = when (config.iconSize) {
        OverlayIconSize.SMALL -> 28.dp
        OverlayIconSize.MEDIUM -> 36.dp
        OverlayIconSize.LARGE -> 44.dp
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(SurfaceDark, RoundedCornerShape(16.dp))
            .alpha(config.opacity.coerceIn(0.3f, 1f))
            .padding(16.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (slots.isEmpty()) {
            Text(
                text = "No overlay slots yet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else when (config.layoutStyle) {
            OverlayLayoutStyle.PILL_BAR -> {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    slots.forEach { SlotChip(it, iconDp, Color(config.accentColorArgb)) }
                }
            }
            OverlayLayoutStyle.RADIAL -> {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        slots.take(3).forEach { SlotChip(it, iconDp, Color(config.accentColorArgb)) }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        slots.drop(3).forEach { SlotChip(it, iconDp, Color(config.accentColorArgb)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun SlotChip(action: RemapAction, iconDp: androidx.compose.ui.unit.Dp, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .size(iconDp + 12.dp)
                .background(accent.copy(alpha = 0.2f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon(),
                contentDescription = action.displayName(),
                tint = accent,
                modifier = Modifier.size(iconDp * 0.6f),
            )
        }
        Text(
            text = action.displayName(),
            style = MaterialTheme.typography.labelSmall,
            maxLines = 1,
        )
    }
}
