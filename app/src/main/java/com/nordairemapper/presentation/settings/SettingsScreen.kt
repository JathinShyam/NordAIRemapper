package com.nordairemapper.presentation.settings

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Backup
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.domain.model.ThemeMode
import com.nordairemapper.presentation.remap.AppPickerSheet
import com.nordairemapper.presentation.remap.InstalledAppInfo
import com.nordairemapper.ui.components.NordHeading
import com.nordairemapper.ui.components.NordPrimaryButton
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
    onOpenPreferences: () -> Unit,
    onOpenVisualOverlay: () -> Unit,
    onOpenLockScreen: () -> Unit,
    onRestartOnboarding: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val batteryExempt by viewModel.batteryExempt.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showAppPicker by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshBattery()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        NordHeading("Settings", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Appearance, feedback & tools",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // ── Appearance ────────────────────────────────────────────────
            SectionLabel("Appearance")

            // Theme pref-card with segmented control
            ThemePrefCard(
                selectedMode = settings.themeMode,
                onSelect = viewModel::setThemeMode,
            )

            SettingsToggleCard(
                title = "Dynamic color",
                checked = settings.dynamicColor,
                onCheckedChange = viewModel::setDynamicColor,
                subtitle = "Follow system Material You",
            )

            SettingsToggleCard(
                title = "Service notification",
                checked = settings.showServiceNotification,
                onCheckedChange = viewModel::setShowServiceNotification,
                subtitle = "Ongoing status while remapping",
            )

            // ── Feedback & overlay ────────────────────────────────────────
            SectionLabel("Feedback & overlay")
            HubRow(
                icon = Icons.Outlined.Vibration,
                title = "Feedback",
                subtitle = "Haptic feedback & vibration intensity",
                onClick = onOpenFeedback,
            )
            HubRow(
                icon = Icons.Outlined.Tune,
                title = "Preferences",
                subtitle = "How you're notified when an action triggers",
                onClick = onOpenPreferences,
            )
            HubRow(
                icon = Icons.Outlined.Visibility,
                title = "Visual overlay",
                subtitle = "Action popup style when a remap fires",
                onClick = onOpenVisualOverlay,
            )
            HubRow(
                icon = Icons.Outlined.Widgets,
                title = "Overlay settings",
                subtitle = "Floating menu slots & layout",
                onClick = onOpenOverlay,
            )

            // ── Behavior ──────────────────────────────────────────────────
            SectionLabel("Behavior")
            HubRow(
                icon = Icons.Outlined.Lock,
                title = "Lock Screen",
                subtitle = "Gestures while the screen is locked",
                onClick = onOpenLockScreen,
            )
            HubRow(
                icon = Icons.Outlined.Backup,
                title = "Backup & Restore",
                subtitle = "Export and import remaps",
                onClick = onOpenBackup,
            )

            // ── Advanced ──────────────────────────────────────────────────
            SectionLabel("Advanced")
            HubRow(
                icon = Icons.Outlined.TouchApp,
                title = "Key setup",
                subtitle = "Learn / verify Plus Key",
                onClick = onOpenKeyLearning,
            )
            HubRow(
                icon = Icons.Outlined.Science,
                title = "Lab",
                subtitle = "Strategy, timing, USB unlock (Developer)",
                onClick = onOpenDeveloper,
            )
            HubRow(
                icon = Icons.Outlined.RestartAlt,
                title = "Restart onboarding",
                subtitle = "Walk through setup again",
                onClick = {
                    viewModel.resetOnboarding()
                    onRestartOnboarding()
                },
            )

            // ── Per-app exclusions ────────────────────────────────────────
            SectionLabel("Per-app exclusions")
            Text(
                text = "Remapping is disabled while these apps are in the foreground.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (settings.excludedApps.isEmpty()) {
                Text(
                    text = "No apps excluded — remapping is active in all apps",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }
            settings.excludedApps.forEach { pkg ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(pkg, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { viewModel.removeExclusion(pkg) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Remove")
                        }
                    }
                }
            }
            NordPrimaryButton(
                text = "Add excluded app",
                onClick = {
                    val pm = context.packageManager
                    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                    installedApps = pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
                        .mapNotNull { resolve ->
                            val info = resolve.activityInfo ?: return@mapNotNull null
                            InstalledAppInfo(
                                packageName = info.packageName,
                                label = resolve.loadLabel(pm)?.toString().orEmpty(),
                            )
                        }
                        .distinctBy { it.packageName }
                        .sortedBy { it.label.lowercase() }
                    showAppPicker = true
                },
            )

            // ── Power ─────────────────────────────────────────────────────
            SectionLabel("Power")
            Text(
                text = if (batteryExempt) "Battery optimization exempt" else "Not exempt — detection may be killed",
                style = MaterialTheme.typography.bodyMedium,
                color = if (batteryExempt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            if (!batteryExempt) {
                NordPrimaryButton(
                    text = "Exempt from battery optimization",
                    onClick = viewModel::openBatterySettings,
                )
            }

            // ── About ─────────────────────────────────────────────────────
            SectionLabel("About")
            Text("Version ${viewModel.versionName()}", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = viewModel::openGithub) { Text("GitHub") }
            Spacer(Modifier.height(16.dp))
        }
    }

    if (showAppPicker) {
        AppPickerSheet(
            apps = installedApps,
            onLoad = {},
            onSelect = {
                viewModel.addExclusion(it)
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false },
        )
    }
}

// ── HubRow ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HubRow(icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(11.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text("›", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.outline)
        }
    }
}

// ── ThemePrefCard ─────────────────────────────────────────────────────────────

@Composable
private fun ThemePrefCard(selectedMode: ThemeMode, onSelect: (ThemeMode) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Theme",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            )
            Text(
                text = "Prototype stays dark; chips mirror the app control.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ThemeMode.entries.forEach { mode ->
                    val selected = selectedMode == mode
                    val label = mode.name.lowercase().replaceFirstChar { it.titlecase() }
                    OutlinedButton(
                        onClick = { onSelect(mode) },
                        colors = ButtonDefaults.outlinedButtonColors(
                            containerColor = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surface,
                            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
                        border = BorderStroke(
                            width = 1.dp,
                            color = if (selected) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.outline,
                        ),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 14.dp,
                            vertical = 6.dp,
                        ),
                    ) {
                        Text(label, style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

// ── SettingsToggleCard ────────────────────────────────────────────────────────

@Composable
private fun SettingsToggleCard(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                if (subtitle != null) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
