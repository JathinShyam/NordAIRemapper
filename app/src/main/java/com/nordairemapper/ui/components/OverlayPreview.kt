package com.nordairemapper.ui.components

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nordairemapper.domain.model.OverlayConfig
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.presentation.common.displayName
import com.nordairemapper.presentation.common.icon

@Composable
fun OverlayPreview(
    config: OverlayConfig,
    modifier: Modifier = Modifier,
) {
    val slots = config.slots.filter { it !is RemapAction.None }.take(OverlayConfig.MAX_SLOTS)
    val accent = Color(config.accentColorArgb)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(Color(0xFF141414), RoundedCornerShape(22.dp))
            .border(1.dp, Color(0xFF2C2C2C), RoundedCornerShape(22.dp))
            .padding(horizontal = 14.dp, vertical = 16.dp),
    ) {
        if (slots.isEmpty()) {
            Text(
                text = "No overlay slots configured",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.align(Alignment.Center),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                // Row 1 — slots 0..2
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    slots.take(3).forEach { action ->
                        PreviewTile(action = action, accent = accent, modifier = Modifier.weight(1f))
                    }
                    repeat((3 - slots.take(3).size).coerceAtLeast(0)) {
                        Box(modifier = Modifier.weight(1f))
                    }
                }
                // Row 2 — slots 3..5
                if (slots.size > 3) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        slots.drop(3).forEach { action ->
                            PreviewTile(action = action, accent = accent, modifier = Modifier.weight(1f))
                        }
                        repeat((3 - slots.drop(3).size).coerceAtLeast(0)) {
                            Box(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PreviewTile(action: RemapAction, accent: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .background(Color(0xFF171717), RoundedCornerShape(16.dp))
            .border(1.dp, Color(0xFF2A2A2A), RoundedCornerShape(16.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(accent.copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                    .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = action.icon(),
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(16.dp),
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
