package com.nordairemapper.presentation.home

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalView
import android.view.HapticFeedbackConstants
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.domain.model.PressType
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.presentation.common.categoryAccent
import com.nordairemapper.presentation.common.relativeLastSeen
import com.nordairemapper.presentation.common.categoryFor
import com.nordairemapper.presentation.common.displayName
import com.nordairemapper.presentation.common.icon
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import com.nordairemapper.ui.components.ActionCard
import com.nordairemapper.ui.components.NordTopBarTitle
import com.nordairemapper.ui.components.NordPrimaryButton
import com.nordairemapper.ui.components.PhoneDiagram
import com.nordairemapper.ui.components.SectionLabel
import com.nordairemapper.ui.theme.StatusActive
import com.nordairemapper.ui.theme.StatusInactive
import com.nordairemapper.ui.theme.StatusWarning
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenRemap: (PressType) -> Unit,
    onOpenKeyLearning: () -> Unit,
    onOpenDeveloper: () -> Unit,
    onOpenEnableDetection: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenBackup: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(), // params kept for NavHost compatibility
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var demoPulse by remember { mutableStateOf(false) }

    fun flashKey() {
        demoPulse = true
        scope.launch {
            delay(700)
            demoPulse = false
        }
    }

    LaunchedEffect(Unit) {
        viewModel.plusKeyPulse.collect {
            demoPulse = true
            delay(700)
            demoPulse = false
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refreshRuntimeFlags()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    NordTopBarTitle(
                        title = "Plus Key",
                        subtitle = "Nord AI Remapper · Home",
                    )
                },
                actions = {
                    // Single settings icon — 36×36dp bordered box matching .ico in design
                    Box(
                        modifier = Modifier
                            .padding(end = 12.dp)
                            .size(36.dp)
                            .background(
                                MaterialTheme.colorScheme.surface,
                                RoundedCornerShape(10.dp),
                            )
                            .border(
                                1.dp,
                                MaterialTheme.colorScheme.outline,
                                RoundedCornerShape(10.dp),
                            )
                            .clickable(onClick = onOpenSettings),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            PhoneDiagram(
                // Remapping master on → Plus Key cyan; off → neutral side key
                highlightKey = state.serviceEnabled || demoPulse,
                edgeRipple = state.serviceEnabled || demoPulse,
                modifier = Modifier
                    // Match design `.nord5` scale; ~14% above 220dp compact pass
                    .fillMaxWidth()
                    .height(250.dp)
                    .align(Alignment.CenterHorizontally)
                    .padding(top = 2.dp, bottom = 12.dp)
                    .clickable { flashKey() },
            )

            val statusLabel = when {
                !state.accessibilityEnabled -> "Accessibility off"
                state.serviceEnabled -> "Service active"
                else -> "Remapping paused"
            }
            val statusDot = when {
                !state.accessibilityEnabled -> StatusInactive
                state.serviceEnabled -> StatusActive
                else -> StatusWarning
            }

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                border = BorderStroke(
                    1.dp,
                    if (state.serviceEnabled) {
                        MaterialTheme.colorScheme.primaryContainer
                    } else {
                        MaterialTheme.colorScheme.outline
                    },
                ),
                shape = MaterialTheme.shapes.medium,
                elevation = CardDefaults.cardElevation(
                    defaultElevation = if (state.serviceEnabled) 2.dp else 0.dp,
                ),
            ) {
                Column {
                    StatusRibbon(
                        label = statusLabel,
                        dotColor = statusDot,
                        pulse = state.serviceEnabled && state.accessibilityEnabled,
                        onClick = if (!state.accessibilityEnabled) {
                            { viewModel.openAccessibilitySettings() }
                        } else {
                            null
                        },
                        lastUsedLabel = "Last Used: ${relativeLastSeen(state.lastPlusKeySeenAtMs)}",
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                "Remapping",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            )
                            Text(
                                text = "Master switch for Plus Key actions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Switch(
                            checked = state.serviceEnabled,
                            onCheckedChange = { enabled ->
                                view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                                viewModel.setServiceEnabled(enabled)
                            },
                        )
                    }
                }
            }

            state.banner?.let { banner ->
                TroubleshootingBanner(
                    banner = banner,
                    onPrimary = {
                        when (banner.primaryAction) {
                            HomeBannerAction.OPEN_ACCESSIBILITY -> viewModel.openAccessibilitySettings()
                            HomeBannerAction.OPEN_KEY_LEARNING -> onOpenKeyLearning()
                            HomeBannerAction.OPEN_DEVELOPER -> onOpenDeveloper()
                            HomeBannerAction.OPEN_ENABLE_DETECTION -> onOpenEnableDetection()
                        }
                    },
                )
            }

            SectionLabel("Actions")
            PressType.entries.forEach { pressType ->
                val action = state.actions[pressType] ?: RemapAction.None
                val empty = action is RemapAction.None
                val accent = categoryAccent(categoryFor(action))
                ActionCard(
                    title = when (pressType) {
                        PressType.SINGLE -> "Single"
                        PressType.DOUBLE -> "Double"
                        PressType.LONG -> "Long"
                    },
                    subtitle = action.displayName(),
                    icon = action.icon(),
                    iconContainer = accent.container,
                    iconTint = accent.tint,
                    badge = when (pressType) {
                        PressType.SINGLE -> "1×"
                        PressType.DOUBLE -> "2×"
                        PressType.LONG -> "L"
                    },
                    empty = empty,
                    showConflict = pressType in state.conflictPressTypes,
                    onClick = { onOpenRemap(pressType) },
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun StatusRibbon(
    label: String,
    dotColor: Color,
    pulse: Boolean,
    onClick: (() -> Unit)?,
    lastUsedLabel: String,
) {
    val pulseAlpha by rememberInfiniteTransition(label = "statusPulse").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "pulseAlpha",
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
            .then(
                if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier,
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.weight(1f, fill = false),
        ) {
            Box(
                modifier = Modifier
                    .size(9.dp)
                    .alpha(if (pulse) pulseAlpha else 1f)
                    .background(dotColor, CircleShape),
            )
            Text(
                text = label,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = lastUsedLabel,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
        )
    }
}

@Composable
private fun TroubleshootingBanner(
    banner: HomeBanner,
    onPrimary: () -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        border = BorderStroke(1.dp, StatusWarning.copy(alpha = 0.35f)),
        shape = MaterialTheme.shapes.medium,
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(
                banner.title,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                color = StatusWarning,
            )
            Text(
                text = banner.body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            NordPrimaryButton(text = banner.primaryLabel, onClick = onPrimary)
        }
    }
}
