package com.nordairemapper.presentation.remap

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.domain.model.PressType
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.presentation.common.RemapActionCatalog
import com.nordairemapper.presentation.common.RemapActionCategory
import com.nordairemapper.presentation.common.RemapActionItem
import com.nordairemapper.presentation.common.conflictKey
import com.nordairemapper.presentation.common.displayDescription
import com.nordairemapper.presentation.common.displayName
import com.nordairemapper.presentation.common.icon
import com.nordairemapper.ui.components.NordGhostButton
import com.nordairemapper.ui.components.NordHeading
import com.nordairemapper.ui.components.NordPrimaryButton
import com.nordairemapper.ui.components.NordSurfaceCard
import com.nordairemapper.ui.components.SectionLabel

private enum class RemapSheet { None, AppPicker, UrlInput }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RemapScreen(
    onBack: () -> Unit,
    viewModel: RemapViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val apps by viewModel.installedApps.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    var sheet by remember { mutableStateOf(RemapSheet.None) }
    var query by remember { mutableStateOf("") }
    var categoryFilter by remember { mutableStateOf<RemapActionCategory?>(null) }
    val grouped = remember { RemapActionCatalog.grouped().toList() }
    val filtered = remember(query, grouped, categoryFilter) {
        val base = if (categoryFilter == null || query.isNotBlank()) {
            grouped
        } else {
            grouped.filter { it.first == categoryFilter }
        }
        if (query.isBlank()) {
            base
        } else {
            val q = query.trim().lowercase()
            base.mapNotNull { (category, actionItems) ->
                val match = actionItems.filter { item ->
                    matchesQuery(item, category, q)
                }
                if (match.isEmpty()) null else category to match
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbar.showSnackbar(
                when {
                    message.startsWith("Saved:") -> "Action saved"
                    else -> message
                },
            )
        }
    }

    // Searching across all categories — clear chip filter visually
    LaunchedEffect(query) {
        if (query.isNotBlank()) categoryFilter = null
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        NordHeading(
                            text = pressTitle(state.pressType),
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Text(
                            text = "Assign an action",
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
        snackbarHost = { SnackbarHost(snackbar) },
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                NordGhostButton(
                    text = "Try now",
                    onClick = viewModel::tryNow,
                    modifier = Modifier.weight(1f),
                )
                NordPrimaryButton(
                    text = "Done",
                    onClick = onBack,
                    modifier = Modifier.weight(1f),
                )
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            item {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    placeholder = { Text("Search actions or apps") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                )
            }

            item {
                CategoryChips(
                    selected = if (query.isNotBlank()) null else categoryFilter,
                    dimmed = query.isNotBlank(),
                    onSelect = { categoryFilter = it },
                )
            }

            if (state.hasConflict) {
                item {
                    NordSurfaceCard {
                        Text(
                            text = "Same action is also assigned to: " +
                                state.conflictWith.joinToString { it.label },
                            modifier = Modifier.padding(14.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            item {
                SectionLabel("Current")
                Text(
                    text = state.currentAction.displayName(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
            }

            filtered.forEach { (category, actionItems) ->
                item { SectionLabel(category.label) }
                items(actionItems, key = { catalogKey(it) }) { catalogItem ->
                    ActionPickRow(
                        item = catalogItem,
                        selected = isSelected(state.currentAction, catalogItem.action),
                        showCategory = query.isNotBlank(),
                        onClick = {
                            when {
                                catalogItem.action is RemapAction.LaunchApp ->
                                    sheet = RemapSheet.AppPicker
                                catalogItem.action is RemapAction.OpenUrl ->
                                    sheet = RemapSheet.UrlInput
                                else -> viewModel.setAction(catalogItem.action)
                            }
                        },
                    )
                }
            }
        }
    }

    when (sheet) {
        RemapSheet.AppPicker -> AppPickerSheet(
            apps = apps,
            onLoad = viewModel::loadInstalledApps,
            onSelect = { app ->
                viewModel.setAction(RemapAction.LaunchApp(app.packageName, app.label))
                sheet = RemapSheet.None
            },
            onDismiss = { sheet = RemapSheet.None },
        )
        RemapSheet.UrlInput -> UrlInputSheet(
            initialUrl = (state.currentAction as? RemapAction.OpenUrl)?.url.orEmpty(),
            onSave = { url ->
                viewModel.setAction(RemapAction.OpenUrl(url))
                sheet = RemapSheet.None
            },
            onDismiss = { sheet = RemapSheet.None },
        )
        RemapSheet.None -> Unit
    }
}

@Composable
private fun CategoryChips(
    selected: RemapActionCategory?,
    dimmed: Boolean,
    onSelect: (RemapActionCategory?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        RemapActionCategory.entries.forEach { category ->
            val on = selected == category
            val shape = RoundedCornerShape(999.dp)
            Text(
                text = category.label,
                modifier = Modifier
                    .clip(shape)
                    .background(
                        if (on) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surface,
                    )
                    .border(
                        BorderStroke(
                            1.dp,
                            if (on) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.outline,
                        ),
                        shape,
                    )
                    .clickable(enabled = !dimmed) {
                        onSelect(if (on) null else category)
                    }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    dimmed -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    on -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
                fontWeight = if (on) FontWeight.SemiBold else FontWeight.Medium,
            )
        }
    }
}

@Composable
private fun ActionPickRow(
    item: RemapActionItem,
    selected: Boolean,
    showCategory: Boolean,
    onClick: () -> Unit,
) {
    val action = item.action
    val title = when {
        action is RemapAction.LaunchApp -> "Launch app…"
        action is RemapAction.OpenUrl -> "Open URL / deep link…"
        else -> action.displayName()
    }
    val hint = when {
        action is RemapAction.LaunchApp -> "Pick any installed app"
        action is RemapAction.OpenUrl -> "Open a URL or deep link"
        else -> action.displayDescription()
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .then(
                if (selected) {
                    Modifier.border(
                        BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)),
                        MaterialTheme.shapes.medium,
                    )
                } else {
                    Modifier
                },
            )
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(42.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = MaterialTheme.shapes.small,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = action.icon(),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            if (showCategory) {
                Text(
                    text = item.category.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                ),
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = hint,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (selected) {
            Icon(
                Icons.Outlined.Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.primary,
            )
        }
    }
}

private fun pressTitle(pressType: PressType): String = when (pressType) {
    PressType.SINGLE -> "Single"
    PressType.DOUBLE -> "Double"
    PressType.LONG -> "Long"
}

private fun matchesQuery(
    item: RemapActionItem,
    category: RemapActionCategory,
    q: String,
): Boolean {
    val name = when {
        item.action is RemapAction.LaunchApp -> "launch app"
        item.action is RemapAction.OpenUrl -> "open url deep link"
        else -> item.action.displayName()
    }.lowercase()
    val hint = when {
        item.action is RemapAction.LaunchApp -> "pick any installed app"
        item.action is RemapAction.OpenUrl -> "open a url or deep link"
        else -> item.action.displayDescription()
    }.lowercase()
    val aliases = actionAliases(item.action)
    return name.contains(q) ||
        hint.contains(q) ||
        category.label.lowercase().contains(q) ||
        aliases.any { it.contains(q) } ||
        fuzzySubseq(q.replace(" ", ""), name.replace(" ", ""))
}

private fun actionAliases(action: RemapAction): List<String> = when (action) {
    RemapAction.ToggleFlashlight -> listOf("torch", "light", "led")
    RemapAction.ToggleDoNotDisturb -> listOf("dnd", "focus", "quiet")
    RemapAction.CycleRingerMode -> listOf("mute", "silent", "vibrate", "ringer")
    RemapAction.OpenNotificationShade -> listOf("notifications", "shade", "dropdown")
    RemapAction.OpenQuickSettings -> listOf("qs", "tiles", "panel")
    RemapAction.ShowOverlay -> listOf("chord", "floating", "menu")
    RemapAction.None -> listOf("disable", "off", "nothing")
    RemapAction.TakeScreenshot -> listOf("capture", "snap")
    RemapAction.OpenAssistant -> listOf("voice", "gemini", "google")
    is RemapAction.OpenCamera -> listOf("photo", "picture", "selfie")
    else -> emptyList()
}

/** Subsequence fuzzy: "flsh" matches "flashlight". */
private fun fuzzySubseq(query: String, text: String): Boolean {
    if (query.isEmpty()) return true
    var i = 0
    for (ch in text) {
        if (ch == query[i]) i += 1
        if (i == query.length) return true
    }
    return false
}

private fun catalogKey(item: RemapActionItem): String =
    "${item.category.name}:${item.action.conflictKey()}:${item.needsPicker}"

private fun isSelected(current: RemapAction, catalogAction: RemapAction): Boolean =
    when {
        catalogAction is RemapAction.LaunchApp -> current is RemapAction.LaunchApp
        catalogAction is RemapAction.OpenUrl -> current is RemapAction.OpenUrl
        else -> current.conflictKey() == catalogAction.conflictKey()
    }
