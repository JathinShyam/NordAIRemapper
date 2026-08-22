package com.nordairemapper.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontSynthesis
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.nordairemapper.ui.theme.HeadingRed
import com.nordairemapper.ui.theme.NordHeadingFontFamily

/**
 * Product heading in Space Grotesk. First letter is OnePlus red (#EB0028).
 * Body / UI copy uses Inter via [MaterialTheme.typography].
 *
 * Uses [FontSynthesis.None] so Android never soft-fakes bold on the variable font.
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
    // Space Grotesk axis max is 700 — Bold is the real heavy cut (see Type.kt RCA).
    val weight = style.fontWeight ?: FontWeight.Bold
    val headingStyle = style.copy(
        fontFamily = NordHeadingFontFamily,
        fontWeight = weight,
        fontSynthesis = FontSynthesis.None,
    )
    Text(
        text = buildAnnotatedString {
            withStyle(
                SpanStyle(
                    color = HeadingRed,
                    fontFamily = NordHeadingFontFamily,
                    fontWeight = weight,
                    fontSize = style.fontSize,
                    letterSpacing = style.letterSpacing,
                    fontSynthesis = FontSynthesis.None,
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
                    fontSynthesis = FontSynthesis.None,
                ),
            ) {
                append(rest)
            }
        },
        modifier = modifier,
        style = headingStyle,
        maxLines = maxLines,
    )
}

/** Screen top bars — matches design `.topbar .h` (22sp Space Grotesk w700). */
@Composable
fun NordTopBarHeading(
    text: String,
    modifier: Modifier = Modifier,
) {
    val style = MaterialTheme.typography.titleLarge.copy(
        fontFamily = NordHeadingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSynthesis = FontSynthesis.None,
    )
    // TopAppBar provides its own LocalTextStyle (often softer). Override so weight sticks.
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
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
            fontSynthesis = FontSynthesis.None,
        ),
    )
}
