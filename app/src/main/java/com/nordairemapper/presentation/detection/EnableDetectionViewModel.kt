package com.nordairemapper.presentation.detection

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.provider.Settings
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
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import rikka.shizuku.Shizuku
import javax.inject.Inject

/** Which Unlock route the user picked on the Enable detection screen. */
enum class DetectionMethod(val label: String) {
    BUILTIN("Built-in"),
    SHIZUKU("Shizuku"),
    MANUAL_ADB("Manual ADB"),
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
    /** Heads-up pairing replies need POST_NOTIFICATIONS. */
    val notificationsGranted: Boolean = false,
    // Built-in wireless pairing path
    val devOptionsEnabled: Boolean = false,
    val wifiDebugEnabled: Boolean = false,
    val isWatchingForPairing: Boolean = false,
    val discoveredPort: Int? = null,
    // Shizuku path
    val shizukuInstalled: Boolean = false,
    val shizukuRunning: Boolean = false,
    val shizukuGranted: Boolean = false,
    val isGrantingViaShizuku: Boolean = false,
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
            notificationsGranted = areNotificationsEnabled(),
            devOptionsEnabled = isDevOptionsEnabled(),
            wifiDebugEnabled = isWifiDebugEnabled(),
        ),
    )
    val uiState: StateFlow<EnableDetectionUiState> = _uiState.asStateFlow()

    private var pairingWatchJob: Job? = null

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
        pairingWatchJob?.cancel()
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
                    notificationsGranted = areNotificationsEnabled(),
                    devOptionsEnabled = isDevOptionsEnabled(),
                    wifiDebugEnabled = isWifiDebugEnabled(),
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
        if (_uiState.value.method == method) return
        if (method != DetectionMethod.BUILTIN) stopPairingWatch()
        _uiState.update { it.copy(method = method, errorMessage = null) }
        if (method == DetectionMethod.SHIZUKU) refreshShizukuState()
    }

    fun setNotificationsGranted() {
        _uiState.update { it.copy(notificationsGranted = true) }
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
        stopPairingWatch()
        PairingSession.clear()
        PairingNotifier.cancel(context)
        val banking = ElevatedPermissions.canAutoResumeAccessibility(context)
        val visible = LogVisibilityProbe.probe() == LogVisibilityProbe.Result.VISIBLE
        if (!LogcatWatcherService.hasTailSeenNonSelf()) {
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

    // ── Built-In checklist ────────────────────────────────────────────────

    fun openAboutDevice() {
        runCatching {
            context.startActivity(
                Intent(Settings.ACTION_DEVICE_INFO_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure {
            ReadLogsGrantHelper.openDeveloperOptions(context)
        }
    }

    /**
     * Deep link to the Wireless debugging sub-screen itself (not the whole
     * Developer options list). Falls back to Developer options on ROMs that
     * don't resolve the OEM intent.
     */
    fun openWirelessDebugging() {
        ReadLogsGrantHelper.openWirelessDebugging(context)
    }

    /** Opens the Shizuku manager app so the user can start the service. */
    fun openShizukuApp() {
        runCatching {
            val intent = context.packageManager.getLaunchIntentForPackage(ShizukuGrant.SHIZUKU_PACKAGE)
            if (intent != null) {
                context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } else {
                _uiState.update {
                    it.copy(errorMessage = "Shizuku manager not installed. Install it from Play Store or GitHub.")
                }
            }
        }
    }

    /**
     * Step 3: lands the user on the Wireless debugging page (exactly where
     * "Pair device with pairing code" lives), then watches for the temporary
     * pairing service. When mDNS finds it, the floating notification upgrades
     * from "listening" to "enter the 6-digit code".
     */
    fun startPairingWatch() {
        if (_uiState.value.readLogsGranted) return
        pairingWatchJob?.cancel()
        _uiState.update {
            it.copy(
                isWatchingForPairing = true,
                errorMessage = null,
                statusMessage = null,
            )
        }
        PairingNotifier.postWaiting(context)
        // Drop the user on the exact page with the pairing entry point.
        ReadLogsGrantHelper.openWirelessDebugging(context)
        pairingWatchJob = viewModelScope.launch {
            while (isActive && !_uiState.value.readLogsGranted) {
                val endpoint = grantViaWirelessAdb.discoverPairingEndpoint(timeoutMs = 8_000L)
                if (endpoint == null) {
                    if (isActive) delay(1_500L)
                    continue
                }
                PairingSession.set(
                    host = endpoint.host,
                    pairingPort = endpoint.port,
                    connectPort = null,
                )
                PairingNotifier.postPrompt(context, endpoint.port)
                _uiState.update {
                    it.copy(
                        isWatchingForPairing = false,
                        discoveredPort = endpoint.port,
                        statusMessage = "Port ${endpoint.port} detected — enter the code in the notification.",
                    )
                }
                return@launch
            }
        }
    }

    fun stopPairingWatch() {
        pairingWatchJob?.cancel()
        pairingWatchJob = null
        if (_uiState.value.isWatchingForPairing) {
            _uiState.update { it.copy(isWatchingForPairing = false) }
        }
    }

    fun onNearbyWifiDenied() {
        _uiState.update {
            it.copy(
                statusMessage = "Allow the Nearby devices permission so Keyforge can detect the pairing port automatically.",
            )
        }
    }

    // ── Manual ADB ─────────────────────────────────────────────────────────

    fun copyUsbAdbCommand() {
        val clipboard = context.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(
            ClipData.newPlainText("adb command", LogcatWatcherService.ADB_GRANT_COMMAND),
        )
        _uiState.update { it.copy(statusMessage = "ADB commands copied.") }
    }

    // ── System-state probes ────────────────────────────────────────────────

    private fun areNotificationsEnabled(): Boolean =
        context.getSystemService(android.app.NotificationManager::class.java)
            ?.areNotificationsEnabled() ?: true

    private fun isDevOptionsEnabled(): Boolean = runCatching {
        Settings.Global.getInt(
            context.contentResolver,
            Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
            0,
        ) == 1
    }.getOrDefault(false)

    private fun isWifiDebugEnabled(): Boolean = runCatching {
        // Hidden-ish global setting; present since Android 11 (API 30).
        Settings.Global.getInt(context.contentResolver, "adb_wifi_enabled", 0) == 1
    }.getOrDefault(false)
}
