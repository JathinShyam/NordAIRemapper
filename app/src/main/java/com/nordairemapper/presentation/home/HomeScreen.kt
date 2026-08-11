package com.nordairemapper.presentation.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.domain.model.PressType
import com.nordairemapper.presentation.common.displayName
import com.nordairemapper.presentation.common.icon
import com.nordairemapper.ui.components.ActionCard
import com.nordairemapper.ui.components.PhoneDiagram
import com.nordairemapper.ui.theme.StatusActive
import com.nordairemapper.ui.theme.StatusInactive

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenRemap: (PressType) -> Unit,
    onOpenKeyLearning: () -> Unit,
    onOpenDeveloper: () -> Unit,
    onOpenSettings: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshRuntimeFlags()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Nord AI Remapper") },
                actions = {
                    IconButton(onClick = onOpenDeveloper) {
                        Icon(Icons.Outlined.BugReport, contentDescription = "Developer")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            StatusRow(
                active = state.accessibilityEnabled && state.serviceEnabled,
                accessibilityEnabled = state.accessibilityEnabled,
                onClick = {
                    if (!state.accessibilityEnabled) viewModel.openAccessibilitySettings()
                },
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Remapping enabled", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Master switch for Plus Key actions",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(
                        checked = state.serviceEnabled,
                        onCheckedChange = viewModel::setServiceEnabled,
                    )
                }
            }

            PhoneDiagram(
                highlightKey = state.serviceEnabled && state.accessibilityEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp),
            )

            state.banner?.let { banner ->
                TroubleshootingBanner(
                    banner = banner,
                    onPrimary = {
                        when (banner.primaryAction) {
                            HomeBannerAction.OPEN_ACCESSIBILITY -> viewModel.openAccessibilitySettings()
                            HomeBannerAction.OPEN_KEY_LEARNING -> onOpenKeyLearning()
                            HomeBannerAction.OPEN_DEVELOPER -> onOpenDeveloper()
                        }
                    },
                )
            }

            PressType.entries.forEach { pressType ->
                val action = state.actions[pressType] ?: com.nordairemapper.domain.model.RemapAction.None
                ActionCard(
                    title = pressType.label,
                    subtitle = action.displayName(),
                    icon = action.icon(),
                    showConflict = pressType in state.conflictPressTypes,
                    onClick = { onOpenRemap(pressType) },
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
private fun StatusRow(
    active: Boolean,
    accessibilityEnabled: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = !accessibilityEnabled, onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(if (active) StatusActive else StatusInactive, CircleShape),
        )
        Text(
            text = when {
                !accessibilityEnabled -> "Accessibility inactive — tap to fix"
                active -> "Service active"
                else -> "Remapping paused"
            },
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun TroubleshootingBanner(
    banner: HomeBanner,
    onPrimary: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(banner.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = banner.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(onClick = onPrimary) { Text(banner.primaryLabel) }
        }
    }
}
