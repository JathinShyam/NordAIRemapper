package com.nordairemapper.presentation.settings

import android.view.HapticFeedbackConstants
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
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.ui.components.NordTopBarTitle
import com.nordairemapper.ui.components.SectionLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockScreenSettingsScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val view = LocalView.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    NordTopBarTitle(
                        title = "Lock Screen",
                        subtitle = "Gestures While Locked",
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
            Text(
                text = "Controls whether each press type works while the screen is locked. Lock Screen actions still need Accessibility connected.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 8.dp),
            )

            SectionLabel("Press types")
            SettingsGroup {
                SettingsToggleRow(
                    title = "Single Press",
                    subtitle = if (settings.lockScreenSingleEnabled) "Enabled When Locked" else "Disabled When Locked",
                    checked = settings.lockScreenSingleEnabled,
                    onCheckedChange = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        viewModel.setLockScreenSingleEnabled(it)
                    },
                )
                SettingsDivider()
                SettingsToggleRow(
                    title = "Double Press",
                    subtitle = if (settings.lockScreenDoubleEnabled) "Enabled When Locked" else "Disabled When Locked",
                    checked = settings.lockScreenDoubleEnabled,
                    onCheckedChange = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        viewModel.setLockScreenDoubleEnabled(it)
                    },
                )
                SettingsDivider()
                SettingsToggleRow(
                    title = "Long Press",
                    subtitle = if (settings.lockScreenLongEnabled) "Enabled When Locked" else "Disabled When Locked",
                    checked = settings.lockScreenLongEnabled,
                    onCheckedChange = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        viewModel.setLockScreenLongEnabled(it)
                    },
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
