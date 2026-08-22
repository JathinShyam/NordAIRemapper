package com.nordairemapper.presentation.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.ui.theme.Destructive
import com.nordairemapper.ui.theme.LightNavy
import com.nordairemapper.ui.theme.NordBlue

data class CategoryAccent(val container: Color, val tint: Color)

/** True when the active color scheme is the dark one. */
@Composable
fun isDarkTheme(): Boolean = MaterialTheme.colorScheme.background.luminance() < 0.5f

/**
 * Picks between a dark-first OxygenOS-style pair and its light-theme twin.
 * The old behavior shipped near-black containers into LIGHT mode, where they
 * rendered as dark blobs on light surfaces.
 */
@Composable
fun adaptiveAccent(
    darkContainer: Color,
    darkTint: Color,
    lightContainer: Color,
    lightTint: Color,
): CategoryAccent =
    if (isDarkTheme()) {
        CategoryAccent(darkContainer, darkTint)
    } else {
        CategoryAccent(lightContainer, lightTint)
    }

@Composable
fun categoryAccent(category: RemapActionCategory): CategoryAccent = when (category) {
    RemapActionCategory.APPS -> adaptiveAccent(
        darkContainer = Color(0xFF3D2E14),
        darkTint = Color(0xFFFFB020),
        lightContainer = Color(0xFFFFF0D6),
        lightTint = Color(0xFF9A5F00),
    )
    RemapActionCategory.MEDIA -> adaptiveAccent(
        darkContainer = Color(0xFF14321F),
        darkTint = Color(0xFF3DDC84),
        lightContainer = Color(0xFFDCF3E4),
        lightTint = Color(0xFF1C7C46),
    )
    RemapActionCategory.SYSTEM -> CategoryAccent(
        container = MaterialTheme.colorScheme.primaryContainer,
        tint = if (isDarkTheme()) NordBlue else LightNavy,
    )
    RemapActionCategory.OVERLAY -> adaptiveAccent(
        darkContainer = Color(0xFF2A2A2A),
        darkTint = Color(0xFFB0B0B0),
        lightContainer = Color(0xFFE6E6E6),
        lightTint = Color(0xFF4A4A4A),
    )
    RemapActionCategory.NONE -> adaptiveAccent(
        darkContainer = Color(0xFF3A1818),
        darkTint = Destructive,
        lightContainer = Color(0xFFFBE3E0),
        lightTint = Color(0xFFB3261E),
    )
}

fun categoryFor(action: RemapAction): RemapActionCategory =
    RemapActionCatalog.items.firstOrNull {
        when {
            action is RemapAction.LaunchApp -> it.action is RemapAction.LaunchApp
            action is RemapAction.OpenUrl -> it.action is RemapAction.OpenUrl
            else -> it.action.conflictKey() == action.conflictKey()
        }
    }?.category ?: when (action) {
        is RemapAction.None -> RemapActionCategory.NONE
        is RemapAction.ShowOverlay -> RemapActionCategory.OVERLAY
        is RemapAction.LaunchApp, is RemapAction.OpenUrl -> RemapActionCategory.APPS
        else -> RemapActionCategory.SYSTEM
    }
