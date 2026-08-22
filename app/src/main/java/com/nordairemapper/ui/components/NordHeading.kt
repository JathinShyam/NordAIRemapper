package com.nordairemapper.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import androidx.compose.material3.LocalTextStyle
import com.nordairemapper.ui.theme.HeadingRed
import com.nordairemapper.ui.theme.NordHeadingFontFamily

/**
 * Product heading in Space Grotesk. First letter is OnePlus red (#EB0028).
 * Body / UI copy uses Inter via [MaterialTheme.typography].
 */
@Composable
fun NordHeading(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineMedium,
    maxLines: Int = Int.MAX_VALUE,
) {
    if (text.isEmpty()) return
    val first = text.first().toString()
    val rest = text.drop(1)
    val restColor =
        if (style.color == Color.Unspecified) MaterialTheme.colorScheme.onSurface else style.color
    // Space Grotesk variable axis — prefer explicit weight from style (titleLarge = ExtraBold).
    val weight = style.fontWeight ?: FontWeight.ExtraBold
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = HeadingRed,
                    fontFamily = NordHeadingFontFamily,
                    fontWeight = weight,
                    fontSize = style.fontSize,
                    letterSpacing = style.letterSpacing,
                ),
            ) {
                append(first)
            }
            withStyle(
                SpanStyle(
                    color = restColor,
                    fontFamily = NordHeadingFontFamily,
                    fontWeight = weight,
                    fontSize = style.fontSize,
                    letterSpacing = style.letterSpacing,
                ),
            ) {
                append(rest)
            }
        },
        modifier = modifier,
        style = style.copy(fontFamily = NordHeadingFontFamily, fontWeight = weight),
        maxLines = maxLines,
    )
}

/** Screen top bars — matches design `.topbar .h` (22sp Space Grotesk ExtraBold). */
@Composable
fun NordTopBarHeading(
    text: String,
    modifier: Modifier = Modifier,
) {
    val style = MaterialTheme.typography.titleLarge.copy(
        fontFamily = NordHeadingFontFamily,
        fontWeight = FontWeight.ExtraBold,
    )
    CompositionLocalProvider(LocalTextStyle provides style) {
        NordHeading(text = text, modifier = modifier, style = style)
    }
}

@Composable
fun NordTopBarTitle(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        NordTopBarHeading(text = title)
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun NordTitle(
    text: String,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 22.sp,
) {
    NordHeading(
        text = text,
        modifier = modifier,
        style = MaterialTheme.typography.titleLarge.copy(
            fontSize = fontSize,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = (-0.3).sp,
        ),
    )
}
