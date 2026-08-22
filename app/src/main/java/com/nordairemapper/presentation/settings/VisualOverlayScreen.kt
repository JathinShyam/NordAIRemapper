package com.nordairemapper.presentation.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.domain.model.AppSettings
import com.nordairemapper.domain.model.OverlayVisualStyle
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.presentation.overlay.OverlaySettingsViewModel
import com.nordairemapper.service.ActionFeedbackOverlayService
import com.nordairemapper.ui.components.NordGhostButton
import com.nordairemapper.ui.components.NordHeading
import com.nordairemapper.ui.components.OverlayPreview

private val AccentPresets = listOf(
    0xFF0AC6FF.toInt(),
    0xFFFFD60A.toInt(),
    0xFF2ECC71.toInt(),
    0xFFFF4D4D.toInt(),
    0xFF9B59B6.toInt(),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisualOverlayScreen(
    onBack: () -> Unit,
    viewModel: OverlaySettingsViewModel = hiltViewModel(),
    settingsViewModel: SettingsViewModel = hiltViewModel(),
) {
    val config by viewModel.config.collectAsStateWithLifecycle()
    val settings by settingsViewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        NordHeading("Visual Overlay", style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "Action popup when a remap fires",
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
        ) {
            Text(
                text = "Brief popup when a remap fires. Needs Display over other apps.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            SettingsGroupCard {
                SettingsToggleRow(
                    title = "Enable visual overlay",
                    subtitle = "Show a brief popup when an action triggers",
                    checked = settings.visualOverlayEnabled,
                    onCheckedChange = settingsViewModel::setVisualOverlayEnabled,
                    icon = Icons.Outlined.Visibility,
                )
            }

            QuietSectionLabel("Style")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                StylePreviewCard(
                    title = "OnePlus",
                    selected = config.visualStyle == OverlayVisualStyle.ONEPLUS,
                    accent = Color(config.accentColorArgb),
                    darkPreview = true,
                    onClick = { viewModel.setVisualStyle(OverlayVisualStyle.ONEPLUS) },
                    modifier = Modifier.weight(1f),
                )
                StylePreviewCard(
                    title = "Stock",
                    selected = config.visualStyle == OverlayVisualStyle.STOCK,
                    accent = Color(0xFF9E9E9E),
                    darkPreview = false,
                    onClick = { viewModel.setVisualStyle(OverlayVisualStyle.STOCK) },
                    modifier = Modifier.weight(1f),
                )
            }

            QuietSectionLabel("Appearance")
            SettingsGroupCard {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(Icons.Outlined.Palette, contentDescription = null)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Color",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            "Choose the accent color",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .background(Color(config.accentColorArgb)),
                    )
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AccentPresets.forEach { argb ->
                        val selected = config.accentColorArgb == argb
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color(argb))
                                .then(
                                    if (selected) {
                                        Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, CircleShape)
                                    } else {
                                        Modifier
                                    },
                                )
                                .clickable { viewModel.setAccentColor(argb) },
                        )
                    }
                }
                SettingsToggleRow(
                    title = "Glow effects",
                    subtitle = "Edge glow, shadow, and line",
                    checked = config.glowEffects,
                    onCheckedChange = viewModel::setGlowEffects,
                    icon = Icons.Outlined.BlurOn,
                )
            }

            QuietSectionLabel("Live preview")
            OverlayPreview(
                config = config,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
            )

            QuietSectionLabel(
                "Hold duration · ${String.format("%.1fs", config.holdDurationMs / 1000f)}",
            )
            SettingsGroupCard {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                    Text(
                        text = "How long the action popup stays visible",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    // Drag locally; persist once on release.
                    var pendingHoldMs by remember(config.holdDurationMs) {
                        mutableStateOf(config.holdDurationMs.toFloat())
                    }
                    Slider(
                        value = pendingHoldMs,
                        onValueChange = { pendingHoldMs = it },
                        onValueChangeFinished = {
                            viewModel.setHoldDurationMs(pendingHoldMs.toLong())
                        },
                        valueRange = AppSettings.HOLD_DURATION_RANGE_MS.first.toFloat()..
                            AppSettings.HOLD_DURATION_RANGE_MS.last.toFloat(),
                        steps = 16,
                    )
                }
            }

            NordGhostButton(
                text = "Preview popup",
                onClick = {
                    ActionFeedbackOverlayService.show(context, RemapAction.ToggleFlashlight)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
