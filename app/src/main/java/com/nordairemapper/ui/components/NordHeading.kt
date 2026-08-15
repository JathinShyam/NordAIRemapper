package com.nordairemapper.ui.components

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
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
    val weight = style.fontWeight ?: FontWeight.Bold
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
        style = style.copy(fontFamily = NordHeadingFontFamily),
        maxLines = maxLines,
    )
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
        style = MaterialTheme.typography.headlineMedium.copy(
            fontSize = fontSize,
            fontWeight = FontWeight.Bold,
            letterSpacing = (-0.3).sp,
        ),
    )
}
