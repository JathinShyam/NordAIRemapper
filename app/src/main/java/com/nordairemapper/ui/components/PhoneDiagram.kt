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
import androidx.compose.ui.graphics.Brush
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
        initialValue = 0.30f,
        targetValue = 0.90f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 56.dp)
            .aspectRatio(0.52f)
            .semantics {
                contentDescription = "OnePlus Nord 5 outline with Plus Key highlighted"
            },
    ) {
        val stroke = Stroke(width = size.minDimension * 0.016f)
        val phoneWidth = size.width * 0.58f
        val phoneHeight = size.height * 0.94f
        val left = (size.width - phoneWidth) / 2f
        val top = (size.height - phoneHeight) / 2f
        val radius = phoneWidth * 0.14f

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    bodyColor.copy(alpha = 0.95f),
                    bodyColor.copy(alpha = 0.75f),
                ),
            ),
            topLeft = Offset(left, top),
            size = Size(phoneWidth, phoneHeight),
            cornerRadius = CornerRadius(radius, radius),
        )
        drawRoundRect(
            color = keyColor.copy(alpha = if (highlightKey) 0.35f else 0.12f),
            topLeft = Offset(left, top),
            size = Size(phoneWidth, phoneHeight),
            cornerRadius = CornerRadius(radius, radius),
            style = stroke,
        )

        val inset = phoneWidth * 0.08f
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.62f),
            topLeft = Offset(left + inset, top + inset * 1.5f),
            size = Size(phoneWidth - inset * 2, phoneHeight - inset * 3.0f),
            cornerRadius = CornerRadius(radius * 0.55f, radius * 0.55f),
        )

        val keyHeight = phoneHeight * 0.11f
        val keyWidth = phoneWidth * 0.065f
        val keyTop = top + phoneHeight * 0.36f
        val keyLeft = left + phoneWidth - keyWidth * 0.30f

        if (highlightKey) {
            drawRoundRect(
                color = keyColor.copy(alpha = glowAlpha * 0.40f),
                topLeft = Offset(keyLeft - keyWidth * 0.7f, keyTop - keyHeight * 0.3f),
                size = Size(keyWidth * 2.4f, keyHeight * 1.6f),
                cornerRadius = CornerRadius(keyWidth, keyWidth),
            )
        }

        drawRoundRect(
            color = if (highlightKey) keyColor else bodyColor.copy(alpha = 0.95f),
            topLeft = Offset(keyLeft, keyTop),
            size = Size(keyWidth, keyHeight),
            cornerRadius = CornerRadius(keyWidth / 2f, keyWidth / 2f),
        )
    }
}
