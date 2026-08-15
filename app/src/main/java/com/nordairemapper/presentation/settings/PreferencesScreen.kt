package com.nordairemapper.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.domain.model.OverlayVisualStyle
import com.nordairemapper.presentation.overlay.OverlaySettingsViewModel
import com.nordairemapper.ui.components.NordGhostButton
import com.nordairemapper.ui.components.NordHeading
import com.nordairemapper.ui.components.NordPrimaryButton
import com.nordairemapper.ui.theme.NordBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PreferencesScreen(
    onBack: () -> Unit,
    onFinish: () -> Unit,
    onOpenVisualOverlay: () -> Unit,
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    overlayViewModel: OverlaySettingsViewModel = hiltViewModel(),
) {
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val overlay by overlayViewModel.config.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            ScreenIntro(
                title = "Preferences",
                body = "Choose how you're notified when an action triggers. You can change these later in settings.",
                centered = true,
            )

            SettingsGroupCard {
                SettingsToggleRow(
                    title = "Haptic feedback",
                    subtitle = "Vibrate on button press",
                    checked = settings.hapticFeedback,
                    onCheckedChange = settingsViewModel::setHapticFeedback,
                    icon = Icons.Outlined.Vibration,
                    showDivider = true,
                )
                SettingsToggleRow(
                    title = "Visual overlay",
                    subtitle = "Show action popup on screen",
                    checked = settings.visualOverlayEnabled,
                    onCheckedChange = settingsViewModel::setVisualOverlayEnabled,
                    icon = Icons.Outlined.Visibility,
                )
            }

            QuietSectionLabel("Overlay style")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StylePreviewCard(
                    title = "OnePlus",
                    selected = overlay.visualStyle == OverlayVisualStyle.ONEPLUS,
                    accent = NordBlue,
                    darkPreview = true,
                    onClick = { overlayViewModel.setVisualStyle(OverlayVisualStyle.ONEPLUS) },
                    modifier = Modifier.weight(1f),
                )
                StylePreviewCard(
                    title = "Stock",
                    selected = overlay.visualStyle == OverlayVisualStyle.STOCK,
                    accent = Color(0xFF9E9E9E),
                    darkPreview = false,
                    onClick = { overlayViewModel.setVisualStyle(OverlayVisualStyle.STOCK) },
                    modifier = Modifier.weight(1f),
                )
            }

            NordGhostButton(
                text = "Customize visual overlay",
                onClick = onOpenVisualOverlay,
                modifier = Modifier.padding(top = 20.dp),
            )
            NordPrimaryButton(
                text = "Finish Setup",
                onClick = onFinish,
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp),
            )
        }
    }
}
