package com.nordairemapper.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nordairemapper.ui.theme.HeadingRed

/**
 * Front silhouette of the OnePlus Nord 5.
 *
 * Exact body ratio from GSMArena: **163.4 × 77 × 8.1 mm** (W/H = 77/163.4).
 * Theme-aware chrome (body / frame / Plus Key) for light and dark;
 * screen glass stays near-black so NEVER / SETTLE stays readable.
 */
@Composable
fun PhoneDiagram(
    highlightKey: Boolean,
    modifier: Modifier = Modifier,
    /** Soft traveling glow along the left edge when remapping is live. */
    edgeRipple: Boolean = false,
) {
    val scheme = MaterialTheme.colorScheme
    val isDark = scheme.background.luminance() < 0.5f
    val keyColor = scheme.primary
    // Product chrome — graphite that contrasts with the page background
    val bodyColor = if (isDark) {
        Color(0xFF2A2A2A)
    } else {
        Color(0xFF3D3D3D)
    }
    val frameStroke = if (isDark) {
        Color.White.copy(alpha = 0.14f)
    } else {
        Color.Black.copy(alpha = 0.22f)
    }
    val screenOff = Color(0xFF050505)
    val screenOn = if (isDark) {
        Color(0xFF0B1820)
    } else {
        Color(0xFF0A1018)
    }
    val earColor = if (isDark) Color(0xFF121212) else Color(0xFF0E0E0E)
    val holeOuter = Color(0xFF030303)
    val holeInner = if (isDark) Color(0xFF1A1A1A) else Color(0xFF222222)
    val sideBtnIdle = if (isDark) {
        bodyColor.copy(alpha = 0.95f)
    } else {
        Color(0xFF525252)
    }
    val softShadow = if (isDark) {
        Color.Black.copy(alpha = 0.35f)
    } else {
        Color.Black.copy(alpha = 0.18f)
    }

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

    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                contentDescription = "OnePlus Nord 5 outline with Plus Key on the left"
            },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bodyAspect = 77f / 163.4f
            val keyGutterFrac = 1.5f / 77f

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
            val radius = phoneWidth * (9.2f / 77f)
            val stroke = Stroke(width = phoneWidth * 0.0045f)

            // Soft contact shadow so the phone lifts off light backgrounds
            drawRoundRect(
                color = softShadow,
                topLeft = Offset(left + phoneWidth * 0.02f, top + phoneHeight * 0.015f),
                size = Size(phoneWidth, phoneHeight),
                cornerRadius = CornerRadius(radius, radius),
            )

            drawRoundRect(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        bodyColor.copy(alpha = 1f),
                        bodyColor.copy(alpha = 0.92f),
                        bodyColor.copy(alpha = 0.82f),
                    ),
                ),
                topLeft = Offset(left, top),
                size = Size(phoneWidth, phoneHeight),
                cornerRadius = CornerRadius(radius, radius),
            )
            drawRoundRect(
                color = frameStroke,
                topLeft = Offset(left, top),
                size = Size(phoneWidth, phoneHeight),
                cornerRadius = CornerRadius(radius, radius),
                style = stroke,
            )

            val bezelX = phoneWidth * (1.9f / 77f)
            val bezelTop = phoneHeight * (2.2f / 163.4f)
            val bezelBottom = phoneHeight * (2.6f / 163.4f)
            val screenLeft = left + bezelX
            val screenTop = top + bezelTop
            val screenWidth = phoneWidth - bezelX * 2f
            val screenHeight = phoneHeight - bezelTop - bezelBottom
            val screenRadius = radius * 0.82f
            drawRoundRect(
                color = if (highlightKey) screenOn else screenOff,
                topLeft = Offset(screenLeft, screenTop),
                size = Size(screenWidth, screenHeight),
                cornerRadius = CornerRadius(screenRadius, screenRadius),
            )

            val earW = phoneWidth * (13f / 77f)
            val earH = phoneHeight * (1.15f / 163.4f)
            drawRoundRect(
                color = earColor,
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
            drawCircle(color = holeOuter, radius = holeRadius, center = holeCenter)
            drawCircle(color = holeInner, radius = holeRadius * 0.48f, center = holeCenter)

            val btnWidth = keyGutter * 1.05f

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
                color = if (highlightKey) keyColor else sideBtnIdle,
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
                color = sideBtnIdle,
                topLeft = Offset(rightBtnLeft, volTop),
                size = Size(btnWidth, volHeight),
                cornerRadius = CornerRadius(btnWidth / 2f, btnWidth / 2f),
            )

            val powerHeight = phoneHeight * (10.2f / 163.4f)
            val powerTop = top + phoneHeight * (56.5f / 163.4f)
            drawRoundRect(
                color = sideBtnIdle,
                topLeft = Offset(rightBtnLeft, powerTop),
                size = Size(btnWidth, powerHeight),
                cornerRadius = CornerRadius(btnWidth / 2f, btnWidth / 2f),
            )
        }

        NeverSettleBrand(
            modifier = Modifier
                .align(Alignment.Center)
                .offset(y = 4.dp),
        )
    }
}

/** Compact recreation of the OnePlus logo + stacked NEVER / SETTLE bars. */
@Composable
private fun NeverSettleBrand(modifier: Modifier = Modifier) {
    val red = HeadingRed
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        OnePlusMark(color = red, modifier = Modifier.size(28.dp))
        Spacer(modifier = Modifier.height(10.dp))
        MottoBar(text = "NEVER", red = red)
        Spacer(modifier = Modifier.height(3.dp))
        MottoBar(text = "SETTLE", red = red)
    }
}

@Composable
private fun MottoBar(text: String, red: Color) {
    Box(
        modifier = Modifier
            .width(72.dp)
            .height(16.dp)
            .background(red, RoundedCornerShape(1.dp)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            color = Color.White,
            fontSize = 8.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.6.sp,
            textAlign = TextAlign.Center,
        )
    }
}

/** Red square frame with “1” and a plus on the top-right corner. */
@Composable
private fun OnePlusMark(color: Color, modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val stroke = size.minDimension * 0.11f
            val square = size.minDimension * 0.78f
            val left = (size.width - square) / 2f - size.minDimension * 0.04f
            val top = (size.height - square) / 2f + size.minDimension * 0.06f

            drawRoundRect(
                color = color,
                topLeft = Offset(left, top),
                size = Size(square, square),
                cornerRadius = CornerRadius(stroke * 0.35f),
                style = Stroke(width = stroke),
            )

            val plusCenter = Offset(
                x = left + square + stroke * 0.15f,
                y = top - stroke * 0.15f,
            )
            val arm = size.minDimension * 0.16f
            val thick = stroke * 0.95f
            drawRect(
                color = color,
                topLeft = Offset(plusCenter.x - arm, plusCenter.y - thick / 2f),
                size = Size(arm * 2f, thick),
            )
            drawRect(
                color = color,
                topLeft = Offset(plusCenter.x - thick / 2f, plusCenter.y - arm),
                size = Size(thick, arm * 2f),
            )
        }
        Text(
            text = "1",
            color = color,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.offset(x = (-1.5).dp, y = 1.5.dp),
            textAlign = TextAlign.Center,
        )
    }
}
