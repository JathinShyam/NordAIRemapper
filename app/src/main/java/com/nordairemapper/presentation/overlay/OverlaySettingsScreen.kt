package com.nordairemapper.presentation.overlay

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
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
import com.nordairemapper.presentation.common.RemapActionItem
import com.nordairemapper.presentation.common.categoryAccent
import com.nordairemapper.presentation.common.categoryFor
import com.nordairemapper.presentation.common.displayDescription
import com.nordairemapper.presentation.common.displayName
import com.nordairemapper.presentation.common.icon
import com.nordairemapper.presentation.common.rememberAppIcon
import com.nordairemapper.presentation.remap.AppPickerSheet
import com.nordairemapper.presentation.remap.UrlInputSheet
import com.nordairemapper.ui.components.ActionCard
import com.nordairemapper.ui.components.NordTopBarHeading
import com.nordairemapper.ui.components.NordTopBarTitle
import com.nordairemapper.ui.components.OverlayPreview
import com.nordairemapper.ui.components.SectionLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OverlaySettingsScreen(
    onBack: () -> Unit,
    viewModel: OverlaySettingsViewModel = hiltViewModel(),
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    val loadingApps by viewModel.loadingApps.collectAsStateWithLifecycle()
    // Saveable: rotation must not close an open slot editor mid-assignment.
    var editingSlot by rememberSaveable { mutableStateOf<Int?>(null) }
    var appPickerSlot by rememberSaveable { mutableStateOf<Int?>(null) }
    var urlSheetSlot by rememberSaveable { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    NordTopBarTitle(
                        title = "Floating Menu",
                        subtitle = "Quick-action slots",
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
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {

            // ── Enable toggle ─────────────────────────────────────────────
            PrefCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .toggleable(
                            value = config.enabled,
                            role = Role.Switch,
                            onValueChange = viewModel::setEnabled,
                        )
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Enable floating menu",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "\"Show floating menu\" actions will not open the menu when off",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = config.enabled, onCheckedChange = null)
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
                    text = "Tap a slot to add an action — at least one is needed for the floating menu to show",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 4.dp),
                )
            } else {
                Text(
                    text = "Tap a slot to assign an action. Up to 6 slots are shown in the floating menu.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            repeat(OverlayConfig.MAX_SLOTS) { index ->
                val action = config.slots.getOrNull(index) ?: RemapAction.None
                val empty = action is RemapAction.None
                val appIcon = rememberAppIcon((action as? RemapAction.LaunchApp)?.packageName)
                ActionCard(
                    title = "Slot ${index + 1}",
                    subtitle = action.displayName(),
                    icon = if (empty) null else action.icon(),
                    appIcon = appIcon,
                    iconContainer = if (empty) null else categoryAccent(categoryFor(action)).container,
                    iconTint = if (empty) null else categoryAccent(categoryFor(action)).tint,
                    badge = (index + 1).toString(),
                    empty = empty,
                    onClick = { editingSlot = index },
                )
            }

            // ── Layout ────────────────────────────────────────────────────
            SectionLabel("Layout style")
            PrefCard {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp)) {
                    Text(
                        text = when (config.layoutStyle) {
                            OverlayLayoutStyle.GRID -> "Grid"
                            OverlayLayoutStyle.PILL_BAR -> "Pill bar"
                        },
                        style = MaterialTheme.typography.titleSmall,
                    )
                    Text(
                        text = when (config.layoutStyle) {
                            OverlayLayoutStyle.GRID ->
                                "3×2 square tile panel — Top / Middle / Bottom"
                            OverlayLayoutStyle.PILL_BAR ->
                                "Vertical strip (Left/Right) or horizontal scroll row (Bottom)"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    SegRow {
                        OverlayLayoutStyle.entries.forEach { style ->
                            val selected = config.layoutStyle == style
                            val label = when (style) {
                                OverlayLayoutStyle.GRID -> "Grid"
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
                        text = when (config.layoutStyle) {
                            OverlayLayoutStyle.GRID -> "Where the panel appears vertically"
                            OverlayLayoutStyle.PILL_BAR ->
                                "Left / Right = vertical strip · Bottom = horizontal scroll row"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 10.dp),
                    )
                    val positions = when (config.layoutStyle) {
                        OverlayLayoutStyle.GRID -> listOf(
                            OverlayPosition.TOP,
                            OverlayPosition.MIDDLE,
                            OverlayPosition.BOTTOM,
                        )
                        OverlayLayoutStyle.PILL_BAR -> listOf(
                            OverlayPosition.LEFT,
                            OverlayPosition.RIGHT,
                            OverlayPosition.BOTTOM,
                        )
                    }
                    SegRow {
                        positions.forEach { pos ->
                            val selected = config.position == pos
                            val label = when (pos) {
                                OverlayPosition.TOP -> "Top"
                                OverlayPosition.MIDDLE -> "Middle"
                                OverlayPosition.BOTTOM -> "Bottom"
                                OverlayPosition.LEFT -> "Left"
                                OverlayPosition.RIGHT -> "Right"
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
                        text = "How tiles animate in when the floating menu opens",
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
                        modifier = Modifier.semantics {
                            stateDescription = "${(config.opacity * 100).toInt()}%"
                        },
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
            onPickLaunchApp = {
                appPickerSlot = slot
                editingSlot = null
            },
            onPickUrl = {
                urlSheetSlot = slot
                editingSlot = null
            },
        )
    }

    appPickerSlot?.let { idx ->
        AppPickerSheet(
            apps = apps,
            isLoading = loadingApps,
            onLoad = viewModel::loadInstalledApps,
            onSelect = { app ->
                viewModel.setSlot(idx, RemapAction.LaunchApp(app.packageName, app.label))
                appPickerSlot = null
            },
            onDismiss = { appPickerSlot = null },
        )
    }

    urlSheetSlot?.let { idx ->
        UrlInputSheet(
            initialUrl = (config.slots.getOrNull(idx) as? RemapAction.OpenUrl)?.url.orEmpty(),
            onSave = { url ->
                viewModel.setSlot(idx, RemapAction.OpenUrl(url))
                urlSheetSlot = null
            },
            onDismiss = { urlSheetSlot = null },
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
private fun SegRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        content = content,
    )
}

@Composable
private fun RowScope.SegButton(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        // Equal-width segments survive large font scales without clipping.
        modifier = modifier.weight(1f),
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
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.labelMedium, maxLines = 1)
    }
}

// ─── Action catalog sheet ─────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ActionCatalogSheet(
    onDismiss: () -> Unit,
    onSelect: (RemapAction) -> Unit,
    onPickLaunchApp: () -> Unit,
    onPickUrl: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val grouped = remember {
        RemapActionCatalog.grouped()
            .filterValues { it.isNotEmpty() }
            .toList()
    }
    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            NordTopBarHeading(
                text = "Choose action",
                modifier = Modifier.padding(bottom = 8.dp),
            )
            grouped.forEach { (category, items) ->
                SectionLabel(category.label)
                items.forEach { item ->
                    OverlayActionPickRow(
                        item = item,
                        onClick = when (val action = item.action) {
                            is RemapAction.LaunchApp -> onPickLaunchApp
                            is RemapAction.OpenUrl -> onPickUrl
                            else -> ({ onSelect(action) })
                        },
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun OverlayActionPickRow(
    item: RemapActionItem,
    onClick: () -> Unit,
) {
    val action = item.action
    val accent = categoryAccent(item.category)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(accent.container, MaterialTheme.shapes.small),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon(),
                contentDescription = null,
                tint = accent.tint,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = action.displayName(),
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = action.displayDescription(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
