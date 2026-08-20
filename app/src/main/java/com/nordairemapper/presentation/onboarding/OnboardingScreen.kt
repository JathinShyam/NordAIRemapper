package com.nordairemapper.presentation.onboarding

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.ui.components.NordGhostButton
import com.nordairemapper.ui.components.NordHeading
import com.nordairemapper.ui.components.NordPrimaryButton
import com.nordairemapper.ui.components.PhoneDiagram
import com.nordairemapper.ui.components.StatusChip
import com.nordairemapper.ui.components.StatusTone

private const val PageCount = 6

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    onOpenEnableDetection: () -> Unit,
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
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 20.dp, vertical = 22.dp),
    ) {
        Box(modifier = Modifier.weight(1f)) {
            AnimatedContent(
                targetState = page,
                transitionSpec = {
                    val forward = targetState > initialState
                    (slideInHorizontally { if (forward) it else -it } + fadeIn())
                        .togetherWith(slideOutHorizontally { if (forward) -it else it } + fadeOut())
                },
                label = "onboardingPage",
            ) { currentPage ->
                when (currentPage) {
                    0 -> WelcomeStepContent(onPrimary = { page = 1 })
                    1 -> StepContent(
                        icon = Icons.Outlined.Settings,
                        title = "Accessibility",
                        body = "Required to detect the Plus Key and run system actions like screenshot, lock, and recents. We only observe hardware keys — never your screen.",
                        statusLabel = if (permissions.accessibilityEnabled) "Enabled" else "Not enabled",
                        statusTone = if (permissions.accessibilityEnabled) StatusTone.Active else StatusTone.Warning,
                        primaryLabel = if (permissions.accessibilityEnabled) "Continue" else "Open settings",
                        onPrimary = {
                            if (permissions.accessibilityEnabled) page = 2
                            else viewModel.openAccessibilitySettings()
                        },
                        secondaryLabel = if (!permissions.accessibilityEnabled) "I've enabled it" else null,
                        onSecondary = viewModel::refresh,
                    )
                    2 -> StepContent(
                        icon = Icons.Outlined.Sensors,
                        title = "Enable Plus Key detection",
                        body = "OnePlus doesn't send the Plus Key to apps. Unlock logcat detection once (USB preferred) to grant READ_LOGS.",
                        statusLabel = if (permissions.readLogsGranted) "READ_LOGS granted" else "READ_LOGS needed",
                        statusTone = if (permissions.readLogsGranted) StatusTone.Active else StatusTone.Warning,
                        primaryLabel = if (permissions.readLogsGranted) "Continue" else "Unlock detection",
                        onPrimary = {
                            if (permissions.readLogsGranted) page = 3
                            else onOpenEnableDetection()
                        },
                        secondaryLabel = if (!permissions.readLogsGranted) "I've done this — Recheck" else null,
                        onSecondary = viewModel::refresh,
                        tertiaryLabel = "Skip for now",
                        onTertiary = { page = 3 },
                    )
                    3 -> StepContent(
                        icon = Icons.Outlined.Layers,
                        title = "Display over apps",
                        body = "Needed for the floating overlay menu. Skip if you only want single actions. You can still use single-action remaps without it.",
                        statusLabel = if (permissions.overlayGranted) "Granted" else "Not granted yet",
                        statusTone = if (permissions.overlayGranted) StatusTone.Active else StatusTone.Warning,
                        primaryLabel = if (permissions.overlayGranted) "Continue" else "Open overlay settings",
                        onPrimary = {
                            if (permissions.overlayGranted) page = 4
                            else viewModel.openOverlaySettings()
                        },
                        secondaryLabel = if (!permissions.overlayGranted) "I've enabled it" else null,
                        onSecondary = viewModel::refresh,
                        tertiaryLabel = "Skip for now",
                        onTertiary = { page = 4 },
                    )
                    4 -> StepContent(
                        icon = Icons.Outlined.Notifications,
                        title = "Keep it alive",
                        body = "Notifications and battery exemption help OxygenOS keep detection running in the background.",
                        statusLabel = buildString {
                            append(if (permissions.notificationsGranted) "Notifications OK" else "Notifications needed")
                            append(" · ")
                            append(if (permissions.batteryExempt) "Battery exempt" else "Battery not exempt")
                        },
                        statusTone = if (permissions.notificationsGranted && permissions.batteryExempt) {
                            StatusTone.Active
                        } else {
                            StatusTone.Warning
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
                                else -> page = 5
                            }
                        },
                        secondaryLabel = "I've done this — Recheck",
                        onSecondary = viewModel::refresh,
                        tertiaryLabel = "Continue anyway",
                        onTertiary = { page = 5 },
                    )
                    else -> StepContent(
                        icon = Icons.Outlined.CheckCircle,
                        title = "You're all set",
                        body = "Confirm Plus Key detection, then assign actions from Home.",
                        primaryLabel = "Go to Home",
                        onPrimary = { viewModel.completeOnboarding(onFinished) },
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        PageDots(current = page.coerceAtMost(PageCount - 1), total = PageCount)
    }
}

@Composable
private fun WelcomeStepContent(onPrimary: () -> Unit) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            PhoneDiagram(
                highlightKey = true,
                edgeRipple = true,
                modifier = Modifier
                    .fillMaxWidth(0.56f)
                    .aspectRatio(77f / 163.4f)
                    .padding(bottom = 16.dp),
            )
            NordHeading(
                text = "Nord AI Remapper",
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = "Remap the Nord 5 Plus Key to single, double, and long-press actions.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
        }
        NordPrimaryButton(text = "Get started", onClick = onPrimary)
    }
}

@Composable
private fun StepContent(
    icon: ImageVector,
    title: String,
    body: String,
    primaryLabel: String,
    onPrimary: () -> Unit,
    statusLabel: String? = null,
    statusTone: StatusTone = StatusTone.Active,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null,
    tertiaryLabel: String? = null,
    onTertiary: (() -> Unit)? = null,
) {
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .border(
                        1.dp,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.18f),
                        RoundedCornerShape(18.dp),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
            Spacer(Modifier.height(24.dp))
            NordHeading(
                text = title,
                style = MaterialTheme.typography.headlineMedium,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 8.dp),
            )
            if (statusLabel != null) {
                Spacer(Modifier.height(20.dp))
                StatusChip(
                    label = statusLabel,
                    tone = statusTone,
                )
            }
        }

        NordPrimaryButton(text = primaryLabel, onClick = onPrimary)
        if (secondaryLabel != null && onSecondary != null) {
            Spacer(Modifier.height(8.dp))
            NordGhostButton(text = secondaryLabel, onClick = onSecondary)
        }
        if (tertiaryLabel != null && onTertiary != null) {
            TextButton(onClick = onTertiary, modifier = Modifier.fillMaxWidth()) {
                Text(tertiaryLabel)
            }
        }
    }
}

@Composable
private fun PageDots(current: Int, total: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(total) { index ->
            val active = index == current
            val width by animateDpAsState(
                targetValue = if (active) 16.dp else 6.dp,
                label = "pageDotWidth",
            )
            Box(
                modifier = Modifier
                    .padding(horizontal = 3.dp)
                    .height(6.dp)
                    .width(width)
                    .clip(CircleShape)
                    .background(
                        if (active) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline,
                    ),
            )
        }
    }
}
