package com.nordairemapper.presentation.settings

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.service.AppPermissions
import com.nordairemapper.ui.components.NordTopBarTitle
import com.nordairemapper.ui.components.SectionLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PermissionsScreen(
    onBack: () -> Unit,
    onOpenEnableDetection: () -> Unit,
    viewModel: PermissionsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refresh() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    NordTopBarTitle(
                        title = "Permissions",
                        subtitle = "Access And Special Grants",
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
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
        ) {
            PermissionSection(
                label = "Core",
                items = uiState.items.filter { it.section == AppPermissions.Section.CORE },
                onItemClick = { viewModel.onPermissionAction(it.id, onOpenEnableDetection) },
            )

            PermissionSection(
                label = "Overlays",
                items = uiState.items.filter { it.section == AppPermissions.Section.OVERLAYS },
                onItemClick = { viewModel.onPermissionAction(it.id, onOpenEnableDetection) },
            )

            PermissionSection(
                label = "Reliability",
                items = uiState.items.filter { it.section == AppPermissions.Section.RELIABILITY },
                onItemClick = { item ->
                    if (item.id == AppPermissions.Id.NOTIFICATIONS &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        !item.isOk
                    ) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.onPermissionAction(item.id, onOpenEnableDetection)
                    }
                },
            )

            PermissionSection(
                label = "Advanced",
                items = uiState.items.filter { it.section == AppPermissions.Section.ADVANCED },
                onItemClick = { viewModel.onPermissionAction(it.id, onOpenEnableDetection) },
            )

            Text(
                text = "Tap A Row To Open The Matching Settings Screen Or Unlock Flow.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 8.dp),
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun PermissionSection(
    label: String,
    items: List<AppPermissions.Item>,
    onItemClick: (AppPermissions.Item) -> Unit,
) {
    if (items.isEmpty()) return
    SectionLabel(label)
    SettingsHubGroup {
        items.forEachIndexed { index, item ->
            PermissionStatusRow(
                title = item.title,
                subtitle = item.subtitle,
                statusLabel = item.statusLabel,
                tone = if (item.isOk) SettingsStatusTone.Ok else SettingsStatusTone.Warn,
                onClick = { onItemClick(item) },
            )
            if (index < items.lastIndex) {
                SettingsHubDivider()
            }
        }
    }
}
