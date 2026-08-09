package com.nordairemapper.presentation.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.service.RawKeyEvent
import com.nordairemapper.ui.theme.StatusActive
import com.nordairemapper.ui.theme.StatusInactive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Debug screen that surfaces every hardware key event the detectors can see,
 * so the Plus Key's keyCode/scanCode can be confirmed on real hardware and
 * saved as the learned identity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyLearningScreen(
    onBack: () -> Unit,
    viewModel: KeyLearningViewModel = hiltViewModel(),
) {
    val events by viewModel.capturedEvents.collectAsStateWithLifecycle()
    val serviceActive by viewModel.serviceActive.collectAsStateWithLifecycle()
    val learnedIdentity by viewModel.learnedIdentity.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.refreshServiceState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Key setup") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::clearEvents) { Text("Clear") }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                color = if (serviceActive) StatusActive else StatusInactive,
                                shape = CircleShape,
                            )
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (serviceActive) "Accessibility service active" else "Accessibility service inactive",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = if (learnedIdentity.isConfigured) {
                                "Learned key: keyCode=${learnedIdentity.keyCode}, scanCode=${learnedIdentity.scanCode}"
                            } else {
                                "No key learned yet — press the Plus Key below"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (!serviceActive) {
                        TextButton(onClick = viewModel::openAccessibilitySettings) { Text("Enable") }
                    }
                }
            }

            Text(
                text = "Press the Plus Key (or any hardware button). Every event the service can see appears below — save the one that matches your press.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(events) { event ->
                    KeyEventRow(event = event, onSave = { viewModel.saveAsPlusKey(event) })
                }
            }
        }
    }
}

@Composable
private fun KeyEventRow(event: RawKeyEvent, onSave: () -> Unit) {
    val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "keyCode=${event.keyCode}  scanCode=${event.scanCode}  ${event.action}",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = "${timeFormat.format(Date(event.timestampMs))} · ${event.source.name.lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(onClick = onSave) { Text("Set as Plus Key") }
        }
    }
}
