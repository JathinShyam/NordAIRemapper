package com.nordairemapper.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.ui.components.NordTopBarTitle
import com.nordairemapper.ui.components.SectionLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    NordTopBarTitle(
                        title = "Appearance",
                        subtitle = "Theme & Notifications",
                    )
                },
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            SectionLabel("Theme")
            SettingsHubGroup {
                ThemeSegmentBlock(
                    selectedMode = settings.themeMode,
                    onSelect = viewModel::setThemeMode,
                )
            }

            SectionLabel("Options")
            SettingsHubGroup {
                SettingsHubToggleRow(
                    title = "Dynamic Color",
                    subtitle = "Material You From Wallpaper",
                    checked = settings.dynamicColor,
                    onCheckedChange = viewModel::setDynamicColor,
                )
                SettingsHubDivider()
                SettingsHubToggleRow(
                    title = "OLED Black",
                    subtitle = "Use Pure Black Background In Dark Mode",
                    checked = settings.oledBlack,
                    onCheckedChange = viewModel::setOledBlack,
                )
                SettingsHubDivider()
                SettingsHubToggleRow(
                    title = "Service Notification",
                    subtitle = "Ongoing Status While Remapping",
                    checked = settings.showServiceNotification,
                    onCheckedChange = viewModel::setShowServiceNotification,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
