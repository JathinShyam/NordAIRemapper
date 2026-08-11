package com.nordairemapper.presentation.remap

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.presentation.common.RemapActionCatalog
import com.nordairemapper.presentation.common.RemapActionItem
import com.nordairemapper.presentation.common.conflictKey
import com.nordairemapper.presentation.common.displayDescription
import com.nordairemapper.presentation.common.displayName
import com.nordairemapper.presentation.common.icon

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
    val grouped = remember { RemapActionCatalog.grouped() }

    LaunchedEffect(Unit) {
        viewModel.events.collect { message ->
            snackbar.showSnackbar(
                when {
                    message.startsWith("Saved:") -> "Action saved"
                    else -> message
                }
            )
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.pressType.label) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbar) },
        bottomBar = {
            Button(
                onClick = viewModel::tryNow,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
            ) {
                Text("Try this action now")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (state.hasConflict) {
                item {
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        ),
                    ) {
                        Text(
                            text = "Same action is also assigned to: " +
                                state.conflictWith.joinToString { it.label },
                            modifier = Modifier.padding(16.dp),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }

            item {
                Text(
                    text = "Current: ${state.currentAction.displayName()}",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(vertical = 4.dp),
                )
            }

            grouped.forEach { (category, items) ->
                item {
                    Text(
                        text = category.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp),
                    )
                }
                items(items, key = { catalogKey(it) }) { item ->
                    ActionPickRow(
                        item = item,
                        selected = isSelected(state.currentAction, item.action),
                        onClick = {
                            when {
                                item.action is RemapAction.LaunchApp -> sheet = RemapSheet.AppPicker
                                item.action is RemapAction.OpenUrl -> sheet = RemapSheet.UrlInput
                                else -> viewModel.setAction(item.action)
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
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(action.icon(), contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = when {
                        action is RemapAction.LaunchApp -> "Launch app…"
                        action is RemapAction.OpenUrl -> "Open URL / deep link…"
                        else -> action.displayName()
                    },
                    style = MaterialTheme.typography.titleMedium,
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
}

private fun catalogKey(item: RemapActionItem): String =
    "${item.category.name}:${item.action.conflictKey()}:${item.needsPicker}"

private fun isSelected(current: RemapAction, catalogAction: RemapAction): Boolean =
    when {
        catalogAction is RemapAction.LaunchApp -> current is RemapAction.LaunchApp
        catalogAction is RemapAction.OpenUrl -> current is RemapAction.OpenUrl
        else -> current.conflictKey() == catalogAction.conflictKey()
    }
