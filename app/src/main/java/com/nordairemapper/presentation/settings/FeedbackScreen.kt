package com.nordairemapper.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.domain.model.HapticIntensity
import com.nordairemapper.ui.components.NordHeading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val intensities = HapticIntensity.entries

    Scaffold(
        topBar = {
            TopAppBar(
                title = { NordHeading("Feedback", style = MaterialTheme.typography.titleLarge) },
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
            SettingsGroupCard {
                SettingsToggleRow(
                    title = "Haptic feedback",
                    subtitle = "Vibrate when button action triggers",
                    checked = settings.hapticFeedback,
                    onCheckedChange = viewModel::setHapticFeedback,
                    icon = Icons.Outlined.Vibration,
                )
            }

            QuietSectionLabel("Vibration intensity")
            SettingsGroupCard {
                intensities.forEachIndexed { index, intensity ->
                    SettingsChoiceRow(
                        title = when (intensity) {
                            HapticIntensity.LIGHT -> "Light"
                            HapticIntensity.MEDIUM -> "Medium"
                            HapticIntensity.HEAVY -> "Heavy"
                        },
                        subtitle = when (intensity) {
                            HapticIntensity.LIGHT -> "Subtle tap"
                            HapticIntensity.MEDIUM -> "Balanced feedback"
                            HapticIntensity.HEAVY -> "Firm vibration"
                        },
                        selected = settings.hapticIntensity == intensity,
                        onClick = { viewModel.setHapticIntensity(intensity) },
                        showDivider = index < intensities.lastIndex,
                    )
                }
            }
        }
    }
}
