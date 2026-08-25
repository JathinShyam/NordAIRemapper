package com.nordairemapper.presentation.developer

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.domain.model.AppSettings
import com.nordairemapper.domain.model.DetectionStrategy
import com.nordairemapper.presentation.detection.EnableDetectionViewModel
import com.nordairemapper.presentation.detection.UnlockMethodsSection
import com.nordairemapper.presentation.settings.SettingsGroup
import com.nordairemapper.presentation.settings.SettingsSegmentOption
import com.nordairemapper.presentation.settings.SettingsSegmentedControl
import com.nordairemapper.ui.components.NordTopBarTitle
import com.nordairemapper.ui.components.NordPrimaryButton
import com.nordairemapper.ui.components.NordSurfaceCard
import com.nordairemapper.ui.components.SectionLabel
import com.nordairemapper.ui.components.StatusChip
import com.nordairemapper.ui.components.StatusTone

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(
    onBack: () -> Unit,
    onOpenKeyLearning: () -> Unit,
    onOpenEnableDetection: () -> Unit,
    viewModel: DeveloperViewModel = hiltViewModel(),
    unlockViewModel: EnableDetectionViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
        unlockViewModel.refresh()
    }

    // Returning from Settings (developer options / wireless debugging toggle)
    // must re-run the step checks immediately.
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) unlockViewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    NordTopBarTitle(
                        title = "Lab",
                        subtitle = "Strategy, timing, and unlock",
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionLabel("Detection strategy")
            SettingsGroup {
                SettingsSegmentedControl(
                    options = listOf(
                        SettingsSegmentOption(DetectionStrategy.AUTO.key, "Auto"),
                        SettingsSegmentOption(DetectionStrategy.ACCESSIBILITY.key, "A11y"),
                        SettingsSegmentOption(DetectionStrategy.LOGCAT.key, "Logcat"),
                    ),
                    selectedKey = settings.detectionStrategy.key,
                    onSelect = { key ->
                        DetectionStrategy.fromKey(key).let(viewModel::setStrategy)
                    },
                    modifier = Modifier.padding(start = 14.dp, top = 14.dp, end = 14.dp),
                )
                Text(
                    text = when (settings.detectionStrategy) {
                        DetectionStrategy.AUTO ->
                            "Uses Accessibility when the OS delivers the Plus Key, and logcat when it does not (Nord 5). Recommended."
                        DetectionStrategy.ACCESSIBILITY ->
                            "Listens for KeyEvents. On Nord 5 the Plus Key almost never arrives here (volume keys do). A logcat companion runs when READ_LOGS is granted."
                        DetectionStrategy.LOGCAT ->
                            "Watches system logs for KEYCODE_ACTION_BUTTON_CLICK. Requires READ_LOGS."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 14.dp, top = 10.dp, end = 14.dp, bottom = 14.dp),
                )
            }

            // Same layout as the Unlock screen: flat status chips + method
            // flow at page level — no card-in-card nesting.
            SectionLabel("Unlock · Log access for Plus Key")
            val unlockState by unlockViewModel.uiState.collectAsStateWithLifecycle()
            StatusChip(
                label = if (unlockState.readLogsGranted) "READ_LOGS granted" else "READ_LOGS needed",
                tone = if (unlockState.readLogsGranted) StatusTone.Active else StatusTone.Warning,
            )
            unlockState.logAccessVisible?.let { visible ->
                StatusChip(
                    label = if (visible) {
                        "System log access verified"
                    } else {
                        "Blocked: allow log access when prompted, then reopen"
                    },
                    tone = if (visible) StatusTone.Active else StatusTone.Warning,
                )
            }
            if (unlockState.readLogsGranted && !unlockState.bankingAutoResumeReady) {
                StatusChip(
                    label = "Banking auto-pause needs Unlock",
                    tone = StatusTone.Warning,
                )
            }

            if (unlockState.readLogsGranted && unlockState.bankingAutoResumeReady) {
                Text(
                    text = "Unlocked. Logcat detection and hands-free banking pause are ready.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            } else {
                UnlockMethodsSection(viewModel = unlockViewModel)
                TextButton(onClick = onOpenEnableDetection) {
                    Text("Open full Unlock screen")
                }
            }

            SectionLabel("Log match pattern")
            NordSurfaceCard {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    var pattern by rememberSaveable(settings.logcatPattern) {
                        mutableStateOf(settings.logcatPattern)
                    }
                    OutlinedTextField(
                        value = pattern,
                        onValueChange = { pattern = it },
                        label = { Text("Log match pattern") },
                        supportingText = {
                            Text("Nord 5 default is KEYCODE_ACTION_BUTTON_CLICK (one down/up per press).")
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { viewModel.setLogcatPattern(pattern) }) {
                            Text("Save pattern")
                        }
                        TextButton(onClick = viewModel::restartLogcatWatcher) {
                            Text("Restart watcher")
                        }
                    }
                }
            }

            SectionLabel("Gesture timing")
            NordSurfaceCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    TimingSlider(
                        label = "Double-press window",
                        valueMs = settings.doublePressWindowMs,
                        range = AppSettings.DOUBLE_PRESS_WINDOW_RANGE,
                        onChange = viewModel::setDoublePressWindow,
                    )
                    TimingSlider(
                        label = "Long-press threshold",
                        valueMs = settings.longPressThresholdMs,
                        range = AppSettings.LONG_PRESS_THRESHOLD_RANGE,
                        onChange = viewModel::setLongPressThreshold,
                    )
                }
            }

            SectionLabel("Key identity")
            NordSurfaceCard {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = if (settings.keyIdentity.isConfigured) {
                            "keyCode=${settings.keyIdentity.keyCode}, scanCode=${settings.keyIdentity.scanCode}"
                        } else {
                            "Not learned yet"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    NordPrimaryButton(text = "Open key setup", onClick = onOpenKeyLearning)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TimingSlider(
    label: String,
    valueMs: Long,
    range: LongRange,
    onChange: (Long) -> Unit,
) {
    // Drag locally; commit once on release so we don't write a DataStore
    // value (and restart detection work) on every tick.
    var pendingMs by remember(valueMs) { mutableStateOf(valueMs.toFloat()) }
    Column {
        Text(
            text = "$label · ${pendingMs.toLong()}ms",
            style = MaterialTheme.typography.titleSmall,
        )
        Slider(
            value = pendingMs,
            onValueChange = { pendingMs = it },
            onValueChangeFinished = { onChange(pendingMs.toLong()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "${range.first}ms",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${range.last}ms",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
