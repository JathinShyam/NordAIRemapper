package com.nordairemapper.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.nordairemapper.ui.theme.NordBlue
import com.nordairemapper.ui.theme.SurfaceVariantDark

/** Simplified OnePlus Nord 5 silhouette with the side Plus Key highlighted. */
@Composable
fun PhoneDiagram(
    highlightKey: Boolean,
    modifier: Modifier = Modifier,
    bodyColor: Color = SurfaceVariantDark,
    keyColor: Color = NordBlue,
) {
    val glowAlpha by rememberInfiniteTransition(label = "plusKeyGlow").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.95f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp)
            .aspectRatio(0.52f)
            .semantics {
                contentDescription = "OnePlus Nord 5 outline with Plus Key highlighted"
            },
    ) {
        val stroke = Stroke(width = size.minDimension * 0.018f)
        val phoneWidth = size.width * 0.55f
        val phoneHeight = size.height * 0.92f
        val left = (size.width - phoneWidth) / 2f
        val top = (size.height - phoneHeight) / 2f
        val radius = phoneWidth * 0.12f

        drawRoundRect(
            color = bodyColor,
            topLeft = Offset(left, top),
            size = Size(phoneWidth, phoneHeight),
            cornerRadius = CornerRadius(radius, radius),
        )
        drawRoundRect(
            color = bodyColor.copy(alpha = 0.5f),
            topLeft = Offset(left, top),
            size = Size(phoneWidth, phoneHeight),
            cornerRadius = CornerRadius(radius, radius),
            style = stroke,
        )

        val inset = phoneWidth * 0.08f
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.55f),
            topLeft = Offset(left + inset, top + inset * 1.6f),
            size = Size(phoneWidth - inset * 2, phoneHeight - inset * 3.2f),
            cornerRadius = CornerRadius(radius * 0.6f, radius * 0.6f),
        )

        val keyHeight = phoneHeight * 0.12f
        val keyWidth = phoneWidth * 0.07f
        val keyTop = top + phoneHeight * 0.38f
        val keyLeft = left + phoneWidth - keyWidth * 0.35f

        if (highlightKey) {
            drawRoundRect(
                color = keyColor.copy(alpha = glowAlpha * 0.35f),
                topLeft = Offset(keyLeft - keyWidth * 0.6f, keyTop - keyHeight * 0.25f),
                size = Size(keyWidth * 2.2f, keyHeight * 1.5f),
                cornerRadius = CornerRadius(keyWidth, keyWidth),
            )
        }

        drawRoundRect(
            color = if (highlightKey) keyColor else bodyColor.copy(alpha = 0.9f),
            topLeft = Offset(keyLeft, keyTop),
            size = Size(keyWidth, keyHeight),
            cornerRadius = CornerRadius(keyWidth / 2f, keyWidth / 2f),
        )
    }
}
