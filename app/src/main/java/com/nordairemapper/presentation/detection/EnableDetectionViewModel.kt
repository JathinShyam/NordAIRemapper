package com.nordairemapper.presentation.detection

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nordairemapper.service.LogcatWatcherService
import com.nordairemapper.service.ReadLogsGrantHelper
import com.nordairemapper.service.adb.ReadLogsGrantViaWirelessAdb
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EnableDetectionUiState(
    val readLogsGranted: Boolean = false,
    val pairingCode: String = "",
    val manualPort: String = "",
    val discoveredPort: Int? = null,
    val discoveredHost: String? = null,
    val isDiscovering: Boolean = false,
    val isGranting: Boolean = false,
    val statusMessage: String? = null,
    val errorMessage: String? = null,
    val showAdvanced: Boolean = false,
)

@HiltViewModel
class EnableDetectionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val grantViaWirelessAdb: ReadLogsGrantViaWirelessAdb,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        EnableDetectionUiState(readLogsGranted = grantViaWirelessAdb.hasReadLogs()),
    )
    val uiState: StateFlow<EnableDetectionUiState> = _uiState.asStateFlow()

    private var discoverJob: Job? = null

    fun refresh() {
        viewModelScope.launch {
            val ok = grantViaWirelessAdb.verifyAndSyncWatcher()
            _uiState.update {
                it.copy(
                    readLogsGranted = ok,
                    statusMessage = if (ok) {
                        "READ_LOGS granted — Plus Key detection can run."
                    } else {
                        it.statusMessage
                    },
                    errorMessage = null,
                )
            }
        }
    }

    fun onPairingCodeChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(pairingCode = digits, errorMessage = null) }
    }

    fun onManualPortChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(5)
        _uiState.update { it.copy(manualPort = digits) }
    }

    fun setShowAdvanced(show: Boolean) {
        _uiState.update { it.copy(showAdvanced = show) }
    }

    fun openWirelessDebugging() {
        ReadLogsGrantHelper.openWirelessDebugging(context)
    }

    fun openDeveloperOptions() {
        ReadLogsGrantHelper.openDeveloperOptions(context)
        _uiState.update {
            it.copy(
                statusMessage = "In Developer options: turn on Wireless debugging → tap the Wireless debugging row → Pair device with pairing code.",
            )
        }
    }

    fun onNearbyWifiDenied() {
        _uiState.update {
            it.copy(
                isDiscovering = false,
                statusMessage = "Nearby Wi‑Fi permission denied — enter the pairing port manually from the system dialog.",
            )
        }
    }

    fun startDiscovery() {
        if (_uiState.value.readLogsGranted || _uiState.value.isGranting) return
        discoverJob?.cancel()
        discoverJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isDiscovering = true,
                    statusMessage = "Looking for pairing port… Keep “Pair device with pairing code” open (not the SSID Allow screen).",
                    errorMessage = null,
                )
            }
            val endpoint = grantViaWirelessAdb.discoverPairingEndpoint()
            _uiState.update {
                if (endpoint != null) {
                    it.copy(
                        isDiscovering = false,
                        discoveredHost = endpoint.host,
                        discoveredPort = endpoint.port,
                        statusMessage = "Found pairing port ${endpoint.port}. Enter the 6-digit code.",
                    )
                } else {
                    it.copy(
                        isDiscovering = false,
                        statusMessage = "Port not found automatically. Open Pair device with pairing code and enter the port after the colon (IP:port).",
                    )
                }
            }
        }
    }

    fun pairAndGrant() {
        val state = _uiState.value
        if (state.isGranting) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isGranting = true,
                    errorMessage = null,
                    statusMessage = "Pairing and granting READ_LOGS…",
                )
            }
            val port = state.manualPort.toIntOrNull() ?: state.discoveredPort
            val result = grantViaWirelessAdb.pairAndGrant(
                pairingCode = state.pairingCode,
                host = state.discoveredHost,
                pairingPort = port,
            )
            when (result) {
                ReadLogsGrantViaWirelessAdb.GrantResult.AlreadyGranted,
                ReadLogsGrantViaWirelessAdb.GrantResult.Success,
                -> _uiState.update {
                    it.copy(
                        isGranting = false,
                        readLogsGranted = true,
                        statusMessage = "Done. You can turn Wireless debugging off — READ_LOGS stays granted.",
                        errorMessage = null,
                    )
                }
                is ReadLogsGrantViaWirelessAdb.GrantResult.Failed -> _uiState.update {
                    it.copy(
                        isGranting = false,
                        errorMessage = result.message,
                        statusMessage = null,
                    )
                }
            }
        }
    }

    fun copyUsbAdbCommand() {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(
            ClipData.newPlainText("adb command", LogcatWatcherService.ADB_GRANT_COMMAND),
        )
        _uiState.update { it.copy(statusMessage = "USB ADB command copied.") }
    }
}
