package com.nordairemapper.presentation.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.ui.components.StatusChip
import com.nordairemapper.ui.components.StatusTone
@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    var page by remember { mutableIntStateOf(0) }
    val permissions by viewModel.permissions.collectAsStateWithLifecycle()
    val lifecycleOwner = LocalLifecycleOwner.current

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

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        when (page) {
            0 -> StepContent(
                icon = Icons.Outlined.Smartphone,
                title = "Nord AI Remapper",
                body = "Remap the Nord 5 Plus Key to custom single, double, and long-press actions.",
                primaryLabel = "Get started",
                onPrimary = { page = 1 },
            )
            1 -> StepContent(
                icon = Icons.Outlined.Settings,
                title = "Accessibility service",
                body = "Required to detect the Plus Key and run system actions like screenshot, lock, and recents. The service only observes hardware keys — not your screen content.",
                status = if (permissions.accessibilityEnabled) "Enabled" else "Not enabled yet",
                primaryLabel = if (permissions.accessibilityEnabled) "Continue" else "Open Accessibility settings",
                onPrimary = {
                    if (permissions.accessibilityEnabled) page = 2
                    else viewModel.openAccessibilitySettings()
                },
                secondaryLabel = if (!permissions.accessibilityEnabled) "I've enabled it — Recheck" else null,
                onSecondary = viewModel::refresh,
            )
            2 -> StepContent(
                icon = Icons.Outlined.Layers,
                title = "Display over other apps",
                body = "Needed for the optional floating overlay menu. You can still use single-action remaps without it.",
                status = if (permissions.overlayGranted) "Granted" else "Not granted yet",
                primaryLabel = if (permissions.overlayGranted) "Continue" else "Open overlay settings",
                onPrimary = {
                    if (permissions.overlayGranted) page = 3
                    else viewModel.openOverlaySettings()
                },
                secondaryLabel = if (!permissions.overlayGranted) "I've enabled it — Recheck" else null,
                onSecondary = viewModel::refresh,
                tertiaryLabel = "Skip for now",
                onTertiary = { page = 3 },
            )
            3 -> StepContent(
                icon = Icons.Outlined.Notifications,
                title = "Notifications & battery",
                body = "Notifications keep detection alive in the background. Battery exemption reduces the chance OxygenOS kills the watcher.",
                status = buildString {
                    append(if (permissions.notificationsGranted) "Notifications OK" else "Notifications needed")
                    append(" · ")
                    append(if (permissions.batteryExempt) "Battery exempt" else "Battery not exempt")
                },
                primaryLabel = when {
                    !permissions.notificationsGranted -> "Allow notifications"
                    !permissions.batteryExempt -> "Exempt from battery optimization"
                    else -> "Continue"
                },
                onPrimary = {
                    when {
                        !permissions.notificationsGranted ->
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        !permissions.batteryExempt -> viewModel.openBatterySettings()
                        else -> page = 4
                    }
                },
                secondaryLabel = "I've done this — Recheck",
                onSecondary = viewModel::refresh,
                tertiaryLabel = "Continue anyway",
                onTertiary = { page = 4 },
            )
            else -> StepContent(
                icon = Icons.Outlined.CheckCircle,
                title = "You're all set",
                body = "Next: learn your Plus Key identity, then assign actions from Home.",
                primaryLabel = "Let's go",
                onPrimary = { viewModel.completeOnboarding(onFinished) },
            )
        }

        Text(
            text = "${(page + 1).coerceAtMost(5)} / 5",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun StepContent(
    icon: ImageVector,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    status: String? = null,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    tertiaryLabel: String? = null,
    onTertiary: (() -> Unit)? = null,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(Modifier.height(32.dp))
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(64.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(20.dp))
                Text(text = title, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (status != null) {
                    Spacer(Modifier.height(16.dp))
                    StatusChip(
                        label = status,
                        tone = if (status.contains("not", ignoreCase = true) ||
                            status.contains("needed", ignoreCase = true)
                        ) {
                            StatusTone.Warning
                        } else {
                            StatusTone.Active
                        },
                    )
                }
            }
        }
        Spacer(Modifier.height(28.dp))
        Button(onClick = onPrimary, modifier = Modifier.fillMaxWidth()) {
            Text(primaryLabel)
        }
        if (secondaryLabel != null && onSecondary != null) {
            OutlinedButton(
                onClick = onSecondary,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
            ) {
                Text(secondaryLabel)
            }
        }
        if (tertiaryLabel != null && onTertiary != null) {
            TextButton(onClick = onTertiary) {
                Text(tertiaryLabel)
            }
        }
    }
}
