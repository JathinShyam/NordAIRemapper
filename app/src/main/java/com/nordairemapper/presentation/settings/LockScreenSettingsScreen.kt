package com.nordairemapper.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.ui.components.NordHeading

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreenSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { NordHeading("Lock Screen", style = MaterialTheme.typography.titleLarge) },
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
            Text(
                text = "Choose which gestures work when locked.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 14.dp),
            )

            SettingsGroupCard {
                SettingsToggleRow(
                    title = "Single Press",
                    subtitle = if (settings.lockScreenSingleEnabled) {
                        "Enabled when locked"
                    } else {
                        "Disabled when locked"
                    },
                    checked = settings.lockScreenSingleEnabled,
                    onCheckedChange = viewModel::setLockScreenSingleEnabled,
                    showDivider = true,
                )
                SettingsToggleRow(
                    title = "Double Press",
                    subtitle = if (settings.lockScreenDoubleEnabled) {
                        "Enabled when locked"
                    } else {
                        "Disabled when locked"
                    },
                    checked = settings.lockScreenDoubleEnabled,
                    onCheckedChange = viewModel::setLockScreenDoubleEnabled,
                    showDivider = true,
                )
                SettingsToggleRow(
                    title = "Long Press",
                    subtitle = if (settings.lockScreenLongEnabled) {
                        "Enabled when locked"
                    } else {
                        "Disabled when locked"
                    },
                    checked = settings.lockScreenLongEnabled,
                    onCheckedChange = viewModel::setLockScreenLongEnabled,
                )
            }
        }
    }
}
