package com.nordairemapper.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.nordairemapper.R

/**
 * Type pairing (research pick for hardware / product UI):
 * - Headings: **Space Grotesk** — geometric, slightly technical, strong hierarchy
 * - Body / UI: **Inter** — high x-height, best-in-class screen readability
 *
 * Both are OFL (free to ship). OnePlus Sans stays optional/local-only and is not used.
 *
 * ## Heading weight RCA (why top bars looked thin)
 * `space_grotesk.ttf` is a **variable** font. OS/2 default / named instance sits near
 * Light (~300). Listing `Font(..., FontWeight.ExtraBold)` without
 * [FontVariation.Settings] does **not** move the `wght` axis — Compose keeps the
 * default light outlines (or soft faux-bold). Space Grotesk’s axis tops out at
 * **700** (no real 800), matching design `font-weight: 700`. Every heading weight
 * below must set `FontVariation.weight(...)` explicitly.
 */
@OptIn(ExperimentalTextApi::class)
private fun spaceGrotesk(weight: FontWeight, axis: Int) = Font(
    resId = R.font.space_grotesk,
    weight = weight,
    variationSettings = FontVariation.Settings(
        FontVariation.weight(axis),
    ),
)

@OptIn(ExperimentalTextApi::class)
val NordHeadingFontFamily = FontFamily(
    spaceGrotesk(FontWeight.Light, 300),
    spaceGrotesk(FontWeight.Normal, 400),
    spaceGrotesk(FontWeight.Medium, 500),
    spaceGrotesk(FontWeight.SemiBold, 600),
    // Axis max = 700. Map Bold / ExtraBold / Black → 700 so requests never faux-bold.
    spaceGrotesk(FontWeight.Bold, 700),
    spaceGrotesk(FontWeight.ExtraBold, 700),
    spaceGrotesk(FontWeight.Black, 700),
)

val NordBodyFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
)

/** @deprecated Prefer [NordBodyFontFamily] / [NordHeadingFontFamily]. */
val NordFontFamily: FontFamily = NordBodyFontFamily

val NordTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = NordHeadingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = NordHeadingFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.84).sp,  // matches design -0.03em at 28sp
    ),
    titleLarge = TextStyle(
        fontFamily = NordHeadingFontFamily,
        // Design `.topbar .h`: 22px / font-weight 700 (Space Grotesk axis max).
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 26.sp,
        letterSpacing = (-0.66).sp,  // -0.03em @ 22sp
    ),
    titleMedium = TextStyle(
        fontFamily = NordBodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    titleSmall = TextStyle(
        fontFamily = NordBodyFontFamily,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelLarge = TextStyle(
        fontFamily = NordBodyFontFamily,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    labelMedium = TextStyle(
        fontFamily = NordBodyFontFamily,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
    bodyLarge = TextStyle(
        fontFamily = NordBodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
    ),
    bodyMedium = TextStyle(
        fontFamily = NordBodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
    ),
    bodySmall = TextStyle(
        fontFamily = NordBodyFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
    ),
)
