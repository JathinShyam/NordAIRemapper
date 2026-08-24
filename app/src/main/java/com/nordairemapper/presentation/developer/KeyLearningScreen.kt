package com.nordairemapper.presentation.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.presentation.common.relativeLastSeen
import com.nordairemapper.presentation.common.rememberNowTicker
import com.nordairemapper.ui.components.NordTopBarTitle
import com.nordairemapper.ui.components.NordPrimaryButton
import com.nordairemapper.ui.components.NordSurfaceCard
import com.nordairemapper.ui.components.SectionLabel
import com.nordairemapper.ui.theme.StatusActive
import com.nordairemapper.ui.theme.StatusInactive
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Debug screen that surfaces every hardware key press the detectors can see,
 * so the Plus Key's keyCode/scanCode can be confirmed on real hardware and
 * saved as the learned identity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyLearningScreen(
    onBack: () -> Unit,
    onOpenEnableDetection: () -> Unit,
    viewModel: KeyLearningViewModel = hiltViewModel(),
) {
    val presses by viewModel.capturedPresses.collectAsStateWithLifecycle()
    val serviceActive by viewModel.serviceActive.collectAsStateWithLifecycle()
    val learnedIdentity by viewModel.learnedIdentity.collectAsStateWithLifecycle()
    val plusKeyMissingHint by viewModel.plusKeyMissingHint.collectAsStateWithLifecycle()
    val lastPlusKeySeenAtMs by viewModel.lastPlusKeySeenAtMs.collectAsStateWithLifecycle()
    val logcatPlusKeySeen by viewModel.logcatPlusKeySeen.collectAsStateWithLifecycle()
    val nowMs = rememberNowTicker()

    LaunchedEffect(Unit) { viewModel.refreshServiceState() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    NordTopBarTitle(
                        title = "Key setup",
                        subtitle = "Listening for Plus Key",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    TextButton(onClick = viewModel::clearEvents) { Text("Clear") }
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
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            NordSurfaceCard {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(
                                color = if (serviceActive) StatusActive else StatusInactive,
                                shape = CircleShape,
                            ),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (serviceActive) {
                                "Accessibility active"
                            } else {
                                "Accessibility inactive"
                            },
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            text = if (lastPlusKeySeenAtMs > 0) {
                                "Last Plus Key press: " + relativeLastSeen(lastPlusKeySeenAtMs, nowMs)
                            } else {
                                "No Plus Key press recorded yet"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = when {
                                logcatPlusKeySeen -> "Plus Key via logcat — no keyCode to save"
                                learnedIdentity.isConfigured ->
                                    "Learned key: keyCode=${learnedIdentity.keyCode}, scanCode=${learnedIdentity.scanCode}"
                                else -> "No key learned yet — press the Plus Key below"
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

            if (logcatPlusKeySeen) {
                NordSurfaceCard {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Plus Key detected via logcat",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            text = "OnePlus never sends this button to Accessibility, so that warning does not apply. You do not need to tap Set as Plus Key. Go back to Home and test single / double / hold.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            } else if (plusKeyMissingHint) {
                NordSurfaceCard {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Plus Key not reaching Accessibility",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            text = "Volume and other buttons are visible, but the Plus Key is handled by OnePlus system code and never arrives as a KeyEvent. That is expected on Nord 5 — not an Accessibility bug.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            text = "Unlock detection once (USB preferred, or Wireless as advanced) so the logcat companion can detect the Plus Key. Accessibility stays required for screenshot, lock, and other system actions.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        NordPrimaryButton(text = "Unlock detection", onClick = onOpenEnableDetection)
                    }
                }
            }

            SectionLabel("Captured presses")
            Text(
                text = "Each row is one physical press. Volume keys come from Accessibility. The Plus Key usually appears only as a logcat row.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (presses.isEmpty()) {
                    item {
                        NordSurfaceCard {
                            Text(
                                text = "Listening… press any key. Volume keys prove Accessibility works; " +
                                    "on Nord 5 the Plus Key usually only appears as a logcat row.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(14.dp),
                            )
                        }
                    }
                }
                items(presses, key = { it.id }) { press ->
                    PressRow(press = press, onSave = { viewModel.saveAsPlusKey(press) })
                }
            }
        }
    }
}

/** One formatter shared by all press rows (main-thread only). */
private val pressTimeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

@Composable
private fun PressRow(press: CapturedPress, onSave: () -> Unit) {
    val duration = press.durationMs?.let { " · ${it}ms" }.orEmpty()
    NordSurfaceCard {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${press.label}  keyCode=${press.keyCode}  scanCode=${press.scanCode}",
                    style = MaterialTheme.typography.labelLarge,
                )
                Text(
                    text = "${pressTimeFormat.format(Date(press.timestampMs))}$duration · ${press.source.name.lowercase()}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            TextButton(
                onClick = onSave,
                enabled = !press.isLogcatPlusKey && !press.isSystemKey,
            ) {
                Text(
                    when {
                        press.isLogcatPlusKey -> "Logcat"
                        press.isSystemKey -> "System key"
                        else -> "Set as Plus Key"
                    },
                )
            }
        }
    }
}
