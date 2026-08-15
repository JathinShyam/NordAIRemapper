package com.nordairemapper.presentation.settings

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
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
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
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.domain.model.ThemeMode
import com.nordairemapper.presentation.remap.AppPickerSheet
import com.nordairemapper.presentation.remap.InstalledAppInfo
import com.nordairemapper.ui.components.SectionLabel
import android.content.Intent
import android.content.pm.PackageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenDeveloper: () -> Unit,
    onOpenKeyLearning: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenOverlay: () -> Unit,
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
                title = { Text("Settings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
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
            SettingsToggle(
                title = "Dynamic color",
                checked = settings.dynamicColor,
                onCheckedChange = viewModel::setDynamicColor,
            )

            SectionLabel("Behavior")
            SettingsToggle(
                title = "Service notification",
                checked = settings.showServiceNotification,
                onCheckedChange = viewModel::setShowServiceNotification,
            )
            SettingsToggle(
                title = "Haptic feedback",
                checked = settings.hapticFeedback,
                onCheckedChange = viewModel::setHapticFeedback,
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
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(pkg, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
                        IconButton(onClick = { viewModel.removeExclusion(pkg) }) {
                            Icon(Icons.Outlined.Delete, contentDescription = "Remove")
                        }
                    }
                }
            }
            OutlinedAddButton(onClick = {
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
            })

            SectionLabel("Power")
            Text(
                text = if (batteryExempt) "Battery optimization exempt" else "Not exempt — detection may be killed",
                style = MaterialTheme.typography.bodyMedium,
                color = if (batteryExempt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            if (!batteryExempt) {
                Button(onClick = viewModel::openBatterySettings, modifier = Modifier.fillMaxWidth()) {
                    Text("Exempt from battery optimization")
                }
            }

            SectionLabel("Advanced")
            LinkRow("Developer settings", onOpenDeveloper)
            LinkRow("Key setup", onOpenKeyLearning)
            LinkRow("Backup & Restore", onOpenBackup)
            LinkRow("Overlay settings", onOpenOverlay)
            TextButton(
                onClick = {
                    viewModel.resetOnboarding()
                    onRestartOnboarding()
                },
            ) { Text("Show onboarding again") }

            SectionLabel("About")
            Text("Version ${viewModel.versionName()}", style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = viewModel::openGithub) { Text("GitHub") }
            Spacer(modifier = Modifier.height(16.dp))
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
private fun SettingsToggle(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}

@Composable
private fun LinkRow(title: String, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.titleMedium,
            )
            Icon(
                Icons.Outlined.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun OutlinedAddButton(onClick: () -> Unit) {
    Button(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Text("Add excluded app")
    }
}
