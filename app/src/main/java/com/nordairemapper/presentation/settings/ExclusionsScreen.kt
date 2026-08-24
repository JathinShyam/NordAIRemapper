package com.nordairemapper.presentation.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.presentation.remap.AppPickerSheet
import com.nordairemapper.presentation.remap.InstalledAppInfo
import com.nordairemapper.presentation.remap.queryLaunchableApps
import com.nordairemapper.service.ElevatedPermissions
import com.nordairemapper.ui.components.NordPrimaryButton
import com.nordairemapper.ui.components.NordTopBarTitle
import com.nordairemapper.ui.components.StatusChip
import com.nordairemapper.ui.components.StatusTone
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExclusionsScreen(
    onBack: () -> Unit,
    onOpenEnableDetection: () -> Unit = {},
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showAppPicker by remember { mutableStateOf(false) }
    var installedApps by remember { mutableStateOf<List<InstalledAppInfo>>(emptyList()) }
    var loadingApps by remember { mutableStateOf(false) }
    var loadFailed by remember { mutableStateOf(false) }
    var handsFreeReady by remember {
        mutableStateOf(ElevatedPermissions.canAutoResumeAccessibility(context))
    }
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                handsFreeReady = ElevatedPermissions.canAutoResumeAccessibility(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun openAppPicker() {
        scope.launch {
            if (installedApps.isEmpty()) {
                loadingApps = true
                loadFailed = false
                // PackageManager queries are binder calls — keep them off main.
                val result = withContext(Dispatchers.IO) {
                    runCatching { queryLaunchableApps(context) }
                }
                installedApps = result.getOrDefault(emptyList())
                loadFailed = result.isFailure
                loadingApps = false
            }
            showAppPicker = true
        }
    }

    val pm = context.packageManager
    // Label lookup is a binder call per package; resolve off the main thread.
    var exclusionRows by remember { mutableStateOf<List<Pair<String, String>>>(emptyList()) }
    LaunchedEffect(settings.excludedApps) {
        val packages = settings.excludedApps
        exclusionRows = withContext(Dispatchers.IO) {
            packages
                .map { pkg ->
                    val label = runCatching {
                        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
                    }.getOrDefault(pkg)
                    pkg to label
                }
                .sortedBy { it.second.lowercase() }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    NordTopBarTitle(
                        title = "Per-App Exclusions",
                        subtitle = "Pause Remapping In Selected Apps",
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
        // Key emptiness off the setting itself, not the async-resolved rows,
        // so the list never flashes the "no exclusions" panel mid-load.
        if (settings.excludedApps.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                AutoPauseAccessibilityCard(
                    enabled = settings.pauseAccessibilityInExcludedApps,
                    handsFreeReady = handsFreeReady,
                    onEnabledChange = viewModel::setPauseAccessibilityInExcludedApps,
                    onOpenEnableDetection = onOpenEnableDetection,
                )
                Spacer(Modifier.height(12.dp))
                SettingsHubGroup {
                    ExclusionsEmptyPanel(
                        icon = Icons.Outlined.Apps,
                        onAdd = ::openAppPicker,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Add BHIM (or your UPI app), enable Auto-Pause, and complete Wireless Unlock once. After that, payments are hands-free: Accessibility off while the app is open, back on when you leave.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                item {
                    AutoPauseAccessibilityCard(
                        enabled = settings.pauseAccessibilityInExcludedApps,
                        handsFreeReady = handsFreeReady,
                        onEnabledChange = viewModel::setPauseAccessibilityInExcludedApps,
                        onOpenEnableDetection = onOpenEnableDetection,
                    )
                }
                item {
                    Text(
                        text = "${exclusionRows.size} ${if (exclusionRows.size == 1) "App" else "Apps"} Excluded",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                }
                items(exclusionRows, key = { it.first }) { (pkg, label) ->
                    ExclusionAppRow(
                        packageName = pkg,
                        label = label,
                        onRemove = { viewModel.removeExclusion(pkg) },
                    )
                }
                item {
                    Spacer(Modifier.height(8.dp))
                    NordPrimaryButton(
                        text = "Add Excluded App",
                        onClick = ::openAppPicker,
                    )
                    Text(
                        text = "Excluded apps keep their own Plus Key behavior. Everywhere else, Keyforge remaps as usual.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 12.dp),
                    )
                    Text(
                        text = "For BHIM/UPI: exclude the app, enable Auto-Pause, Unlock once. Keyforge pauses Accessibility while you pay and restores it when you leave — no daily Settings trip.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 10.dp),
                    )
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }

    if (showAppPicker) {
        AppPickerSheet(
            apps = installedApps,
            isLoading = loadingApps,
            onLoad = {},
            errorMessage = if (loadFailed && !loadingApps) {
                "Couldn't load your apps. Close this sheet and try again."
            } else {
                null
            },
            onSelect = {
                viewModel.addExclusion(it)
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false },
        )
    }
}

@Composable
private fun AutoPauseAccessibilityCard(
    enabled: Boolean,
    handsFreeReady: Boolean,
    onEnabledChange: (Boolean) -> Unit,
    onOpenEnableDetection: () -> Unit,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = enabled,
                        role = Role.Switch,
                        onValueChange = onEnabledChange,
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Pause Accessibility",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = "Turns Keyforge Accessibility off in excluded apps (so BHIM/UPI work) and back on when you leave.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = enabled,
                    onCheckedChange = null,
                )
            }
            StatusChip(
                label = if (handsFreeReady) "Hands-free ready" else "Needs Wireless Unlock once",
                tone = if (handsFreeReady) StatusTone.Active else StatusTone.Warning,
            )
            if (!handsFreeReady) {
                Text(
                    text = "Without Unlock, Auto-Pause still works but you’ll tap a notification after each payment to turn Keyforge back on.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NordPrimaryButton(
                    text = "Complete Wireless Unlock",
                    onClick = onOpenEnableDetection,
                )
            } else {
                Text(
                    text = "Daily payments: open BHIM → Keyforge pauses → pay → leave → Keyforge resumes. No Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExclusionAppRow(
    packageName: String,
    label: String,
    onRemove: () -> Unit,
) {
    val context = LocalContext.current
    var icon by remember(packageName) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(packageName) {
        icon = withContext(Dispatchers.IO) {
            runCatching {
                context.packageManager.getApplicationIcon(packageName)?.toBitmap(96, 96)
                    ?.asImageBitmap()
            }.getOrNull()
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = MaterialTheme.shapes.medium,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ExclusionAppIcon(bitmap = icon)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                )
                Text(
                    text = packageName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onRemove) {
                Icon(
                    Icons.Outlined.Close,
                    contentDescription = "Remove $label",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ExclusionAppIcon(bitmap: ImageBitmap?) {
    if (bitmap == null) {
        Text("App", style = MaterialTheme.typography.labelSmall)
        return
    }
    Image(
        bitmap = bitmap,
        contentDescription = null,
        modifier = Modifier.size(40.dp),
    )
}
