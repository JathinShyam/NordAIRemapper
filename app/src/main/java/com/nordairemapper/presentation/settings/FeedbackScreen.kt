package com.nordairemapper.presentation.settings

import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nordairemapper.domain.model.HapticIntensity
import com.nordairemapper.ui.components.NordGhostButton
import com.nordairemapper.ui.components.NordTopBarTitle
import com.nordairemapper.ui.components.SectionLabel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedbackScreen(
    onBack: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    val ringScale = remember { Animatable(1f) }
    val ringAlpha = remember { Animatable(0.4f) }
    val coreScale = remember { Animatable(1f) }

    fun previewIntensity(intensity: HapticIntensity) {
        val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            ?: context.getSystemService(Vibrator::class.java)
        val effect = when (intensity) {
            HapticIntensity.LIGHT ->
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            HapticIntensity.MEDIUM ->
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            HapticIntensity.HEAVY ->
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        }
        vibrator?.vibrate(effect)
        scope.launch {
            ringScale.snapTo(1f)
            ringAlpha.snapTo(0.55f)
            coreScale.snapTo(1f)
            launch {
                ringScale.animateTo(1.55f, tween(520, easing = FastOutSlowInEasing))
            }
            launch {
                ringAlpha.animateTo(0f, tween(520, easing = FastOutSlowInEasing))
            }
            coreScale.animateTo(1.18f, tween(160, easing = FastOutSlowInEasing))
            coreScale.animateTo(1f, tween(220, easing = FastOutSlowInEasing))
            ringScale.snapTo(1f)
            ringAlpha.snapTo(0.4f)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    NordTopBarTitle(
                        title = "Feedback",
                        subtitle = "Haptic confirmation",
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
            SectionLabel("Haptics")
            SettingsGroup {
                SettingsToggleRow(
                    title = "Haptic feedback",
                    subtitle = if (settings.hapticFeedback) {
                        "On when a remap fires"
                    } else {
                        "Off"
                    },
                    checked = settings.hapticFeedback,
                    onCheckedChange = {
                        view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
                        viewModel.setHapticFeedback(it)
                    },
                )
            }

            SectionLabel("Intensity")
            SettingsGroup {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                ) {
                    Text(
                        text = "Vibration intensity",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                        color = if (settings.hapticFeedback) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f)
                        },
                    )
                    Text(
                        text = "How strong the Plus Key confirmation feels",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                            alpha = if (settings.hapticFeedback) 1f else 0.5f,
                        ),
                        modifier = Modifier.padding(top = 3.dp, bottom = 12.dp),
                    )
                    SettingsSegmentedControl(
                        options = HapticIntensity.entries.map { intensity ->
                            SettingsSegmentOption(
                                key = intensity.name,
                                label = when (intensity) {
                                    HapticIntensity.LIGHT -> "Light"
                                    HapticIntensity.MEDIUM -> "Medium"
                                    HapticIntensity.HEAVY -> "Heavy"
                                },
                            )
                        },
                        selectedKey = settings.hapticIntensity.name,
                        onSelect = { key ->
                            if (!settings.hapticFeedback) return@SettingsSegmentedControl
                            val intensity = runCatching { HapticIntensity.valueOf(key) }.getOrNull()
                                ?: return@SettingsSegmentedControl
                            viewModel.setHapticIntensity(intensity)
                            previewIntensity(intensity)
                        },
                    )
                }
            }

            SectionLabel("Preview")
            SettingsGroup {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(
                        modifier = Modifier.size(72.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .scale(ringScale.value)
                                .graphicsLayer { alpha = ringAlpha.value }
                                .border(2.dp, MaterialTheme.colorScheme.primary, CircleShape),
                        )
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .scale(coreScale.value)
                                .background(MaterialTheme.colorScheme.primary, CircleShape),
                        )
                    }
                    NordGhostButton(
                        text = "Pulse preview",
                        onClick = { previewIntensity(settings.hapticIntensity) },
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}
