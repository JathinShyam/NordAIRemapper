package com.nordairemapper.presentation.settings

import android.content.pm.ApplicationInfo
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.BatteryAlert
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.presentation.common.adaptiveAccent
import com.nordairemapper.ui.components.NordTopBarTitle
import com.nordairemapper.ui.components.SectionLabel
import com.nordairemapper.ui.theme.NordBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenDeveloper: () -> Unit,
    onOpenKeyLearning: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenPreferences: () -> Unit,
    onOpenVisualOverlay: () -> Unit,
    onOpenLockScreen: () -> Unit,
    onOpenExclusions: () -> Unit,
    onRestartOnboarding: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val batteryExempt by viewModel.batteryExempt.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshBattery()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val green = adaptiveAccent(
        darkContainer = Color(0xFF14321F),
        darkTint = Color(0xFF3DDC84),
        lightContainer = Color(0xFFDCF3E4),
        lightTint = Color(0xFF1C7C46),
    )
    val purple = adaptiveAccent(
        darkContainer = Color(0xFF2A1F3D),
        darkTint = Color(0xFFB388FF),
        lightContainer = Color(0xFFEAE2FA),
        lightTint = Color(0xFF6A46B8),
    )
    val amber = adaptiveAccent(
        darkContainer = Color(0xFF3D2E14),
        darkTint = Color(0xFFFFB020),
        lightContainer = Color(0xFFFFF0D6),
        lightTint = Color(0xFF9A5F00),
    )
    val muted = adaptiveAccent(
        darkContainer = Color(0xFF222222),
        darkTint = Color(0xFFB0B0B0),
        lightContainer = Color(0xFFE8E8E8),
        lightTint = Color(0xFF5A5A5A),
    )
    val red = adaptiveAccent(
        darkContainer = Color(0xFF3A1818),
        darkTint = Color(0xFFFF6B6B),
        lightContainer = Color(0xFFFBE3E0),
        lightTint = Color(0xFFB3261E),
    )

    val buildLabel = if (
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    ) {
        "Debug"
    } else {
        "Release"
    }
    val exclusionCount = settings.excludedApps.size

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    NordTopBarTitle(
                        title = "Settings",
                        subtitle = "Appearance, Reliability & Tools",
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
            SectionLabel("Appearance")
            SettingsHubGroup {
                ThemeSegmentBlock(
                    selectedMode = settings.themeMode,
                    onSelect = viewModel::setThemeMode,
                )
                SettingsHubDivider()
                SettingsHubToggleRow(
                    title = "Dynamic Color",
                    subtitle = "Material You From Wallpaper",
                    checked = settings.dynamicColor,
                    onCheckedChange = viewModel::setDynamicColor,
                )
                SettingsHubDivider()
                SettingsHubToggleRow(
                    title = "Service Notification",
                    subtitle = "Ongoing Status While Remapping",
                    checked = settings.showServiceNotification,
                    onCheckedChange = viewModel::setShowServiceNotification,
                )
            }

            SectionLabel("Shortcuts")
            SettingsHubGroup {
                SettingsHubRow(
                    icon = Icons.Outlined.Vibration,
                    title = "Feedback",
                    subtitle = "Haptics When A Remap Fires",
                    accentContainer = green.container,
                    accentTint = green.tint,
                    onClick = onOpenFeedback,
                )
                SettingsHubDivider()
                SettingsHubRow(
                    icon = Icons.Outlined.Tune,
                    title = "Preferences",
                    subtitle = "Toast, Sound, Confirmation",
                    accentContainer = MaterialTheme.colorScheme.primaryContainer,
                    accentTint = NordBlue,
                    onClick = onOpenPreferences,
                )
                SettingsHubDivider()
                SettingsHubRow(
                    icon = Icons.Outlined.Visibility,
                    title = "Visual Overlay",
                    subtitle = "Popup Style On Remap",
                    accentContainer = purple.container,
                    accentTint = purple.tint,
                    onClick = onOpenVisualOverlay,
                )
                SettingsHubDivider()
                SettingsHubRow(
                    icon = Icons.Outlined.Widgets,
                    title = "Overlay Settings",
                    subtitle = "Floating Menu Slots & Layout",
                    accentContainer = purple.container,
                    accentTint = purple.tint,
                    onClick = onOpenOverlay,
                )
            }

            SectionLabel("Tools")
            SettingsHubGroup {
                SettingsHubRow(
                    icon = Icons.Outlined.Lock,
                    title = "Lock Screen",
                    subtitle = "Gestures While Locked",
                    accentContainer = amber.container,
                    accentTint = amber.tint,
                    onClick = onOpenLockScreen,
                )
                SettingsHubDivider()
                SettingsHubRow(
                    icon = Icons.Outlined.Backup,
                    title = "Backup & Restore",
                    subtitle = "Export And Import Remaps",
                    accentContainer = MaterialTheme.colorScheme.primaryContainer,
                    accentTint = NordBlue,
                    onClick = onOpenBackup,
                )
                SettingsHubDivider()
                SettingsHubRow(
                    icon = Icons.Outlined.TouchApp,
                    title = "Key Setup",
                    subtitle = "Learn / Verify Plus Key",
                    accentContainer = MaterialTheme.colorScheme.primaryContainer,
                    accentTint = NordBlue,
                    onClick = onOpenKeyLearning,
                )
                SettingsHubDivider()
                SettingsHubRow(
                    icon = Icons.Outlined.Science,
                    title = "Lab",
                    subtitle = "Strategy, Timing, USB Unlock",
                    accentContainer = amber.container,
                    accentTint = amber.tint,
                    onClick = onOpenDeveloper,
                )
                SettingsHubDivider()
                SettingsHubRow(
                    icon = Icons.Outlined.RestartAlt,
                    title = "Restart Onboarding",
                    subtitle = "Walk Through Setup Again",
                    accentContainer = red.container,
                    accentTint = red.tint,
                    onClick = {
                        viewModel.resetOnboarding()
                        onRestartOnboarding()
                    },
                )
            }

            SectionLabel("Reliability")
            SettingsHubGroup {
                BatteryOptimizationBlock(
                    exempt = batteryExempt,
                    icon = if (batteryExempt) {
                        Icons.Outlined.BatteryChargingFull
                    } else {
                        Icons.Outlined.BatteryAlert
                    },
                    accentContainer = if (batteryExempt) green.container else amber.container,
                    accentTint = if (batteryExempt) green.tint else amber.tint,
                    onCta = viewModel::openBatterySettings,
                )
                SettingsHubDivider()
                SettingsHubRow(
                    icon = Icons.Outlined.Apps,
                    title = "Per-App Exclusions",
                    subtitle = "Pause Remapping In Selected Apps",
                    accentContainer = muted.container,
                    accentTint = muted.tint,
                    onClick = onOpenExclusions,
                    titleTrailing = {
                        SettingsStatusChip(
                            label = when (exclusionCount) {
                                0 -> "None"
                                1 -> "1 App"
                                else -> "$exclusionCount Apps"
                            },
                            tone = if (exclusionCount == 0) {
                                SettingsStatusTone.Muted
                            } else {
                                SettingsStatusTone.Ok
                            },
                        )
                    },
                )
            }

            SectionLabel("About")
            SettingsHubGroup {
                SettingsHubRow(
                    icon = Icons.Outlined.Info,
                    title = "Version",
                    accentContainer = muted.container,
                    accentTint = muted.tint,
                    onClick = null,
                    trailing = null,
                    subtitleContent = {
                        VersionMeta(
                            versionName = viewModel.versionName(),
                            buildLabel = buildLabel,
                        )
                    },
                )
                SettingsHubDivider()
                SettingsHubRow(
                    icon = Icons.Outlined.Code,
                    title = "GitHub",
                    subtitle = "Source Code & Issues",
                    accentContainer = MaterialTheme.colorScheme.primaryContainer,
                    accentTint = NordBlue,
                    onClick = viewModel::openGithub,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
