package com.nordairemapper.presentation.detection

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
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
 * Shared Unlock UI: notification gate → one-row method selector
 * (Built-In / Shizuku / Manual ADB) → the selected method's flow.
 * Used by the full Unlock screen and embedded in Lab.
 */
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
        if (granted) viewModel.startPairingWatch() else viewModel.onNearbyWifiDenied()
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        // Trust the re-probe, not the callback: refresh() also re-reads the
        // other step checks in one pass.
        viewModel.setNotificationsGranted(granted)
    }

    fun requestNearbyThenStartPairing() {
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
        viewModel.startPairingWatch()
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        if (!state.notificationsGranted) {
            NordSurfaceCard {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = "Allow notifications first",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    )
                    Text(
                        text = "Built-In pairing finishes inside a heads-up notification. Without it you'd have to switch apps mid-dialog.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        NordPrimaryButton(
                            text = "Allow notifications",
                            onClick = { notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) },
                        )
                    }
                }
            }
        }

        MethodSelectorRow(
            selected = state.method,
            onSelect = viewModel::setMethod,
            enabled = true,
        )

        when (state.method) {
            DetectionMethod.BUILTIN -> BuiltInChecklistPanel(
                state = state,
                onOpenAboutDevice = viewModel::openAboutDevice,
                onOpenWirelessDebugging = viewModel::openWirelessDebugging,
                onPairNow = { requestNearbyThenStartPairing() },
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

@Composable
private fun MethodSelectorRow(
    selected: DetectionMethod,
    onSelect: (DetectionMethod) -> Unit,
    enabled: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        DetectionMethod.entries.forEach { method ->
            val on = selected == method
            val shape = MaterialTheme.shapes.small
            Column(
                modifier = Modifier
                    .weight(1f)
                    .selectable(
                        selected = on,
                        enabled = enabled,
                        role = Role.RadioButton,
                        onClick = { onSelect(method) },
                    )
                    .background(
                        color = if (on) {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f)
                        } else {
                            MaterialTheme.colorScheme.surface
                        },
                        shape = shape,
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (on) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                        ),
                        shape,
                    )
                    .padding(horizontal = 4.dp, vertical = 12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = method.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
                    color = if (on) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    maxLines = 1,
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

// ─── Built-In checklist ─────────────────────────────────────────────────────

@Composable
private fun BuiltInChecklistPanel(
    state: EnableDetectionUiState,
    onOpenAboutDevice: () -> Unit,
    onOpenWirelessDebugging: () -> Unit,
    onPairNow: () -> Unit,
) {
    val prerequisitesOk = state.devOptionsEnabled && state.wifiDebugEnabled
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        ChecklistStep(
            stepNumber = 1,
            title = "Enable developer options",
            done = state.devOptionsEnabled,
            body = if (state.devOptionsEnabled) {
                "Done."
            } else {
                "Open About device, scroll to Build number, and tap it 5–7 times until “You are now a developer” appears."
            },
            actionLabel = if (state.devOptionsEnabled) null else "Open About device",
            onAction = onOpenAboutDevice,
        )
        ChecklistStep(
            stepNumber = 2,
            title = "Turn on Wireless debugging",
            done = state.wifiDebugEnabled,
            enabled = state.devOptionsEnabled,
            body = when {
                state.wifiDebugEnabled -> "Done."
                !state.devOptionsEnabled -> "Available after step 1."
                else -> "Flip the Wireless debugging toggle on this page, then come back."
            },
            actionLabel = if (!state.devOptionsEnabled || state.wifiDebugEnabled) null else "Open Wireless debugging",
            onAction = onOpenWirelessDebugging,
        )
        ChecklistStep(
            stepNumber = 3,
            title = "Tap Pair now, then open “Pair device with pairing code”",
            done = state.readLogsGranted,
            // The whole completion path lives in a notification — without
            // POST_NOTIFICATIONS this step can never succeed.
            enabled = prerequisitesOk && state.notificationsGranted,
            body = when {
                state.readLogsGranted ->
                    "Done — you're unlocked."
                !state.notificationsGranted ->
                    "Allow notifications above first — the code box lives there."
                state.discoveredPort != null ->
                    "Port ${state.discoveredPort} detected — enter the 6-digit code in the Keyforge notification."
                state.isWatchingForPairing ->
                    "Watching for the pairing dialog… open “Pair device with pairing code” now; the notification will turn into a code box."
                prerequisitesOk ->
                    "Keyforge posts a floating notification — you'll type the code there, never here. No forms."
                else -> "Complete steps 1–2 first."
            },
            actionLabel = when {
                state.readLogsGranted -> null
                state.isWatchingForPairing -> null
                !prerequisitesOk || !state.notificationsGranted -> null
                state.discoveredPort != null -> "Pair now (new code)"
                else -> "Pair now"
            },
            primary = true,
            onAction = onPairNow,
        )
    }
}

@Composable
private fun ChecklistStep(
    stepNumber: Int,
    title: String,
    done: Boolean,
    body: String,
    actionLabel: String?,
    onAction: () -> Unit,
    enabled: Boolean = true,
    primary: Boolean = false,
) {
    val accent = when {
        done -> StatusActiveGreen
        !enabled -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        else -> MaterialTheme.colorScheme.primary
    }
    NordSurfaceCard {
        Row(
            modifier = Modifier.padding(14.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.CheckCircle,
                contentDescription = if (done) "Done" else "Not done yet",
                tint = accent,
                modifier = Modifier.size(22.dp),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "$stepNumber · $title",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                actionLabel?.let { label ->
                    if (primary) {
                        NordPrimaryButton(text = label, onClick = onAction)
                    } else {
                        NordGhostButton(text = label, onClick = onAction)
                    }
                }
            }
        }
    }
}

private val StatusActiveGreen @Composable get() = MaterialTheme.colorScheme.tertiary

// ─── Shizuku ─────────────────────────────────────────────────────────────────

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

// ─── Manual ADB ──────────────────────────────────────────────────────────────

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
