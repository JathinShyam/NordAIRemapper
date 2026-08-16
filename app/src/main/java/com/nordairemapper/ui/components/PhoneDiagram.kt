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

/**
 * Front silhouette of the OnePlus Nord 5 (163.4 × 77 × 8.1 mm).
 *
 * Hardware layout (reviews / unboxing):
 * - Left: Plus Key only (replaces the Alert Slider)
 * - Right: volume rocker above, power / Gemini key below
 * - Centered punch-hole selfie camera, ~1.65 mm bezels, slightly rounded frame
 */
@Composable
fun PhoneDiagram(
    highlightKey: Boolean,
    modifier: Modifier = Modifier,
    bodyColor: Color = SurfaceVariantDark,
    keyColor: Color = NordBlue,
) {
    val glowAlpha by rememberInfiniteTransition(label = "plusKeyGlow").animateFloat(
        initialValue = 0.28f,
        targetValue = 0.78f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            // Room for side keys only — keep the body large on Home.
            .padding(horizontal = 12.dp)
            .aspectRatio(0.47f)
            .semantics {
                contentDescription = "OnePlus Nord 5 outline with Plus Key on the left"
            },
    ) {
        val stroke = Stroke(width = size.minDimension * 0.011f)
        val bodyAspect = 77f / 163.4f
        val maxW = size.width * 0.92f
        val maxH = size.height * 0.98f
        val phoneWidth = minOf(maxW, maxH * bodyAspect)
        val phoneHeight = phoneWidth / bodyAspect
        val left = (size.width - phoneWidth) / 2f
        val top = (size.height - phoneHeight) / 2f
        val radius = phoneWidth * 0.13f

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    bodyColor.copy(alpha = 0.98f),
                    bodyColor.copy(alpha = 0.78f),
                ),
            ),
            topLeft = Offset(left, top),
            size = Size(phoneWidth, phoneHeight),
            cornerRadius = CornerRadius(radius, radius),
        )
        drawRoundRect(
            color = Color.White.copy(alpha = 0.08f),
            topLeft = Offset(left, top),
            size = Size(phoneWidth, phoneHeight),
            cornerRadius = CornerRadius(radius, radius),
            style = stroke,
        )

        val bezel = phoneWidth * 0.035f
        val screenTop = top + bezel * 1.15f
        val screenLeft = left + bezel
        val screenWidth = phoneWidth - bezel * 2f
        val screenHeight = phoneHeight - bezel * 2.35f
        drawRoundRect(
            color = Color.Black.copy(alpha = 0.72f),
            topLeft = Offset(screenLeft, screenTop),
            size = Size(screenWidth, screenHeight),
            cornerRadius = CornerRadius(radius * 0.72f, radius * 0.72f),
        )

        val holeRadius = phoneWidth * 0.026f
        val holeCenter = Offset(
            x = left + phoneWidth / 2f,
            y = screenTop + holeRadius * 2.4f,
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.92f),
            radius = holeRadius,
            center = holeCenter,
        )
        drawCircle(
            color = Color(0xFF1A1A1A),
            radius = holeRadius * 0.52f,
            center = holeCenter,
        )

        val btnWidth = phoneWidth * 0.042f
        val rightEdge = left + phoneWidth

        val plusHeight = phoneHeight * 0.055f
        val plusTop = top + phoneHeight * 0.20f
        val plusLeft = left - btnWidth * 0.55f
        if (highlightKey) {
            drawRoundRect(
                color = keyColor.copy(alpha = glowAlpha * 0.30f),
                topLeft = Offset(plusLeft - btnWidth * 0.65f, plusTop - plusHeight * 0.25f),
                size = Size(btnWidth * 2.3f, plusHeight * 1.5f),
                cornerRadius = CornerRadius(btnWidth, btnWidth),
            )
        }
        drawRoundRect(
            color = if (highlightKey) keyColor else bodyColor.copy(alpha = 0.95f),
            topLeft = Offset(plusLeft, plusTop),
            size = Size(btnWidth, plusHeight),
            cornerRadius = CornerRadius(btnWidth / 2f, btnWidth / 2f),
        )

        val volHeight = phoneHeight * 0.115f
        val volTop = top + phoneHeight * 0.22f
        val rightBtnLeft = rightEdge - btnWidth * 0.45f
        drawRoundRect(
            color = bodyColor.copy(alpha = 0.9f),
            topLeft = Offset(rightBtnLeft, volTop),
            size = Size(btnWidth * 0.9f, volHeight),
            cornerRadius = CornerRadius(btnWidth / 2f, btnWidth / 2f),
        )

        val powerHeight = phoneHeight * 0.065f
        val powerTop = volTop + volHeight + phoneHeight * 0.018f
        drawRoundRect(
            color = bodyColor.copy(alpha = 0.9f),
            topLeft = Offset(rightBtnLeft, powerTop),
            size = Size(btnWidth * 0.9f, powerHeight),
            cornerRadius = CornerRadius(btnWidth / 2f, btnWidth / 2f),
        )
    }
}
