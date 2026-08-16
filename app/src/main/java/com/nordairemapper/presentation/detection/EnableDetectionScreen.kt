package com.nordairemapper.presentation.detection

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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.service.LogcatWatcherService
import com.nordairemapper.ui.components.NordGhostButton
import com.nordairemapper.ui.components.NordHeading
import com.nordairemapper.ui.components.NordPrimaryButton
import com.nordairemapper.ui.components.NordSurfaceCard
import com.nordairemapper.ui.components.SectionLabel
import com.nordairemapper.ui.components.StatusChip
import com.nordairemapper.ui.components.StatusTone

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
                    NordHeading("Enable detection", style = MaterialTheme.typography.titleLarge)
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
                text = "OnePlus doesn’t send the Plus Key to apps. One quick Wireless debugging pair unlocks detection. No computer needed.",
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
                            text = "Detection is ready. You can turn Wireless debugging off — the grant persists across reboots.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        if (onContinue != null) {
                            NordPrimaryButton(text = "Continue", onClick = onContinue)
                        } else {
                            NordPrimaryButton(text = "Done", onClick = onBack)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                return@Column
            }

            SectionLabel("1 · Open Wireless debugging")
            NordSurfaceCard {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Developer options → Wireless debugging → Pair device with pairing code. Leave that dialog open.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    NordPrimaryButton(
                        text = "Open Wireless debugging",
                        onClick = viewModel::openWirelessDebugging,
                    )
                    NordGhostButton(
                        text = if (state.isDiscovering) "Searching for port…" else "Find pairing port",
                        onClick = viewModel::startDiscovery,
                        enabled = !state.isDiscovering && !state.isGranting,
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

            SectionLabel("2 · Enter pairing code")
            NordSurfaceCard {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
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
                        value = state.manualPort,
                        onValueChange = viewModel::onManualPortChange,
                        label = { Text("Pairing port (if not discovered)") },
                        supportingText = {
                            Text("Shown under the code as IP:port — enter only the port.")
                        },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isGranting,
                    )
                    NordPrimaryButton(
                        text = if (state.isGranting) "Granting…" else "Pair and grant READ_LOGS",
                        onClick = viewModel::pairAndGrant,
                        enabled = !state.isGranting && state.pairingCode.length == 6,
                    )
                    Text(
                        text = "We only run pm grant for READ_LOGS. Nothing else.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
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

            TextButton(
                onClick = { viewModel.setShowAdvanced(!state.showAdvanced) },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(if (state.showAdvanced) "Hide advanced" else "Advanced: USB ADB fallback")
            }

            if (state.showAdvanced) {
                NordSurfaceCard {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "From a computer with USB debugging:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = LogcatWatcherService.ADB_GRANT_COMMAND,
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TextButton(onClick = viewModel::copyUsbAdbCommand) {
                                Text("Copy")
                            }
                            TextButton(onClick = viewModel::refresh) {
                                Text("I've granted it — Recheck")
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
