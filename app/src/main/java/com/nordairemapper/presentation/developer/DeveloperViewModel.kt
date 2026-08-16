package com.nordairemapper.presentation.developer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nordairemapper.domain.model.AppSettings
import com.nordairemapper.domain.model.DetectionStrategy
import com.nordairemapper.domain.repository.SettingsRepository
import com.nordairemapper.service.DetectionCoordinator
import com.nordairemapper.service.LogcatWatcherService
import com.nordairemapper.service.ReadLogsGrantHelper
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
class DeveloperViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    private val _readLogsGranted = MutableStateFlow(LogcatWatcherService.hasReadLogsPermission(context))
    val readLogsGranted: StateFlow<Boolean> = _readLogsGranted.asStateFlow()

    fun refreshPermissions() {
        _readLogsGranted.value = LogcatWatcherService.hasReadLogsPermission(context)
        DetectionCoordinator.syncLogcatWatcher(
            context = context,
            strategy = settings.value.detectionStrategy,
            serviceEnabled = settings.value.serviceEnabled,
        )
    }

    fun setStrategy(strategy: DetectionStrategy) {
        viewModelScope.launch {
            settingsRepository.setDetectionStrategy(strategy)
            DetectionCoordinator.syncLogcatWatcher(
                context = context,
                strategy = strategy,
                serviceEnabled = settings.value.serviceEnabled,
            )
        }
    }

    fun setLogcatPattern(pattern: String) {
        viewModelScope.launch { settingsRepository.setLogcatPattern(pattern) }
    }

    fun setDoublePressWindow(ms: Long) {
        viewModelScope.launch { settingsRepository.setDoublePressWindowMs(ms) }
    }

    fun setLongPressThreshold(ms: Long) {
        viewModelScope.launch { settingsRepository.setLongPressThresholdMs(ms) }
    }

    fun copyAdbCommand() {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(
            ClipData.newPlainText("adb command", LogcatWatcherService.ADB_GRANT_COMMAND),
        )
    }

    fun copyOnDeviceCommand() {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(
            ClipData.newPlainText("pm grant", ReadLogsGrantHelper.ON_DEVICE_SHELL_COMMAND),
        )
    }

    fun openWirelessDebugging() = ReadLogsGrantHelper.openWirelessDebugging(context)

    fun openShizuku() = ReadLogsGrantHelper.openShizukuOrPlayStore(context)

    fun restartLogcatWatcher() {
        DetectionCoordinator.syncLogcatWatcher(
            context = context,
            strategy = settings.value.detectionStrategy,
            serviceEnabled = settings.value.serviceEnabled,
        )
    }
}
