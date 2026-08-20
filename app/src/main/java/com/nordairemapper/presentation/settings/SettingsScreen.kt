package com.nordairemapper.presentation.settings

import android.content.Intent
import android.content.pm.PackageManager
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
                title = { NordHeading("Settings", style = MaterialTheme.typography.titleLarge) },
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
            SectionLabel("Appearance")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ThemeMode.entries.forEach { mode ->
                    FilterChip(
                        selected = settings.themeMode == mode,
                        onClick = { viewModel.setThemeMode(mode) },
                        label = { Text(mode.name.lowercase().replaceFirstChar { it.titlecase() }) },
                    )
                }
            }
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

            SectionLabel("Feedback & overlay")
            SettingsLinkRow(
                title = "Preferences",
                subtitle = "How you’re notified when an action triggers",
                onClick = onOpenPreferences,
            )
            SettingsLinkRow(
                title = "Feedback",
                subtitle = "Haptic feedback & vibration intensity",
                onClick = onOpenFeedback,
            )
            SettingsLinkRow(
                title = "Visual Overlay",
                subtitle = "Action popup style when a remap fires",
                onClick = onOpenVisualOverlay,
            )
            SettingsLinkRow(
                title = "Overlay settings",
                subtitle = "Floating menu slots & layout",
                onClick = onOpenOverlay,
            )

            SectionLabel("Behavior")
            SettingsLinkRow(
                title = "Lock Screen",
                subtitle = "Gestures while the screen is locked",
                onClick = onOpenLockScreen,
            )
            SettingsLinkRow(
                title = "Backup & Restore",
                subtitle = "Export and import remaps",
                onClick = onOpenBackup,
            )

            SectionLabel("Per-app exclusions")
            Text(
                text = "Remapping is disabled while these apps are in the foreground.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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

            SectionLabel("Advanced")
            SettingsLinkRow(
                title = "Key setup",
                subtitle = "Learn / verify Plus Key",
                onClick = onOpenKeyLearning,
            )
            SettingsLinkRow(
                title = "Lab",
                subtitle = "Strategy, timing, USB unlock",
                onClick = onOpenDeveloper,
            )
            TextButton(
                onClick = {
                    viewModel.resetOnboarding()
                    onRestartOnboarding()
                },
            ) { Text("Restart onboarding") }

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
