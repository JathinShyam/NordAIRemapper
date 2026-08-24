package com.nordairemapper.presentation.detection

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nordairemapper.service.ElevatedPermissions
import com.nordairemapper.service.LogVisibilityProbe
import com.nordairemapper.service.LogcatWatcherService
import com.nordairemapper.service.ReadLogsGrantHelper
import com.nordairemapper.service.ShizukuGrant
import com.nordairemapper.service.adb.PairingNotifier
import com.nordairemapper.service.adb.PairingSession
import com.nordairemapper.service.adb.ReadLogsGrantViaWirelessAdb
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import javax.inject.Inject

/** Which Unlock route the user picked on the Enable detection screen. */
enum class DetectionMethod(val label: String, val description: String) {
    BUILTIN("Built-in", "Pair with Wireless debugging right in the app — no PC"),
    SHIZUKU("Shizuku", "One tap if you already run Shizuku"),
    MANUAL_ADB("Manual ADB", "Copy one command and run it from a computer"),
}

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
    val method: DetectionMethod = DetectionMethod.BUILTIN,
    // Shizuku path
    val shizukuInstalled: Boolean = false,
    val shizukuRunning: Boolean = false,
    val shizukuGranted: Boolean = false,
    val isGrantingViaShizuku: Boolean = false,
    // Built-in wireless pairing path
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
            shizukuInstalled = ShizukuGrant.isInstalled(context),
        ),
    )
    val uiState: StateFlow<EnableDetectionUiState> = _uiState.asStateFlow()

    private var discoverJob: Job? = null

    private val shizukuPermissionListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (requestCode != ShizukuGrant.PERMISSION_REQUEST_CODE) return@OnRequestPermissionResultListener
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                grantViaShizuku()
            } else {
                _uiState.update {
                    it.copy(errorMessage = "Shizuku permission denied — pick another method.")
                }
            }
        }

    init {
        Shizuku.addRequestPermissionResultListener(shizukuPermissionListener)
        refreshShizukuState()
    }

    override fun onCleared() {
        Shizuku.removeRequestPermissionResultListener(shizukuPermissionListener)
        PairingSession.clear()
        PairingNotifier.cancel(context)
        super.onCleared()
    }

    fun refresh() {
        viewModelScope.launch {
            refreshShizukuState()
            val ok = grantViaWirelessAdb.verifyAndSyncWatcher()
            val banking = ElevatedPermissions.canAutoResumeAccessibility(context)
            val visible = if (ok) {
                LogVisibilityProbe.probe() == LogVisibilityProbe.Result.VISIBLE
            } else {
                null
            }
            if (!LogcatWatcherService.hasTailSeenNonSelf()) {
                // Probe above already surfaced the consent prompt. Allow only
                // applies to future connections, so reconnect unconditionally
                // — the fresh tail inherits whatever consent is registered.
                LogcatWatcherService.restart(context)
            }
            _uiState.update {
                it.copy(
                    readLogsGranted = ok,
                    logAccessVisible = visible,
                    bankingAutoResumeReady = banking,
                    statusMessage = when {
                        ok && visible == false ->
                            "System logs are still blocked. Allow log access when Keyforge asks, then leave and reopen this screen."
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

    fun setMethod(method: DetectionMethod) {
        _uiState.update { it.copy(method = method, errorMessage = null) }
        if (method == DetectionMethod.SHIZUKU) refreshShizukuState()
    }

    fun refreshShizukuState() {
        _uiState.update {
            it.copy(
                shizukuInstalled = ShizukuGrant.isInstalled(context),
                shizukuRunning = ShizukuGrant.isServiceRunning(),
                shizukuGranted = ShizukuGrant.hasPermission(),
            )
        }
    }

    fun requestShizukuThenGrant() {
        refreshShizukuState()
        val state = _uiState.value
        when {
            !state.shizukuRunning -> _uiState.update {
                it.copy(
                    errorMessage = "Shizuku isn't running. Open the Shizuku app and start it " +
                        "(wireless debugging or a one-time PC start), then come back.",
                )
            }
            state.shizukuGranted -> grantViaShizuku()
            else -> runCatching { Shizuku.requestPermission(ShizukuGrant.PERMISSION_REQUEST_CODE) }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(errorMessage = "Could not ask Shizuku for permission: ${t.message}")
                    }
                }
        }
    }

    private fun grantViaShizuku() {
        if (_uiState.value.isGrantingViaShizuku) return
        viewModelScope.launch {
            _uiState.update {
                it.copy(isGrantingViaShizuku = true, errorMessage = null, statusMessage = "Granting via Shizuku…")
            }
            ShizukuGrant.runGrants(context)
                .onSuccess {
                    afterGrantSucceeded(
                        extraStatus = "Done via Shizuku. You can stop Shizuku — grants stay.",
                    )
                }
                .onFailure { t ->
                    _uiState.update {
                        it.copy(
                            isGrantingViaShizuku = false,
                            statusMessage = null,
                            errorMessage = "Shizuku grant failed: ${t.message}",
                        )
                    }
                }
        }
    }

    /** Shared verification + watcher reconnect after any successful grant path. */
    private suspend fun afterGrantSucceeded(extraStatus: String) {
        val banking = ElevatedPermissions.canAutoResumeAccessibility(context)
        val visible = LogVisibilityProbe.probe() == LogVisibilityProbe.Result.VISIBLE
        if (!LogcatWatcherService.hasTailSeenNonSelf()) {
            // Same deterministic reconnect: probe surfaced the
            // consent prompt; the fresh tail inherits the Allow.
            LogcatWatcherService.restart(context)
        }
        _uiState.update {
            it.copy(
                isGrantingViaShizuku = false,
                readLogsGranted = true,
                logAccessVisible = visible,
                bankingAutoResumeReady = banking,
                statusMessage = when {
                    visible == false ->
                        "System logs are still blocked. Allow log access when Keyforge asks, then leave and reopen this screen."
                    banking -> extraStatus
                    else -> "Granted. If banking auto-pause still fails, re-run Unlock."
                },
                errorMessage = null,
            )
        }
    }

    fun onPairingCodeChange(value: String) {
        val digits = value.filter { it.isDigit() }.take(6)
        _uiState.update { it.copy(pairingCode = digits, errorMessage = null) }
    }

    fun onPairingPortChange(value: String) {
        val parsed = parseHostPortOrPort(value)
        PairingSession.set(
            host = parsed.host ?: _uiState.value.discoveredHost,
            pairingPort = parsed.portText.toIntOrNull(),
            connectPort = _uiState.value.connectPort.toIntOrNull(),
        )
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
        PairingSession.set(
            host = _uiState.value.discoveredHost,
            pairingPort = _uiState.value.pairingPort.toIntOrNull()
                ?: _uiState.value.discoveredPort,
            connectPort = parsed.portText.toIntOrNull(),
        )
        _uiState.update {
            it.copy(
                connectPort = parsed.portText,
                discoveredHost = parsed.host ?: it.discoveredHost,
                errorMessage = null,
            )
        }
    }

    fun openWirelessDebugging() {
        ReadLogsGrantHelper.openWirelessDebugging(context)
    }

    /** Opens the Shizuku manager app so the user can start the service. */
    fun openShizukuApp() {
        runCatching {
            val intent = context.packageManager.getLaunchIntentForPackage(ShizukuGrant.SHIZUKU_PACKAGE)
            if (intent != null) {
                context.startActivity(intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK))
            } else {
                _uiState.update {
                    it.copy(errorMessage = "Shizuku manager not installed. Install it from Play Store or GitHub.")
                }
            }
        }
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
                    // Arm the notification-reply path: the user can finish
                    // pairing from the heads-up without leaving the system dialog.
                    PairingSession.set(
                        host = endpoint.host,
                        pairingPort = endpoint.port,
                        connectPort = it.connectPort.toIntOrNull(),
                    )
                    PairingNotifier.postPrompt(context, endpoint.port)
                    it.copy(
                        isDiscovering = false,
                        discoveredHost = endpoint.host,
                        discoveredPort = endpoint.port,
                        pairingPort = endpoint.port.toString(),
                        statusMessage = "Found pairing port ${endpoint.port}. Enter the 6-digit code — " +
                            "in the app or straight in the notification.",
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
                    _uiState.update { it.copy(isGranting = false) }
                    PairingSession.clear()
                    PairingNotifier.cancel(context)
                    afterGrantSucceeded(
                        extraStatus = "Done. You can turn Wireless debugging off — grants stay.",
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
