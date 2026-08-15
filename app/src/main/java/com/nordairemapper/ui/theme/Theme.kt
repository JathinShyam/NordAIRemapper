package com.nordairemapper.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme = darkColorScheme(
    primary = NordBlue,
    onPrimary = Color.Black,
    primaryContainer = NordBlueMuted,
    onPrimaryContainer = NordBlue,
    secondary = NordBlueDim,
    onSecondary = Color.Black,
    tertiary = StatusActive,
    onTertiary = Color.Black,
    background = BackgroundDark,
    onBackground = TextPrimaryDark,
    surface = SurfaceDark,
    onSurface = TextPrimaryDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = TextSecondary,
    surfaceContainerHighest = SurfaceElevatedDark,
    error = Destructive,
    onError = Color.Black,
    outline = OutlineDark,
    outlineVariant = SurfaceVariantDark,
)

private val LightColorScheme = lightColorScheme(
    primary = LightNavy,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFDDE8FF),
    onPrimaryContainer = LightNavy,
    secondary = NordBlue,
    onSecondary = Color.Black,
    tertiary = StatusActive,
    onTertiary = Color.Black,
    background = BackgroundLight,
    onBackground = TextPrimaryLight,
    surface = SurfaceLight,
    onSurface = TextPrimaryLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = Color(0xFF777777),
    error = Destructive,
    onError = Color.White,
    outline = Color(0xFFE0E0E0),
)

@Composable
fun NordAIRemapperTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = NordTypography,
        shapes = NordShapes,
        content = content,
    )
}
