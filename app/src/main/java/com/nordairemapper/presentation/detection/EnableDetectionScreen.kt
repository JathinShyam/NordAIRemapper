package com.nordairemapper.presentation.detection

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontFamily
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
 * Unlock Plus Key detection (READ_LOGS).
 * Nord Edge happy path: one USB ADB grant. Wireless debugging is advanced.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EnableDetectionScreen(
    onBack: () -> Unit,
    onContinue: (() -> Unit)? = null,
    viewModel: EnableDetectionViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
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
                text = "OnePlus doesn’t send the Plus Key to apps. Unlock logcat detection once — then you’re done.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            StatusChip(
                label = if (state.readLogsGranted) "READ_LOGS granted" else "READ_LOGS needed",
                tone = if (state.readLogsGranted) StatusTone.Active else StatusTone.Warning,
            )

            if (state.readLogsGranted) {
                NordSurfaceCard {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Detection unlocked. Open Home to assign Single, Double, and Long press.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (onContinue != null) {
                            NordPrimaryButton(text = "Open Home", onClick = onContinue)
                        } else {
                            NordPrimaryButton(text = "Open Home", onClick = onBack)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                return@Column
            }

            SectionLabel("1 · Preferred: USB")
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
                        onClick = viewModel::copyUsbAdbCommand,
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
                    NordPrimaryButton(
                        text = "I've run it — Recheck",
                        onClick = viewModel::refresh,
                    )
                    Text(
                        text = "We only grant READ_LOGS. Nothing else.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            TextButton(
                onClick = { viewModel.setShowAdvanced(!state.showAdvanced) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    if (state.showAdvanced) {
                        "Hide Wireless path"
                    } else {
                        "No computer — Wireless debugging (advanced)"
                    },
                )
            }

            if (state.showAdvanced) {
                SectionLabel("2 · Wireless debugging")
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
                        NordPrimaryButton(
                            text = "Open Wireless debugging",
                            onClick = {
                                viewModel.openWirelessDebugging()
                                requestNearbyWifiThenDiscover()
                            },
                        )
                        NordGhostButton(
                            text = "Open Developer options instead",
                            onClick = viewModel::openDeveloperOptions,
                        )
                        NordGhostButton(
                            text = if (state.isDiscovering) "Searching for port…" else "Find pairing port",
                            onClick = { requestNearbyWifiThenDiscover() },
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
                        }
                    }
                }

                SectionLabel("3 · Pair, then connect")
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
                            onValueChange = viewModel::onPairingCodeChange,
                            label = { Text("6-digit pairing code") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isGranting,
                        )
                        OutlinedTextField(
                            value = state.pairingPort,
                            onValueChange = viewModel::onPairingPortChange,
                            label = { Text("Pairing port") },
                            supportingText = {
                                Text("Under the code: 192.168.x.x:PAIRING_PORT")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isGranting,
                        )
                        OutlinedTextField(
                            value = state.connectPort,
                            onValueChange = viewModel::onConnectPortChange,
                            label = { Text("Connection port (if connect fails)") },
                            supportingText = {
                                Text("Wireless debugging page → IP address & port")
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth(),
                            enabled = !state.isGranting,
                        )
                        NordPrimaryButton(
                            text = if (state.isGranting) "Granting…" else "Pair and grant READ_LOGS",
                            onClick = viewModel::pairAndGrant,
                            enabled = !state.isGranting && state.pairingCode.length == 6,
                            loading = state.isGranting,
                        )
                    }
                }
            }

            state.statusMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            state.errorMessage?.let { msg ->
                Text(
                    text = msg,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
