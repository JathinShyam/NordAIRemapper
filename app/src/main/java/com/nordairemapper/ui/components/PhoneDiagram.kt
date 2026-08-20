package com.nordairemapper.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
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
import com.nordairemapper.ui.theme.NordBlue
import com.nordairemapper.ui.theme.SurfaceVariantDark

/**
 * Front silhouette of the OnePlus Nord 5.
 *
 * Exact body ratio from GSMArena: **163.4 × 77 × 8.1 mm** (W/H = 77/163.4).
 * Screen-to-body ~90.1%; flat slightly-rounded frame; centered punch-hole.
 * Left: short Plus Key. Right: volume rocker above power.
 *
 * Draws the body as large as the canvas allows at that ratio (only a thin
 * gutter for side keys) so it does not look like a skinny toy.
 */
@Composable
fun PhoneDiagram(
    highlightKey: Boolean,
    modifier: Modifier = Modifier,
    /** Soft traveling glow along the left edge when remapping is live. */
    edgeRipple: Boolean = false,
    bodyColor: Color = SurfaceVariantDark,
    keyColor: Color = NordBlue,
) {
    val glowAlpha by rememberInfiniteTransition(label = "plusKeyGlow").animateFloat(
        initialValue = 0.32f,
        targetValue = 0.82f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1600),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "glowAlpha",
    )
    val rippleTravel by rememberInfiniteTransition(label = "edgeRipple").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 2200),
            repeatMode = RepeatMode.Restart,
        ),
        label = "rippleTravel",
    )

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "OnePlus Nord 5 outline with Plus Key on the left"
            },
    ) {
        // Body aspect = width / height (mm)
        val bodyAspect = 77f / 163.4f
        // Reserve ~2% of body width per side for keys (≈1.5 mm / 77 mm)
        val keyGutterFrac = 1.5f / 77f

        // Fit body to canvas at exact ratio — prefer filling height, then clamp width
        var phoneHeight = size.height * 0.98f
        var phoneWidth = phoneHeight * bodyAspect
        val maxBodyWidth = size.width / (1f + 2f * keyGutterFrac)
        if (phoneWidth > maxBodyWidth) {
            phoneWidth = maxBodyWidth
            phoneHeight = phoneWidth / bodyAspect
        }

        val keyGutter = phoneWidth * keyGutterFrac
        val left = (size.width - phoneWidth) / 2f
        val top = (size.height - phoneHeight) / 2f
        // ~9.2 mm corner on 77 mm width ≈ slightly-rounded OnePlus frame
        val radius = phoneWidth * (9.2f / 77f)
        val stroke = Stroke(width = phoneWidth * 0.0045f)

        drawRoundRect(
            brush = Brush.verticalGradient(
                colors = listOf(
                    bodyColor.copy(alpha = 1f),
                    bodyColor.copy(alpha = 0.88f),
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

        // Thin bezels (~90% STB)
        val bezelX = phoneWidth * (1.9f / 77f)
        val bezelTop = phoneHeight * (2.2f / 163.4f)
        val bezelBottom = phoneHeight * (2.6f / 163.4f)
        val screenLeft = left + bezelX
        val screenTop = top + bezelTop
        val screenWidth = phoneWidth - bezelX * 2f
        val screenHeight = phoneHeight - bezelTop - bezelBottom
        val screenRadius = radius * 0.82f
        drawRoundRect(
            color = if (highlightKey) {
                Color(0xFF0B1820).copy(alpha = 0.96f)
            } else {
                Color.Black.copy(alpha = 0.92f)
            },
            topLeft = Offset(screenLeft, screenTop),
            size = Size(screenWidth, screenHeight),
            cornerRadius = CornerRadius(screenRadius, screenRadius),
        )

        val earW = phoneWidth * (13f / 77f)
        val earH = phoneHeight * (1.15f / 163.4f)
        drawRoundRect(
            color = Color(0xFF121212),
            topLeft = Offset(
                left + phoneWidth / 2f - earW / 2f,
                screenTop + phoneHeight * (3f / 163.4f),
            ),
            size = Size(earW, earH),
            cornerRadius = CornerRadius(earH, earH),
        )

        val holeRadius = phoneWidth * (1.55f / 77f)
        val holeCenter = Offset(
            x = left + phoneWidth / 2f,
            y = screenTop + phoneHeight * (8.2f / 163.4f),
        )
        drawCircle(color = Color(0xFF030303), radius = holeRadius, center = holeCenter)
        drawCircle(color = Color(0xFF1A1A1A), radius = holeRadius * 0.48f, center = holeCenter)

        val btnWidth = keyGutter * 1.05f
        val sideBtnColor = bodyColor.copy(alpha = 0.95f)

        // Plus Key — 8.5 mm tall, starts ~31.5 mm from top
        val plusHeight = phoneHeight * (8.5f / 163.4f)
        val plusTop = top + phoneHeight * (31.5f / 163.4f)
        val plusLeft = left - btnWidth * 0.92f
        if (highlightKey) {
            drawRoundRect(
                color = keyColor.copy(alpha = glowAlpha * 0.28f),
                topLeft = Offset(plusLeft - btnWidth * 0.45f, plusTop - plusHeight * 0.2f),
                size = Size(btnWidth * 1.9f, plusHeight * 1.4f),
                cornerRadius = CornerRadius(btnWidth, btnWidth),
            )
        }
        drawRoundRect(
            color = if (highlightKey) keyColor else sideBtnColor,
            topLeft = Offset(plusLeft, plusTop),
            size = Size(btnWidth, plusHeight),
            cornerRadius = CornerRadius(btnWidth / 2f, btnWidth / 2f),
        )

        if (edgeRipple && highlightKey) {
            val rippleH = phoneHeight * 0.22f
            val rippleY = top + (phoneHeight - rippleH) * rippleTravel
            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color.Transparent,
                        keyColor.copy(alpha = 0.55f),
                        Color.Transparent,
                    ),
                    startY = rippleY,
                    endY = rippleY + rippleH,
                ),
                topLeft = Offset(left - keyGutter * 0.35f, rippleY),
                size = Size(keyGutter * 0.55f, rippleH),
                cornerRadius = CornerRadius(keyGutter, keyGutter),
            )
        }

        val rightBtnLeft = left + phoneWidth - btnWidth * 0.08f
        val volHeight = phoneHeight * (19.2f / 163.4f)
        val volTop = top + phoneHeight * (34.5f / 163.4f)
        drawRoundRect(
            color = sideBtnColor,
            topLeft = Offset(rightBtnLeft, volTop),
            size = Size(btnWidth, volHeight),
            cornerRadius = CornerRadius(btnWidth / 2f, btnWidth / 2f),
        )

        val powerHeight = phoneHeight * (10.2f / 163.4f)
        val powerTop = top + phoneHeight * (56.5f / 163.4f)
        drawRoundRect(
            color = sideBtnColor,
            topLeft = Offset(rightBtnLeft, powerTop),
            size = Size(btnWidth, powerHeight),
            cornerRadius = CornerRadius(btnWidth / 2f, btnWidth / 2f),
        )
    }
}
