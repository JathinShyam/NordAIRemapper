package com.nordairemapper.presentation.developer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.domain.model.AppSettings
import com.nordairemapper.domain.model.DetectionStrategy
import com.nordairemapper.service.LogcatWatcherService

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperScreen(
    onBack: () -> Unit,
    onOpenKeyLearning: () -> Unit,
    viewModel: DeveloperViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val readLogsGranted by viewModel.readLogsGranted.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refreshPermissions() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Developer") },
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
            SectionCard(title = "Detection strategy") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = settings.detectionStrategy == DetectionStrategy.ACCESSIBILITY,
                        onClick = { viewModel.setStrategy(DetectionStrategy.ACCESSIBILITY) },
                        label = { Text("Accessibility") },
                    )
                    FilterChip(
                        selected = settings.detectionStrategy == DetectionStrategy.LOGCAT,
                        onClick = { viewModel.setStrategy(DetectionStrategy.LOGCAT) },
                        label = { Text("Logcat watcher") },
                    )
                }
                Text(
                    text = "Accessibility observes raw key events; on OnePlus the Plus Key is often only visible in system logs, which the logcat watcher picks up.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (settings.detectionStrategy == DetectionStrategy.LOGCAT) {
                SectionCard(title = "Logcat watcher") {
                    Text(
                        text = if (readLogsGranted) "READ_LOGS granted" else "READ_LOGS not granted — run this once from a computer:",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (readLogsGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                    )
                    if (!readLogsGranted) {
                        Text(
                            text = LogcatWatcherService.ADB_GRANT_COMMAND,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        TextButton(onClick = viewModel::copyAdbCommand) { Text("Copy ADB command") }
                    }

                    var pattern by rememberSaveable(settings.logcatPattern) {
                        mutableStateOf(settings.logcatPattern)
                    }
                    OutlinedTextField(
                        value = pattern,
                        onValueChange = { pattern = it },
                        label = { Text("Log match pattern") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { viewModel.setLogcatPattern(pattern) }) { Text("Save pattern") }
                        TextButton(onClick = viewModel::restartLogcatWatcher) { Text("Restart watcher") }
                    }
                }
            }

            SectionCard(title = "Gesture timing") {
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

            SectionCard(title = "Key identity") {
                Text(
                    text = if (settings.keyIdentity.isConfigured) {
                        "keyCode=${settings.keyIdentity.keyCode}, scanCode=${settings.keyIdentity.scanCode}"
                    } else {
                        "Not learned yet"
                    },
                    style = MaterialTheme.typography.bodyMedium,
                )
                Button(onClick = onOpenKeyLearning, modifier = Modifier.fillMaxWidth()) {
                    Text("Open key setup")
                }
            }
        }
    }
}

@Composable
private fun SectionCard(title: String, content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleMedium)
            content()
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
    Column {
        Text(
            text = "$label: ${valueMs}ms",
            style = MaterialTheme.typography.labelLarge,
        )
        Slider(
            value = valueMs.toFloat(),
            onValueChange = { onChange(it.toLong()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
        )
    }
}
