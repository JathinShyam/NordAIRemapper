package com.nordairemapper.presentation.settings

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BlurOn
import androidx.compose.material.icons.outlined.FlashlightOn
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.domain.model.AppSettings
import com.nordairemapper.domain.model.OverlayVisualStyle
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.presentation.overlay.OverlaySettingsViewModel
import com.nordairemapper.service.ActionFeedbackOverlayService
import com.nordairemapper.ui.components.NordGhostButton
import com.nordairemapper.ui.components.NordPrimaryButton
import com.nordairemapper.ui.components.NordTopBarTitle
import com.nordairemapper.ui.components.StatusChip
import com.nordairemapper.ui.components.StatusTone
import com.nordairemapper.ui.components.VisualActionPopupPill

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
    val lifecycleOwner = LocalLifecycleOwner.current
    var overlayGrantedNow by remember {
        mutableStateOf(Settings.canDrawOverlays(context))
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                overlayGrantedNow = Settings.canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    NordTopBarTitle(
                        title = "Visual Overlay",
                        subtitle = "Action popup near Plus Key",
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
        ) {
            Text(
                text = "Shows which action fired — a brief icon popup on the Plus Key edge. Not the floating menu (see Floating Menu in Settings).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            StatusChip(
                label = if (overlayGrantedNow) "Display over apps granted" else "Display over apps needed",
                tone = if (overlayGrantedNow) StatusTone.Active else StatusTone.Warning,
                modifier = Modifier.padding(bottom = 12.dp),
            )

            if (!overlayGrantedNow) {
                SettingsGroupCard {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Text(
                            text = "Visual Overlay needs Display over other apps to draw on screen.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        NordPrimaryButton(
                            text = "Open Display over apps",
                            onClick = {
                                context.startActivity(
                                    Intent(
                                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                        android.net.Uri.parse("package:${context.packageName}"),
                                    ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            SettingsGroupCard {
                SettingsToggleRow(
                    title = "Enable visual overlay",
                    subtitle = "Popup when Single, Double, or Long press fires",
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
                VisualStyleCard(
                    title = "OnePlus",
                    selected = config.visualStyle == OverlayVisualStyle.ONEPLUS,
                    accent = Color(config.accentColorArgb),
                    visualStyle = OverlayVisualStyle.ONEPLUS,
                    glowEffects = config.glowEffects,
                    onClick = { viewModel.setVisualStyle(OverlayVisualStyle.ONEPLUS) },
                    modifier = Modifier.weight(1f),
                )
                VisualStyleCard(
                    title = "Stock",
                    selected = config.visualStyle == OverlayVisualStyle.STOCK,
                    accent = Color(0xFF9E9E9E),
                    visualStyle = OverlayVisualStyle.STOCK,
                    glowEffects = false,
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
                            "Accent color",
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        )
                        Text(
                            "Border, icon, and edge glow",
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
                    title = "Glow effect",
                    subtitle = "Accent line and bloom on the Plus Key edge",
                    checked = config.glowEffects,
                    onCheckedChange = viewModel::setGlowEffects,
                    icon = Icons.Outlined.BlurOn,
                )
            }

            QuietSectionLabel(
                "Hold duration · ${String.format("%.1fs", config.holdDurationMs / 1000f)}",
            )
            SettingsGroupCard {
                Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 14.dp)) {
                    Text(
                        text = "How long the popup stays on screen",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
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
                text = "Preview on screen",
                onClick = {
                    overlayGrantedNow = Settings.canDrawOverlays(context)
                    if (!overlayGrantedNow) {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                android.net.Uri.parse("package:${context.packageName}"),
                            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    } else {
                        ActionFeedbackOverlayService.show(context, RemapAction.ToggleFlashlight)
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun VisualStyleCard(
    title: String,
    selected: Boolean,
    accent: Color,
    visualStyle: OverlayVisualStyle,
    glowEffects: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(100.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0A0A0A))
                .border(
                    BorderStroke(
                        width = if (selected) 2.dp else 1.dp,
                        color = if (selected) accent else MaterialTheme.colorScheme.outline,
                    ),
                    RoundedCornerShape(14.dp),
                ),
            contentAlignment = Alignment.CenterStart,
        ) {
            VisualActionPopupPill(
                icon = Icons.Outlined.FlashlightOn,
                accent = accent,
                visualStyle = visualStyle,
                glowEffects = glowEffects && visualStyle == OverlayVisualStyle.ONEPLUS,
                pillWidth = 40.dp,
                pillHeight = 52.dp,
                iconSize = 22.dp,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
            color = if (selected) MaterialTheme.colorScheme.onSurface
            else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
