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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.BatterySaver
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.nordairemapper.presentation.detection.EnableDetectionViewModel
import com.nordairemapper.presentation.detection.UnlockMethodsSection
import com.nordairemapper.ui.components.NordGhostButton
import com.nordairemapper.ui.components.NordHeading
import com.nordairemapper.ui.components.NordPrimaryButton
import com.nordairemapper.ui.components.NordSurfaceCard
import com.nordairemapper.ui.components.PhoneDiagram
import com.nordairemapper.ui.components.SectionLabel
import com.nordairemapper.ui.components.StatusChip
import com.nordairemapper.ui.components.StatusTone

private const val PageCount = 7

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
    unlockViewModel: EnableDetectionViewModel = hiltViewModel(),
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
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
            if (page > 0) {
                IconButton(
                    onClick = { page -= 1 },
                    modifier = Modifier.align(Alignment.TopStart),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Previous step",
                    )
                }
            }
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
                        icon = Icons.Outlined.Notifications,
                        title = "Heads-up codes",
                        body = "Pairing finishes inside a heads-up notification — you type the code without leaving the pairing dialog. Alerts also warn you if detection ever stops.",
                        statusLabel = if (permissions.notificationsGranted) "Allowed" else "Not allowed yet",
                        statusTone = if (permissions.notificationsGranted) StatusTone.Active else StatusTone.Warning,
                        primaryLabel = if (permissions.notificationsGranted) "Continue" else "Allow notifications",
                        onPrimary = {
                            if (permissions.notificationsGranted) page = 3
                            else notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        secondaryLabel = if (!permissions.notificationsGranted) "I've enabled it — Recheck" else null,
                        onSecondary = viewModel::refresh,
                        tertiaryLabel = "Skip for now",
                        onTertiary = { page = 3 },
                    )
                    3 -> DetectionStepContent(
                        viewModel = unlockViewModel,
                        onContinue = { page = 4 },
                    )
                    4 -> StepContent(
                        icon = Icons.Outlined.Layers,
                        title = "Display over apps",
                        body = "Needed for the floating menu and visual action popup. Skip if you only want single-action remaps without on-screen UI.",
                        statusLabel = if (permissions.overlayGranted) "Granted" else "Not granted yet",
                        statusTone = if (permissions.overlayGranted) StatusTone.Active else StatusTone.Warning,
                        primaryLabel = if (permissions.overlayGranted) "Continue" else "Open Display over apps",
                        onPrimary = {
                            if (permissions.overlayGranted) page = 5
                            else viewModel.openOverlaySettings()
                        },
                        secondaryLabel = if (!permissions.overlayGranted) "I've enabled it" else null,
                        onSecondary = viewModel::refresh,
                        tertiaryLabel = "Skip for now",
                        onTertiary = { page = 5 },
                    )
                    5 -> StepContent(
                        icon = Icons.Outlined.BatterySaver,
                        title = "Keep it alive",
                        body = "Battery exemption helps OxygenOS keep Plus Key detection running overnight and through doze.",
                        statusLabel = if (permissions.batteryExempt) "Battery exempt" else "Battery not exempt",
                        statusTone = if (permissions.batteryExempt) StatusTone.Active else StatusTone.Warning,
                        primaryLabel = if (permissions.batteryExempt) "Continue" else "Exempt from battery optimization",
                        onPrimary = {
                            if (permissions.batteryExempt) page = 6
                            else viewModel.openBatterySettings()
                        },
                        secondaryLabel = if (!permissions.batteryExempt) "I've done this — Recheck" else null,
                        onSecondary = viewModel::refresh,
                        tertiaryLabel = "Continue anyway",
                        onTertiary = { page = 6 },
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
                text = "Keyforge",
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

/**
 * Onboarding page 2: the whole Unlock experience lives here — no detour to
 * the standalone subpage. Embeds [UnlockMethodsSection] so the user pairs,
 * watches chips turn green, and hits Continue without leaving onboarding.
 */
@Composable
private fun DetectionStepContent(
    viewModel: EnableDetectionViewModel,
    onContinue: () -> Unit,
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val fullyUnlocked = state.readLogsGranted &&
        state.bankingAutoResumeReady &&
        state.logAccessVisible != false

    Column(modifier = Modifier.fillMaxSize()) {
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StepIconTile(icon = Icons.Outlined.Sensors)
                Spacer(Modifier.height(24.dp))
                NordHeading(
                    text = "Enable Plus Key detection",
                    style = MaterialTheme.typography.headlineMedium,
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "OnePlus doesn't send the Plus Key to apps. Unlock once right here — Built-in needs no PC.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 8.dp),
                )
                Spacer(Modifier.height(16.dp))

                if (fullyUnlocked) {
                    NordSurfaceCard(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(28.dp),
                            )
                            Text(
                                text = "Detection unlocked — all three grants are in. Continue to finish setup.",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                } else {
                    // Compact one-row status: READ_LOGS / log access / banking.
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        StatusChip(
                            label = "READ_LOGS",
                            tone = if (state.readLogsGranted) StatusTone.Active else StatusTone.Warning,
                            modifier = Modifier.weight(1f),
                        )
                        StatusChip(
                            label = when (state.logAccessVisible) {
                                null -> "Log access"
                                true -> "Log access"
                                false -> "Log blocked"
                            },
                            tone = if (state.logAccessVisible == true) StatusTone.Active else StatusTone.Warning,
                            modifier = Modifier.weight(1f),
                        )
                        StatusChip(
                            label = "Banking pause",
                            tone = if (state.bankingAutoResumeReady) StatusTone.Active else StatusTone.Warning,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    Spacer(Modifier.height(14.dp))
                    SectionLabel(text = "Choose how to unlock")
                    UnlockMethodsSection(
                        viewModel = viewModel,
                        // Notifications have their own onboarding page now — the
                        // in-section gate card would ask a second time.
                        showNotificationGate = false,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }

        NordPrimaryButton(text = "Continue", onClick = onContinue)
    }
}

@Composable
private fun StepIconTile(icon: ImageVector) {
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
        // Centered when content is short, scrollable when it overflows:
        // BoxWithConstraints lends the viewport height as minHeight so
        // Arrangement.Center still works inside the scroll.
        androidx.compose.foundation.layout.BoxWithConstraints(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = maxHeight)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                StepIconTile(icon = icon)
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
