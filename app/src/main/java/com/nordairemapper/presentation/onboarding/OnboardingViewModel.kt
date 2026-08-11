package com.nordairemapper.presentation.onboarding

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nordairemapper.domain.repository.SettingsRepository
import com.nordairemapper.service.AccessibilityUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OnboardingPermissionState(
    val accessibilityEnabled: Boolean = false,
    val overlayGranted: Boolean = false,
    val notificationsGranted: Boolean = true,
    val batteryExempt: Boolean = false,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _permissions = MutableStateFlow(readPermissions())
    val permissions: StateFlow<OnboardingPermissionState> = _permissions.asStateFlow()

    fun refresh() {
        _permissions.update { readPermissions() }
    }

    fun openAccessibilitySettings() = AccessibilityUtils.openAccessibilitySettings(context)

    fun openOverlaySettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openBatterySettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun completeOnboarding(onDone: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
            onDone()
        }
    }

    private fun readPermissions(): OnboardingPermissionState {
        val pm = context.getSystemService(PowerManager::class.java)
        return OnboardingPermissionState(
            accessibilityEnabled = AccessibilityUtils.isServiceEnabled(context),
            overlayGranted = Settings.canDrawOverlays(context),
            notificationsGranted = context.checkSelfPermission(
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == android.content.pm.PackageManager.PERMISSION_GRANTED,
            batteryExempt = pm?.isIgnoringBatteryOptimizations(context.packageName) == true,
        )
    }
}
