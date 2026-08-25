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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.domain.model.ThemeMode
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
    onOpenVisualOverlay: () -> Unit,
    onOpenLockScreen: () -> Unit,
    onOpenExclusions: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenPermissions: () -> Unit,
    onRestartOnboarding: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val permissionSummary by viewModel.permissionSummary.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshPermissionSummary()
            }
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
    val cyanContainer = MaterialTheme.colorScheme.primaryContainer
    val cyanTint = NordBlue

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
                        subtitle = "Shortcuts, Reliability, And Tools",
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
                    accentContainer = cyanContainer,
                    accentTint = cyanTint,
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
                        "Haptics Off"
                    },
                    accentContainer = green.container,
                    accentTint = green.tint,
                    onClick = onOpenFeedback,
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Outlined.Visibility,
                    title = "Visual Overlay",
                    subtitle = if (settings.visualOverlayEnabled) "On" else "Off",
                    accentContainer = purple.container,
                    accentTint = purple.tint,
                    onClick = onOpenVisualOverlay,
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Outlined.Widgets,
                    title = "Floating Menu",
                    subtitle = "Slots, Layout, And Position",
                    accentContainer = purple.container,
                    accentTint = purple.tint,
                    onClick = onOpenOverlay,
                )
            }

            SectionLabel("Tools")
            SettingsGroup {
                SettingsNavRow(
                    icon = Icons.Outlined.Lock,
                    title = "Lock Screen",
                    subtitle = "Gestures While Locked",
                    accentContainer = amber.container,
                    accentTint = amber.tint,
                    onClick = onOpenLockScreen,
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Outlined.Backup,
                    title = "Backup & Restore",
                    subtitle = "Export And Import Remaps",
                    accentContainer = cyanContainer,
                    accentTint = cyanTint,
                    onClick = onOpenBackup,
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Outlined.TouchApp,
                    title = "Key Setup",
                    subtitle = "Learn / Verify Plus Key",
                    accentContainer = cyanContainer,
                    accentTint = cyanTint,
                    onClick = onOpenKeyLearning,
                )
                SettingsDivider()
                SettingsNavRow(
                    icon = Icons.Outlined.Science,
                    title = "Lab",
                    subtitle = "Strategy, Timing, Unlock",
                    accentContainer = amber.container,
                    accentTint = amber.tint,
                    onClick = onOpenDeveloper,
                )
                SettingsDivider()
                SettingsNavRow(
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
            SettingsGroup {
                SettingsNavRow(
                    icon = Icons.Outlined.Shield,
                    title = "Permissions",
                    subtitle = permissionSummary.label,
                    accentContainer = cyanContainer,
                    accentTint = cyanTint,
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
                        1 -> "1 App"
                        else -> "$exclusionCount Apps"
                    },
                    accentContainer = muted.container,
                    accentTint = muted.tint,
                    onClick = onOpenExclusions,
                    status = {
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
            SettingsGroup {
                SettingsNavRow(
                    icon = Icons.Outlined.Info,
                    title = "Version",
                    showChevron = false,
                    accentContainer = muted.container,
                    accentTint = muted.tint,
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
                    subtitle = "Source Code And Issues",
                    accentContainer = cyanContainer,
                    accentTint = cyanTint,
                    onClick = viewModel::openGithub,
                )
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
