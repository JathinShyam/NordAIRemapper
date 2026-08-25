package com.nordairemapper.presentation.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.ui.components.NordGhostButton
import com.nordairemapper.ui.components.NordTopBarTitle
import com.nordairemapper.ui.components.NordPrimaryButton
import com.nordairemapper.ui.components.NordSurfaceCard
import com.nordairemapper.ui.components.SectionLabel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val snapshots by viewModel.snapshots.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    // Saveable: a rotation must never drop a destructive-confirm dialog.
    var snapshotName by rememberSaveable { mutableStateOf("") }
    var pendingRestoreId by rememberSaveable { mutableStateOf<Long?>(null) }
    var localExpanded by rememberSaveable { mutableStateOf(false) }
    // Destructive actions confirm first; import overwrites the whole setup.
    var pendingImportUri by rememberSaveable { mutableStateOf<Uri?>(null) }
    var pendingDeleteId by rememberSaveable { mutableStateOf<Long?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri -> uri?.let(viewModel::exportTo) }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri -> uri?.let { pendingImportUri = it } }

    LaunchedEffect(Unit) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    NordTopBarTitle(
                        title = "Backup & Restore",
                        subtitle = "Export and import",
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            SectionLabel("Transfer")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                NordPrimaryButton(
                    text = "Export",
                    onClick = {
                        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
                        exportLauncher.launch("keyforge-$stamp.json")
                    },
                    modifier = Modifier.weight(1f),
                )
                NordGhostButton(
                    text = "Import",
                    onClick = { importLauncher.launch(arrayOf("application/json", "*/*")) },
                    modifier = Modifier.weight(1f),
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        onClickLabel = if (localExpanded) "Collapse local snapshots" else "Expand local snapshots",
                    ) { localExpanded = !localExpanded }
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Local snapshot",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    imageVector = if (localExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                    contentDescription = if (localExpanded) "Collapse" else "Expand",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (localExpanded) {
                Text(
                    text = "Optional on-device copies — most people only need Export / Import.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedTextField(
                    value = snapshotName,
                    onValueChange = { snapshotName = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Snapshot name") },
                    singleLine = true,
                    shape = MaterialTheme.shapes.small,
                )
                NordPrimaryButton(
                    text = "Save local snapshot",
                    onClick = {
                        viewModel.saveSnapshot(snapshotName)
                        snapshotName = ""
                    },
                )

                if (snapshots.isEmpty()) {
                    Text(
                        text = "No snapshots yet — save one above.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    SectionLabel("Saved")
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(snapshots, key = { it.id }) { snapshot ->
                            NordSurfaceCard(onClick = { pendingRestoreId = snapshot.id }) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            snapshot.name,
                                            style = MaterialTheme.typography.titleSmall.copy(
                                                fontWeight = FontWeight.SemiBold,
                                            ),
                                        )
                                        Text(
                                            text = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US)
                                                .format(Date(snapshot.createdAtEpochMs)),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    IconButton(onClick = { pendingDeleteId = snapshot.id }) {
                                        Icon(Icons.Outlined.Delete, contentDescription = "Delete")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    pendingImportUri?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text("Import backup?") },
            text = { Text("This replaces your current remaps, floating menu, and related settings with the file's contents.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.importFrom(uri)
                        pendingImportUri = null
                    },
                ) { Text("Import") }
            },
            dismissButton = {
                TextButton(onClick = { pendingImportUri = null }) { Text("Cancel") }
            },
        )
    }

    pendingDeleteId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingDeleteId = null },
            title = { Text("Delete snapshot?") },
            text = { Text("This permanently removes the local snapshot.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteSnapshot(id)
                        pendingDeleteId = null
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeleteId = null }) { Text("Cancel") }
            },
        )
    }

    pendingRestoreId?.let { id ->
        AlertDialog(
            onDismissRequest = { pendingRestoreId = null },
            title = { Text("Restore snapshot?") },
            text = { Text("This replaces your current remap, floating menu, and related settings.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.restoreSnapshot(id)
                        pendingRestoreId = null
                    },
                ) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestoreId = null }) { Text("Cancel") }
            },
        )
    }
}
