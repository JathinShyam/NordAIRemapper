package com.nordairemapper.presentation.detection

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nordairemapper.service.ElevatedPermissions
import com.nordairemapper.service.LogVisibilityProbe
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
    /**
     * Null until probed. False = logd filters cross-app logs despite the
     * grant (OxygenOS "USB debugging (Security settings)" reset at boot):
     * detection is dead even though every Android-level check is green.
     */
    val logAccessVisible: Boolean? = null,
    /** WRITE_SECURE_SETTINGS + usage access for hands-free banking Accessibility pause. */
    val bankingAutoResumeReady: Boolean = false,
    val pairingCode: String = "",
    /** Pairing dialog port only (under the 6-digit code). */
    val pairingPort: String = "",
    /**
     * Wireless debugging page “IP address & port” — different from [pairingPort].
     * Needed when mDNS cannot find the TLS connect service after pairing.
     */
    val connectPort: String = "",
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
        EnableDetectionUiState(
            readLogsGranted = grantViaWirelessAdb.hasReadLogs(),
            bankingAutoResumeReady = ElevatedPermissions.canAutoResumeAccessibility(context),
        ),
    )
    val uiState: StateFlow<EnableDetectionUiState> = _uiState.asStateFlow()

    private var discoverJob: Job? = null

    fun refresh() {
        viewModelScope.launch {
            val ok = grantViaWirelessAdb.verifyAndSyncWatcher()
            val banking = ElevatedPermissions.canAutoResumeAccessibility(context)
            val visible = if (ok) {
                LogVisibilityProbe.probe() == LogVisibilityProbe.Result.VISIBLE
            } else {
                null
            }
            if (visible == true && !LogcatWatcherService.hasTailSeenNonSelf()) {
                // Foreground spawn proved access; reconnect the consent-blind tail.
                LogcatWatcherService.restart(context)
            }
            _uiState.update {
                it.copy(
                    readLogsGranted = ok,
                    logAccessVisible = visible,
                    bankingAutoResumeReady = banking,
                    statusMessage = when {
                        ok && visible == false ->
                            "Grants look fine but system logs are blocked. Enable Developer options → USB debugging (Security settings), then reboot."
                        ok && banking ->
                            "Unlocked — Plus Key detection and hands-free banking pause are ready."
                        ok ->
                            "READ_LOGS granted. Run Unlock once more for hands-free banking Accessibility pause."
                        else -> it.statusMessage
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

    fun onPairingPortChange(value: String) {
        val parsed = parseHostPortOrPort(value)
        _uiState.update {
            it.copy(
                pairingPort = parsed.portText,
                discoveredHost = parsed.host ?: it.discoveredHost,
                errorMessage = null,
            )
        }
    }

    fun onConnectPortChange(value: String) {
        val parsed = parseHostPortOrPort(value)
        _uiState.update {
            it.copy(
                connectPort = parsed.portText,
                discoveredHost = parsed.host ?: it.discoveredHost,
                errorMessage = null,
            )
        }
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
                statusMessage = "Wireless debugging → Pair device with pairing code for the 6-digit " +
                    "code. After that, the main Wireless debugging page shows a different IP:port for connection.",
            )
        }
    }

    fun onNearbyWifiDenied() {
        _uiState.update {
            it.copy(
                isDiscovering = false,
                statusMessage = "Nearby Wi‑Fi permission denied — enter pairing port and connection port manually.",
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
                    statusMessage = "Looking for pairing port… Keep “Pair device with pairing code” open.",
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
                        pairingPort = endpoint.port.toString(),
                        statusMessage = "Found pairing port ${endpoint.port}. Enter the 6-digit code. " +
                            "Also note the Wireless debugging page IP:port for Connection port if connect fails.",
                    )
                } else {
                    it.copy(
                        isDiscovering = false,
                        statusMessage = "Port not found automatically. Enter pairing port from under the code, " +
                            "and Connection port from the Wireless debugging page IP address & port.",
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
                    statusMessage = "Pairing, then connecting (uses a different port)…",
                )
            }
            val pairingPort = state.pairingPort.toIntOrNull() ?: state.discoveredPort
            val connectPort = state.connectPort.toIntOrNull()
            val result = grantViaWirelessAdb.pairAndGrant(
                pairingCode = state.pairingCode,
                host = state.discoveredHost,
                pairingPort = pairingPort,
                connectPort = connectPort,
            )
            when (result) {
                ReadLogsGrantViaWirelessAdb.GrantResult.AlreadyGranted,
                ReadLogsGrantViaWirelessAdb.GrantResult.Success,
                -> {
                    val banking = ElevatedPermissions.canAutoResumeAccessibility(context)
                    val visible = LogVisibilityProbe.probe() == LogVisibilityProbe.Result.VISIBLE
                    if (visible && !LogcatWatcherService.hasTailSeenNonSelf()) {
                        LogcatWatcherService.restart(context)
                    }
                    _uiState.update {
                        it.copy(
                            isGranting = false,
                            readLogsGranted = true,
                            logAccessVisible = visible,
                            bankingAutoResumeReady = banking,
                            statusMessage = if (visible == false) {
                                "Grants look fine but system logs are blocked. Enable Developer options → USB debugging (Security settings), then reboot."
                            } else if (banking) {
                                "Done. You can turn Wireless debugging off — grants stay."
                            } else {
                                "READ_LOGS granted. If banking auto-pause still fails, re-run Unlock."
                            },
                            errorMessage = null,
                        )
                    }
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

    private data class HostPortParse(val host: String?, val portText: String)

    /** Accepts "37123" or "192.168.1.5:37123". */
    private fun parseHostPortOrPort(raw: String): HostPortParse {
        val trimmed = raw.trim()
        if (trimmed.contains(':')) {
            val host = trimmed.substringBeforeLast(':').trim().ifEmpty { null }
            val port = trimmed.substringAfterLast(':').filter { it.isDigit() }.take(5)
            return HostPortParse(host, port)
        }
        return HostPortParse(null, trimmed.filter { it.isDigit() }.take(5))
    }
}
