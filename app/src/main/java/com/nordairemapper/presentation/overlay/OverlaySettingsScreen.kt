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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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

            // ── Enable toggle ─────────────────────────────────────────────
            PrefCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Enable overlay",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "Show overlay actions will not open this menu when off",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = config.enabled, onCheckedChange = viewModel::setEnabled)
                }
            }

            // ── Live preview ──────────────────────────────────────────────
            SectionLabel("Live preview")
            OverlayPreview(config = config)

            // ── Slots ─────────────────────────────────────────────────────
            SectionLabel("Slots")
            val allSlotsEmpty = config.slots.none { it !is RemapAction.None }
            if (allSlotsEmpty) {
                Text(
                    text = "Tap a slot to add an action — at least one is needed for the overlay to show",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            } else {
                Text(
                    text = "Tap a slot to assign an action. Up to 6 slots are shown in the overlay.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            repeat(OverlayConfig.MAX_SLOTS) { index ->
                val action = config.slots.getOrNull(index) ?: RemapAction.None
                val empty = action is RemapAction.None
                ActionCard(
                    title = "Slot ${index + 1}",
                    subtitle = action.displayName(),
                    icon = if (empty) null else action.icon(),
                    empty = empty,
                    onClick = { editingSlot = index },
                )
            }

            // ── Layout ────────────────────────────────────────────────────
            SectionLabel("Layout style")
            PrefCard {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        text = "Grid",
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = "3×2 square tile grid — the default design panel",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    SegRow {
                        OverlayLayoutStyle.entries.forEach { style ->
                            val selected = config.layoutStyle == style
                            val label = when (style) {
                                OverlayLayoutStyle.RADIAL   -> "Grid"
                                OverlayLayoutStyle.PILL_BAR -> "Pill bar"
                            }
                            SegButton(label = label, selected = selected) {
                                viewModel.setLayoutStyle(style)
                            }
                        }
                    }
                }
            }

            // ── Position ──────────────────────────────────────────────────
            SectionLabel("Position")
            PrefCard {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        text = "Where the panel appears on screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    SegRow {
                        OverlayPosition.entries.forEach { pos ->
                            val selected = config.position == pos
                            val label = when (pos) {
                                OverlayPosition.BOTTOM_CENTER -> "Bottom"
                                OverlayPosition.LEFT_EDGE     -> "Left"
                                OverlayPosition.RIGHT_EDGE    -> "Right"
                            }
                            SegButton(label = label, selected = selected) {
                                viewModel.setPosition(pos)
                            }
                        }
                    }
                }
            }

            // ── Icon size ─────────────────────────────────────────────────
            SectionLabel("Icon size")
            PrefCard {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        text = "Size of icons inside each tile",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    SegRow {
                        OverlayIconSize.entries.forEach { size ->
                            val selected = config.iconSize == size
                            val label = size.name.lowercase().replaceFirstChar { it.titlecase() }
                            SegButton(label = label, selected = selected) {
                                viewModel.setIconSize(size)
                            }
                        }
                    }
                }
            }

            // ── Animation ─────────────────────────────────────────────────
            SectionLabel("Entry animation")
            PrefCard {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        text = "How tiles animate in when the overlay opens",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    SegRow {
                        OverlayAnimation.entries.forEach { anim ->
                            val selected = config.animation == anim
                            val label = anim.name.lowercase().replaceFirstChar { it.titlecase() }
                            SegButton(label = label, selected = selected) {
                                viewModel.setAnimation(anim)
                            }
                        }
                    }
                }
            }

            // ── Opacity ───────────────────────────────────────────────────
            SectionLabel("Opacity · ${(config.opacity * 100).toInt()}%")
            PrefCard {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        text = "Panel background transparency",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 4.dp),
                    )
                    Slider(
                        value = config.opacity,
                        onValueChange = viewModel::setOpacity,
                        valueRange = 0.3f..1f,
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

// ─── Reusable primitives ──────────────────────────────────────────────────

@Composable
private fun PrefCard(content: @Composable () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = MaterialTheme.shapes.medium,
        modifier = Modifier.fillMaxWidth(),
    ) {
        content()
    }
}

@Composable
private fun SegRow(content: @Composable () -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) { content() }
}

@Composable
private fun SegButton(label: String, selected: Boolean, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primary
                             else MaterialTheme.colorScheme.surface,
            contentColor = if (selected) MaterialTheme.colorScheme.onPrimary
                           else MaterialTheme.colorScheme.onSurfaceVariant,
        ),
        border = BorderStroke(
            1.dp,
            if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
        ),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 14.dp,
            vertical = 6.dp,
        ),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

// ─── Action catalog sheet ─────────────────────────────────────────────────

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
