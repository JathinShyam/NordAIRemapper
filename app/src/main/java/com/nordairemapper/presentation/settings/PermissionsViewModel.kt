package com.nordairemapper.presentation.settings

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nordairemapper.service.AccessibilityUtils
import com.nordairemapper.service.AppPermissions
import com.nordairemapper.service.ElevatedPermissions
import com.nordairemapper.service.LogVisibilityProbe
import com.nordairemapper.service.LogcatWatcherService
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PermissionsUiState(
    val items: List<AppPermissions.Item> = emptyList(),
    val hubAttentionCount: Int = 0,
    val hubSummaryLabel: String = "Checking",
    val probingLogVisibility: Boolean = false,
)

@HiltViewModel
class PermissionsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _uiState = MutableStateFlow(PermissionsUiState())
    val uiState: StateFlow<PermissionsUiState> = _uiState.asStateFlow()

    init {
        refresh(fast = true)
    }

    fun refresh(fast: Boolean = false) {
        val base = AppPermissions.snapshot(context)
        val readLogsGranted = LogcatWatcherService.hasReadLogsPermission(context)
        val attention = AppPermissions.hubAttentionCount(base)

        if (!readLogsGranted || fast) {
            _uiState.value = PermissionsUiState(
                items = base,
                hubAttentionCount = attention,
                hubSummaryLabel = AppPermissions.hubSummaryLabel(attention),
                probingLogVisibility = false,
            )
            if (readLogsGranted && !fast) {
                probeLogVisibility(base)
            }
            return
        }

        probeLogVisibility(base)
    }

    private fun probeLogVisibility(base: List<AppPermissions.Item>) {
        _uiState.update {
            it.copy(
                items = AppPermissions.withLogVisibility(
                    items = base,
                    readLogsGranted = true,
                    logResult = null,
                    probing = true,
                ),
                probingLogVisibility = true,
            )
        }
        viewModelScope.launch {
            val result = runCatching { LogVisibilityProbe.probe() }.getOrNull()
            val merged = AppPermissions.withLogVisibility(
                items = base,
                readLogsGranted = true,
                logResult = result,
                probing = false,
            )
            val attention = AppPermissions.hubAttentionCount(merged)
            _uiState.value = PermissionsUiState(
                items = merged,
                hubAttentionCount = attention,
                hubSummaryLabel = AppPermissions.hubSummaryLabel(attention),
                probingLogVisibility = false,
            )
        }
    }

    fun openAccessibilitySettings() = AccessibilityUtils.openAccessibilitySettings(context)

    fun openOverlaySettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun openNotificationSettings() {
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    fun openBatterySettings() {
        context.startActivity(
            Intent(
                Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                Uri.parse("package:${context.packageName}"),
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }

    fun openUsageAccessSettings() = ElevatedPermissions.openUsageAccessSettings(context)

    fun onPermissionAction(id: AppPermissions.Id, onOpenEnableDetection: () -> Unit) {
        when (id) {
            AppPermissions.Id.ACCESSIBILITY -> openAccessibilitySettings()
            AppPermissions.Id.READ_LOGS,
            AppPermissions.Id.LOG_VISIBILITY,
            AppPermissions.Id.WRITE_SECURE_SETTINGS,
            -> onOpenEnableDetection()
            AppPermissions.Id.OVERLAY -> openOverlaySettings()
            AppPermissions.Id.NOTIFICATIONS -> openNotificationSettings()
            AppPermissions.Id.BATTERY -> openBatterySettings()
            AppPermissions.Id.USAGE_ACCESS -> openUsageAccessSettings()
        }
    }
}
