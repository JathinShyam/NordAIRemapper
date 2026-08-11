package com.nordairemapper.presentation.overlay

import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.domain.model.OverlayAnimation
import com.nordairemapper.domain.model.OverlayConfig
import com.nordairemapper.domain.model.OverlayIconSize
import com.nordairemapper.domain.model.OverlayLayoutStyle
import com.nordairemapper.domain.model.OverlayPosition
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.presentation.common.RemapActionCatalog
import com.nordairemapper.presentation.common.displayName
import com.nordairemapper.ui.components.OverlayPreview

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlaySettingsScreen(
    onBack: () -> Unit,
    viewModel: OverlaySettingsViewModel = hiltViewModel(),
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    var editingSlot by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Overlay") },
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
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Enable overlay", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Assign “Show overlay” to a press type to open this menu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = config.enabled, onCheckedChange = viewModel::setEnabled)
                }
            }

            Text("Live preview", style = MaterialTheme.typography.titleMedium)
            OverlayPreview(config = config)

            Text("Slots", style = MaterialTheme.typography.titleMedium)
            repeat(OverlayConfig.MAX_SLOTS) { index ->
                val action = config.slots.getOrNull(index) ?: RemapAction.None
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { editingSlot = index },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Slot ${index + 1}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = action.displayName(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            Text("Layout", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverlayLayoutStyle.entries.forEach { style ->
                    FilterChip(
                        selected = config.layoutStyle == style,
                        onClick = { viewModel.setLayoutStyle(style) },
                        label = { Text(style.name.replace('_', ' ')) },
                    )
                }
            }

            Text("Position", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverlayPosition.entries.forEach { position ->
                    FilterChip(
                        selected = config.position == position,
                        onClick = { viewModel.setPosition(position) },
                        label = { Text(position.name.replace('_', ' ')) },
                    )
                }
            }

            Text("Opacity ${(config.opacity * 100).toInt()}%", style = MaterialTheme.typography.titleMedium)
            Slider(
                value = config.opacity,
                onValueChange = viewModel::setOpacity,
                valueRange = 0.3f..1f,
            )

            Text("Icon size", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverlayIconSize.entries.forEach { size ->
                    FilterChip(
                        selected = config.iconSize == size,
                        onClick = { viewModel.setIconSize(size) },
                        label = { Text(size.name) },
                    )
                }
            }

            Text("Animation", style = MaterialTheme.typography.titleMedium)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverlayAnimation.entries.forEach { animation ->
                    FilterChip(
                        selected = config.animation == animation,
                        onClick = { viewModel.setAnimation(animation) },
                        label = { Text(animation.name) },
                    )
                }
            }
        }
    }

    val slot = editingSlot
    if (slot != null) {
        ActionCatalogSheet(
            onDismiss = { editingSlot = null },
            onSelect = { action ->
                viewModel.setSlot(slot, action)
                editingSlot = null
            },
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionCatalogSheet(
    onDismiss: () -> Unit,
    onSelect: (RemapAction) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text("Choose action", style = MaterialTheme.typography.titleLarge)
            RemapActionCatalog.items
                .filter { it.action !is RemapAction.LaunchApp && it.action !is RemapAction.OpenUrl }
                .forEach { item ->
                    Text(
                        text = item.action.displayName(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(item.action) }
                            .padding(vertical = 12.dp),
                        style = MaterialTheme.typography.bodyLarge,
                    )
                }
        }
    }
}
