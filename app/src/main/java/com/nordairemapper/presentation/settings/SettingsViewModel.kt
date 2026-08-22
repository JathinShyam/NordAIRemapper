package com.nordairemapper.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.PowerManager
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nordairemapper.domain.model.AppSettings
import com.nordairemapper.domain.model.HapticIntensity
import com.nordairemapper.domain.model.ThemeMode
import com.nordairemapper.domain.repository.SettingsRepository
import com.nordairemapper.presentation.remap.InstalledAppInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _batteryExempt = MutableStateFlow(isBatteryExempt())
    val batteryExempt: StateFlow<Boolean> = _batteryExempt.asStateFlow()

    fun refreshBattery() {
        _batteryExempt.value = isBatteryExempt()
    }

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch {
        settingsRepository.setThemeMode(mode)
    }

    fun setDynamicColor(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setDynamicColor(enabled)
    }

    fun setShowServiceNotification(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setShowServiceNotification(enabled)
    }

    fun setHapticFeedback(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setHapticFeedback(enabled)
    }

    fun setHapticIntensity(intensity: HapticIntensity) = viewModelScope.launch {
        settingsRepository.setHapticIntensity(intensity)
    }

    fun setVisualOverlayEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setVisualOverlayEnabled(enabled)
    }

    fun setLockScreenSingleEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setLockScreenSingleEnabled(enabled)
    }

    fun setLockScreenDoubleEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setLockScreenDoubleEnabled(enabled)
    }

    fun setLockScreenLongEnabled(enabled: Boolean) = viewModelScope.launch {
        settingsRepository.setLockScreenLongEnabled(enabled)
    }

    fun addExclusion(app: InstalledAppInfo) = viewModelScope.launch {
        val next = settings.value.excludedApps + app.packageName
        settingsRepository.setExcludedApps(next)
    }

    fun removeExclusion(packageName: String) = viewModelScope.launch {
        settingsRepository.setExcludedApps(settings.value.excludedApps - packageName)
    }

    fun resetOnboarding() = viewModelScope.launch {
        settingsRepository.setOnboardingCompleted(false)
    }

    fun openBatterySettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun openGithub() {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/JathinShyam/NordAIRemapper"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun versionName(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull() ?: "unknown"

    private fun isBatteryExempt(): Boolean {
        val pm = context.getSystemService(PowerManager::class.java)
        return pm?.isIgnoringBatteryOptimizations(context.packageName) == true
    }
}
