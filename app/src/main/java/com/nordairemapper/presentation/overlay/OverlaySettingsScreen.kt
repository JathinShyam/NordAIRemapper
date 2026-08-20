package com.nordairemapper.presentation.overlay

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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
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
import com.nordairemapper.presentation.common.icon
import com.nordairemapper.ui.components.ActionCard
import com.nordairemapper.ui.components.NordHeading
import com.nordairemapper.ui.components.OverlayPreview
import com.nordairemapper.ui.components.SectionLabel
import com.nordairemapper.ui.components.StatusChip
import com.nordairemapper.ui.components.StatusTone

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
                title = {
                    Column {
                        NordHeading("Overlay", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Floating menu slots",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Tap a slot to select it. Full layout controls are below.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Enable overlay",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            text = "When off, Show overlay actions will not open this menu",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = config.enabled, onCheckedChange = viewModel::setEnabled)
                }
            }

            SectionLabel("Live preview")
            OverlayPreview(config = config)

            SectionLabel("Slots")
            repeat(OverlayConfig.MAX_SLOTS) { index ->
                val action = config.slots.getOrNull(index) ?: RemapAction.None
                ActionCard(
                    title = "Slot ${index + 1}",
                    subtitle = action.displayName(),
                    icon = action.icon(),
                    onClick = { editingSlot = index },
                )
            }

            SectionLabel("Layout")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverlayLayoutStyle.entries.forEach { style ->
                    StatusChip(
                        label = style.name.replace('_', ' ').lowercase()
                            .replaceFirstChar { it.titlecase() },
                        tone = StatusTone.Active,
                        selected = config.layoutStyle == style,
                        showDot = false,
                        onClick = { viewModel.setLayoutStyle(style) },
                    )
                }
            }

            SectionLabel("Position")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverlayPosition.entries.forEach { position ->
                    StatusChip(
                        label = position.name.replace('_', ' ').lowercase()
                            .replaceFirstChar { it.titlecase() },
                        tone = StatusTone.Active,
                        selected = config.position == position,
                        showDot = false,
                        onClick = { viewModel.setPosition(position) },
                    )
                }
            }

            SectionLabel("Opacity ${(config.opacity * 100).toInt()}%")
            Slider(
                value = config.opacity,
                onValueChange = viewModel::setOpacity,
                valueRange = 0.3f..1f,
            )

            SectionLabel("Icon size")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverlayIconSize.entries.forEach { size ->
                    StatusChip(
                        label = size.name.lowercase().replaceFirstChar { it.titlecase() },
                        tone = StatusTone.Active,
                        selected = config.iconSize == size,
                        showDot = false,
                        onClick = { viewModel.setIconSize(size) },
                    )
                }
            }

            SectionLabel("Animation")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OverlayAnimation.entries.forEach { animation ->
                    StatusChip(
                        label = animation.name.lowercase().replaceFirstChar { it.titlecase() },
                        tone = StatusTone.Active,
                        selected = config.animation == animation,
                        showDot = false,
                        onClick = { viewModel.setAnimation(animation) },
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
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
