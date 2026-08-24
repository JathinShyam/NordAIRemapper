package com.nordairemapper.presentation.detection

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.service.LogcatWatcherService
import com.nordairemapper.ui.components.NordGhostButton
import com.nordairemapper.ui.components.NordTopBarTitle
import com.nordairemapper.ui.components.NordPrimaryButton
import com.nordairemapper.ui.components.NordSurfaceCard
import com.nordairemapper.ui.components.SectionLabel
import com.nordairemapper.ui.components.StatusChip
import com.nordairemapper.ui.components.StatusTone

/**
 * Shared Unlock UI: three method cards (Built-In / Shizuku / Manual ADB) and
 * the panel of the selected one, plus status/error lines. Used by the full
 * Unlock screen and embedded in Lab.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UnlockMethodsSection(
    viewModel: EnableDetectionViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val nearbyWifiLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            viewModel.startDiscovery()
        } else {
            viewModel.onNearbyWifiDenied()
        }
    }

    fun requestNearbyWifiThenDiscover() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.NEARBY_WIFI_DEVICES,
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                nearbyWifiLauncher.launch(Manifest.permission.NEARBY_WIFI_DEVICES)
                return
            }
        }
        viewModel.startDiscovery()
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionLabel("Choose a method")
        DetectionMethod.entries.forEach { method ->
            MethodOptionCard(
                method = method,
                selected = state.method == method,
                subStatus = when (method) {
                    DetectionMethod.SHIZUKU -> shizukuStatusLabel(state)
                    else -> null
                },
                onSelect = { viewModel.setMethod(method) },
            )
        }

        when (state.method) {
            DetectionMethod.BUILTIN -> BuiltInMethodPanel(
                state = state,
                onPairingCodeChange = viewModel::onPairingCodeChange,
                onPairingPortChange = viewModel::onPairingPortChange,
                onConnectPortChange = viewModel::onConnectPortChange,
                onOpenWirelessDebugging = {
                    viewModel.openWirelessDebugging()
                    requestNearbyWifiThenDiscover()
                },
                onOpenDeveloperOptions = viewModel::openDeveloperOptions,
                onFindPort = { requestNearbyWifiThenDiscover() },
                onPairAndGrant = viewModel::pairAndGrant,
            )
            DetectionMethod.SHIZUKU -> ShizukuMethodPanel(
                state = state,
                onGrant = viewModel::requestShizukuThenGrant,
                onOpenShizukuApp = viewModel::openShizukuApp,
                onRecheck = viewModel::refreshShizukuState,
            )
            DetectionMethod.MANUAL_ADB -> ManualAdbMethodPanel(
                state = state,
                onCopy = viewModel::copyUsbAdbCommand,
                onRecheck = viewModel::refresh,
            )
        }

        state.statusMessage?.let { msg ->
            Text(
                text = msg,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        state.errorMessage?.let { msg ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(
                    imageVector = Icons.Outlined.ErrorOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

/** Full-screen wrapper around [UnlockMethodsSection] with status header. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnableDetectionScreen(
    onBack: () -> Unit,
    onContinue: (() -> Unit)? = null,
    viewModel: EnableDetectionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    NordTopBarTitle(
                        title = "Unlock",
                        subtitle = "One-time detection setup",
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
                .padding(horizontal = 20.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                text = "OnePlus doesn’t send the Plus Key to apps. Unlock once — logcat detection and hands-free banking pause use the same grant.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            StatusChip(
                label = if (state.readLogsGranted) "READ_LOGS granted" else "READ_LOGS needed",
                tone = if (state.readLogsGranted) StatusTone.Active else StatusTone.Warning,
            )
            state.logAccessVisible?.let { visible ->
                StatusChip(
                    label = if (visible) {
                        "System log access verified"
                    } else {
                        "Blocked: allow log access when prompted, then reopen"
                    },
                    tone = if (visible) StatusTone.Active else StatusTone.Warning,
                )
            }
            if (state.readLogsGranted) {
                StatusChip(
                    label = if (state.bankingAutoResumeReady) {
                        "Banking auto-pause ready"
                    } else {
                        "Banking auto-pause needs Unlock"
                    },
                    tone = if (state.bankingAutoResumeReady) StatusTone.Active else StatusTone.Warning,
                )
            }

            if (state.readLogsGranted && state.bankingAutoResumeReady) {
                NordSurfaceCard {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Unlocked. Open Home to assign presses, or Exclusions → Auto-Pause for BHIM/UPI.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        NordPrimaryButton(
                            text = "Open Home",
                            onClick = onContinue ?: onBack,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                return@Column
            }

            if (state.readLogsGranted && !state.bankingAutoResumeReady) {
                NordSurfaceCard {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Detection works. Run Unlock once more with any method below so Keyforge can pause Accessibility in banking apps and turn it back on when you leave — no daily Settings trip.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            UnlockMethodsSection(viewModel)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

private fun shizukuStatusLabel(state: EnableDetectionUiState): String = when {
    !state.shizukuInstalled -> "Shizuku app not installed"
    !state.shizukuRunning -> "Shizuku not running"
    !state.shizukuGranted -> "Tap to allow Keyforge in Shizuku"
    else -> "Ready"
}

@Composable
private fun MethodOptionCard(
    method: DetectionMethod,
    selected: Boolean,
    subStatus: String?,
    onSelect: () -> Unit,
) {
    val shape = MaterialTheme.shapes.medium
    Card(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, role = Role.RadioButton, onClick = onSelect),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
            } else {
                MaterialTheme.colorScheme.surface
            },
        ),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = shape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = method.label,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = method.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                subStatus?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Medium,
                        color = if (it == "Ready") {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(top = 2.dp),
                    )
                }
            }
            if (selected) {
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun BuiltInMethodPanel(
    state: EnableDetectionUiState,
    onPairingCodeChange: (String) -> Unit,
    onPairingPortChange: (String) -> Unit,
    onConnectPortChange: (String) -> Unit,
    onOpenWirelessDebugging: () -> Unit,
    onOpenDeveloperOptions: () -> Unit,
    onFindPort: () -> Unit,
    onPairAndGrant: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        NordSurfaceCard {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "If you see a screen with your Wi‑Fi name (SSID) and Cancel / Allow — that is not the pairing code. Tap Allow.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = "Then open Wireless debugging → Pair device with pairing code. Leave that dialog open.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                NordPrimaryButton(text = "Open Wireless debugging", onClick = onOpenWirelessDebugging)
                NordGhostButton(
                    text = "Open Developer options instead",
                    onClick = onOpenDeveloperOptions,
                )
                NordGhostButton(
                    text = if (state.isDiscovering) "Searching for port…" else "Find pairing port",
                    onClick = onFindPort,
                    enabled = !state.isDiscovering && !state.isGranting,
                    loading = state.isDiscovering,
                )
                if (state.discoveredPort != null) {
                    Text(
                        text = "Discovered port ${state.discoveredPort}" +
                            (state.discoveredHost?.let { " @ $it" } ?: ""),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = "Heads-up: you can type the 6-digit code straight into the Keyforge notification while the pairing dialog is open.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        NordSurfaceCard {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "Pairing port and Connection port are different. Pairing uses the port under the 6-digit code. Connection uses “IP address & port” on the main Wireless debugging page.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = state.pairingCode,
                    onValueChange = onPairingCodeChange,
                    label = { Text("6-digit pairing code") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isGranting,
                )
                OutlinedTextField(
                    value = state.pairingPort,
                    onValueChange = onPairingPortChange,
                    label = { Text("Pairing port") },
                    supportingText = { Text("Under the code: 192.168.x.x:PAIRING_PORT") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isGranting,
                )
                OutlinedTextField(
                    value = state.connectPort,
                    onValueChange = onConnectPortChange,
                    label = { Text("Connection port (if connect fails)") },
                    supportingText = { Text("Wireless debugging page → IP address & port") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isGranting,
                )
                NordPrimaryButton(
                    text = if (state.isGranting) "Granting…" else "Pair and grant Unlock",
                    onClick = onPairAndGrant,
                    enabled = !state.isGranting && state.pairingCode.length == 6,
                    loading = state.isGranting,
                )
            }
        }
    }
}

@Composable
private fun ShizukuMethodPanel(
    state: EnableDetectionUiState,
    onGrant: () -> Unit,
    onOpenShizukuApp: () -> Unit,
    onRecheck: () -> Unit,
) {
    NordSurfaceCard {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ShizukuStatusRow(label = "Shizuku installed", ok = state.shizukuInstalled)
            ShizukuStatusRow(label = "Shizuku service running", ok = state.shizukuRunning)
            ShizukuStatusRow(
                label = "Keyforge allowed in Shizuku",
                ok = state.shizukuGranted || !state.shizukuRunning,
            )
            Text(
                text = "Grants the same three permissions as the other methods — nothing more. Grants persist even if you stop Shizuku afterwards.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NordPrimaryButton(
                text = if (state.isGrantingViaShizuku) "Granting…" else "Unlock via Shizuku",
                onClick = onGrant,
                enabled = !state.isGrantingViaShizuku && state.shizukuRunning,
                loading = state.isGrantingViaShizuku,
            )
            if (!state.shizukuRunning) {
                NordGhostButton(text = "Open Shizuku app", onClick = onOpenShizukuApp)
            }
            NordGhostButton(text = "Recheck", onClick = onRecheck)
        }
    }
}

@Composable
private fun ShizukuStatusRow(label: String, ok: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = if (ok) Color(0xFF3DDC84) else MaterialTheme.colorScheme.outline,
            modifier = Modifier.size(16.dp),
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ManualAdbMethodPanel(
    state: EnableDetectionUiState,
    onCopy: () -> Unit,
    onRecheck: () -> Unit,
) {
    NordSurfaceCard {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "From a computer with USB debugging enabled, run:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Card(
                onClick = onCopy,
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                ),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = MaterialTheme.shapes.small,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Text(
                    text = LogcatWatcherService.ADB_GRANT_COMMAND,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                )
            }
            Text(
                text = "Tap to copy",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NordPrimaryButton(text = "I've run it — Recheck", onClick = onRecheck)
            Text(
                text = "Grants READ_LOGS plus banking auto-pause permissions. Nothing else.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
