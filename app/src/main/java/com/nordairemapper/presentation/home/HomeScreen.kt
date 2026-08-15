package com.nordairemapper.presentation.home

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.outlined.BugReport
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.UploadFile
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.domain.model.PressType
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.presentation.common.displayName
import com.nordairemapper.ui.components.ActionCard
import com.nordairemapper.ui.components.NordHeading
import com.nordairemapper.ui.components.NordPrimaryButton
import com.nordairemapper.ui.components.PhoneDiagram
import com.nordairemapper.ui.components.SectionLabel
import com.nordairemapper.ui.components.StatusChip
import com.nordairemapper.ui.components.StatusTone
import com.nordairemapper.ui.theme.StatusWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenRemap: (PressType) -> Unit,
    onOpenKeyLearning: () -> Unit,
    onOpenDeveloper: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenBackup: () -> Unit,
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
                title = {
                    Column {
                        NordHeading("Plus Key", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Nord AI Remapper",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenBackup) {
                        Icon(Icons.Outlined.UploadFile, contentDescription = "Backup")
                    }
                    IconButton(onClick = onOpenOverlaySettings) {
                        Icon(Icons.Outlined.Layers, contentDescription = "Overlay")
                    }
                    IconButton(onClick = onOpenDeveloper) {
                        Icon(Icons.Outlined.BugReport, contentDescription = "Developer")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Outlined.Settings, contentDescription = "Settings")
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PhoneDiagram(
                highlightKey = state.serviceEnabled && state.accessibilityEnabled,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(220.dp)
                    .padding(vertical = 6.dp),
            )

            StatusChip(
                label = when {
                    !state.accessibilityEnabled -> "Accessibility off"
                    state.serviceEnabled -> "Service active"
                    else -> "Remapping paused"
                },
                tone = when {
                    !state.accessibilityEnabled -> StatusTone.Inactive
                    state.serviceEnabled -> StatusTone.Active
                    else -> StatusTone.Warning
                },
                onClick = if (!state.accessibilityEnabled) {
                    { viewModel.openAccessibilitySettings() }
                } else {
                    null
                },
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Remapping",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
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

            SectionLabel("Actions")
            PressType.entries.forEach { pressType ->
                val action = state.actions[pressType] ?: RemapAction.None
                ActionCard(
                    title = when (pressType) {
                        PressType.SINGLE -> "Single"
                        PressType.DOUBLE -> "Double"
                        PressType.LONG -> "Long"
                    },
                    subtitle = action.displayName(),
                    badge = when (pressType) {
                        PressType.SINGLE -> "1"
                        PressType.DOUBLE -> "2"
                        PressType.LONG -> "—"
                    },
                    showConflict = pressType in state.conflictPressTypes,
                    onClick = { onOpenRemap(pressType) },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun TroubleshootingBanner(
    banner: HomeBanner,
    onPrimary: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.35f)),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                banner.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = StatusWarning,
            )
            Text(
                text = banner.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NordPrimaryButton(text = banner.primaryLabel, onClick = onPrimary)
        }
    }
}
