package com.nordairemapper.presentation.remap

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.presentation.common.RemapActionCatalog
import com.nordairemapper.presentation.common.RemapActionItem
import com.nordairemapper.presentation.common.conflictKey
import com.nordairemapper.presentation.common.displayDescription
import com.nordairemapper.presentation.common.displayName
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
    val grouped = remember { RemapActionCatalog.grouped().toList() }
    val filtered = remember(query, grouped) {
        if (query.isBlank()) {
            grouped
        } else {
            val q = query.trim().lowercase()
            grouped.mapNotNull { (category, actionItems) ->
                val match = actionItems.filter {
                    it.action.displayName().lowercase().contains(q) ||
                        it.action.displayDescription().lowercase().contains(q) ||
                        category.label.lowercase().contains(q)
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    NordHeading(
                        text = state.pressType.label,
                        style = MaterialTheme.typography.titleLarge,
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
private fun ActionPickRow(
    item: RemapActionItem,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val action = item.action
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(
                text = when {
                    action is RemapAction.LaunchApp -> "Launch app…"
                    action is RemapAction.OpenUrl -> "Open URL / deep link…"
                    else -> action.displayName()
                },
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                ),
                color = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            Text(
                text = when {
                    action is RemapAction.LaunchApp -> "Pick any installed app"
                    action is RemapAction.OpenUrl -> "Open a URL or deep link"
                    else -> action.displayDescription()
                },
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

private fun catalogKey(item: RemapActionItem): String =
    "${item.category.name}:${item.action.conflictKey()}:${item.needsPicker}"

private fun isSelected(current: RemapAction, catalogAction: RemapAction): Boolean =
    when {
        catalogAction is RemapAction.LaunchApp -> current is RemapAction.LaunchApp
        catalogAction is RemapAction.OpenUrl -> current is RemapAction.OpenUrl
        else -> current.conflictKey() == catalogAction.conflictKey()
    }
