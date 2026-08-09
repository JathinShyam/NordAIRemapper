package com.nordairemapper.presentation.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Temporary Home Screen — the phone diagram and press-type cards land in
 * Phase 6. For now this is the entry point to the key-learning debug flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenKeyLearning: () -> Unit,
) {
    Scaffold(
        topBar = { TopAppBar(title = { Text("Nord AI Remapper") }) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "First step: confirm your Plus Key's identity on this device.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onOpenKeyLearning, modifier = Modifier.fillMaxWidth()) {
                Text("Key setup (debug)")
            }
        }
    }
}
