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
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.TouchApp
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
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.domain.model.ThemeMode
import com.nordairemapper.ui.components.NordTopBarTitle
import com.nordairemapper.ui.components.SectionLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenDeveloper: () -> Unit,
    onOpenKeyLearning: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenOverlay: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenVisualOverlay: () -> Unit,
    onOpenLockScreen: () -> Unit,
    onOpenExclusions: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenPermissions: () -> Unit,
    onRestartOnboarding: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val batteryExempt by viewModel.batteryExempt.collectAsStateWithLifecycle()
    val permissionSummary by viewModel.permissionSummary.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshBattery()
                viewModel.refreshPermissionSummary()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val buildLabel = if (
        (context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
    ) {
        "Debug"
    } else {
        "Release"
    }
    val exclusionCount = settings.excludedApps.size
    val appearanceStatus = buildString {
        append(
            when (settings.themeMode) {
                ThemeMode.DARK -> "Dark"
                ThemeMode.LIGHT -> "Light"
                ThemeMode.SYSTEM -> "System"
            },
        )
        if (settings.oledBlack) append(" · OLED")
        if (settings.dynamicColor) append(" · Dynamic")
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    NordTopBarTitle(
                        title = "Settings",
                        subtitle = "Shortcuts, reliability, and tools",
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
            SettingsGroup {
                SettingsNavRow(
                    icon = Icons.Outlined.Palette,
                    title = "Appearance",
                    subtitle = appearanceStatus,
                    onClick = onOpenAppearance,
                )
            }

            SectionLabel("Shortcuts")
            SettingsGroup {
                SettingsNavRow(
                    icon = Icons.Outlined.Vibration,
                    title = "Feedback",
                    subtitle = if (settings.hapticFeedback) {
                        "Haptics · ${settings.hapticIntensity.name.lowercase().replaceFirstChar { it.uppercase() }}"
                    } else {
                        "Haptics off"
                    },
                    onClick = onOpenFeedback,
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Outlined.Visibility,
                    title = "Visual Overlay",
                    subtitle = if (settings.visualOverlayEnabled) "On" else "Off",
                    onClick = onOpenVisualOverlay,
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Outlined.Widgets,
                    title = "Floating Menu",
                    subtitle = "Slots, layout, and position",
                    onClick = onOpenOverlay,
                )
            }

            SectionLabel("Tools")
            SettingsGroup {
                SettingsNavRow(
                    icon = Icons.Outlined.Lock,
                    title = "Lock Screen",
                    subtitle = "Gestures while locked",
                    onClick = onOpenLockScreen,
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Outlined.Backup,
                    title = "Backup & Restore",
                    subtitle = "Export and import remaps",
                    onClick = onOpenBackup,
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Outlined.TouchApp,
                    title = "Key Setup",
                    subtitle = "Learn / verify Plus Key",
                    onClick = onOpenKeyLearning,
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Outlined.Science,
                    title = "Lab",
                    subtitle = "Strategy, timing, unlock",
                    onClick = onOpenDeveloper,
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Outlined.RestartAlt,
                    title = "Restart Onboarding",
                    subtitle = "Walk through setup again",
                    onClick = {
                        viewModel.resetOnboarding()
                        onRestartOnboarding()
                    },
                )
            }

            SectionLabel("Reliability")
            SettingsGroup {
                BatteryOptimizationBlock(
                    exempt = batteryExempt,
                    icon = if (batteryExempt) {
                        Icons.Outlined.BatteryChargingFull
                    } else {
                        Icons.Outlined.BatteryAlert
                    },
                    onCta = viewModel::openBatterySettings,
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Outlined.Shield,
                    title = "Permissions",
                    subtitle = permissionSummary.label,
                    onClick = onOpenPermissions,
                    status = {
                        SettingsStatusChip(
                            label = permissionSummary.label,
                            tone = if (permissionSummary.allOk) {
                                SettingsStatusTone.Ok
                            } else {
                                SettingsStatusTone.Warn
                            },
                        )
                    },
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Outlined.Apps,
                    title = "Per-App Exclusions",
                    subtitle = when (exclusionCount) {
                        0 -> "None"
                        1 -> "1 app"
                        else -> "$exclusionCount apps"
                    },
                    onClick = onOpenExclusions,
                    status = {
                        SettingsStatusChip(
                            label = when (exclusionCount) {
                                0 -> "None"
                                1 -> "1 app"
                                else -> "$exclusionCount apps"
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
            SettingsGroup {
                SettingsNavRow(
                    icon = Icons.Outlined.Info,
                    title = "Version",
                    showChevron = false,
                    subtitleContent = {
                        VersionMeta(
                            versionName = viewModel.versionName(),
                            buildLabel = buildLabel,
                        )
                    },
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Outlined.Code,
                    title = "GitHub",
                    subtitle = "Source code and issues",
                    onClick = viewModel::openGithub,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
