package com.nordairemapper.presentation.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.ui.theme.Destructive
import com.nordairemapper.ui.theme.NordBlue

data class CategoryAccent(val container: Color, val tint: Color)

@Composable
fun categoryAccent(category: RemapActionCategory): CategoryAccent = when (category) {
    RemapActionCategory.APPS -> CategoryAccent(
        container = Color(0xFF3D2E14),
        tint = Color(0xFFFFB020),
    )
    RemapActionCategory.MEDIA -> CategoryAccent(
        container = Color(0xFF14321F),
        tint = Color(0xFF3DDC84),
    )
    RemapActionCategory.SYSTEM -> CategoryAccent(
        container = MaterialTheme.colorScheme.primaryContainer,
        tint = NordBlue,
    )
    RemapActionCategory.OVERLAY -> CategoryAccent(
        container = Color(0xFF2A2A2A),
        tint = Color(0xFFB0B0B0),
    )
    RemapActionCategory.NONE -> CategoryAccent(
        container = Color(0xFF3A1818),
        tint = Destructive,
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
