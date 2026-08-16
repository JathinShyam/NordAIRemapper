package com.nordairemapper.service.adb

import android.content.Context
import android.util.Log
import com.nordairemapper.domain.repository.SettingsRepository
import com.nordairemapper.service.DetectionCoordinator
import com.nordairemapper.service.LogcatWatcherService
import com.nordairemapper.service.ReadLogsGrantHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.muntashirakon.adb.android.AdbMdns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * One-time in-app Wireless Debugging flow that grants only
 * `android.permission.READ_LOGS` via loopback ADB — no Shizuku, no laptop.
 */
@Singleton
class ReadLogsGrantViaWirelessAdb @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) {

    data class DiscoveredEndpoint(
        val host: String,
        val port: Int,
    )

    sealed class GrantResult {
        data object AlreadyGranted : GrantResult()
        data object Success : GrantResult()
        data class Failed(val message: String) : GrantResult()
    }

    fun hasReadLogs(): Boolean = LogcatWatcherService.hasReadLogsPermission(context)

    /**
     * Discover the Wireless Debugging TLS pairing endpoint advertised while
     * "Pair device with pairing code" is open.
     */
    suspend fun discoverPairingEndpoint(
        timeoutMs: Long = PAIRING_DISCOVERY_TIMEOUT_MS,
    ): DiscoveredEndpoint? = withContext(Dispatchers.IO) {
        withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                lateinit var mdns: AdbMdns
                mdns = AdbMdns(
                    context,
                    AdbMdns.SERVICE_TYPE_TLS_PAIRING,
                ) { host: InetAddress?, port: Int ->
                    if (host != null && port > 0 && cont.isActive) {
                        val endpoint = DiscoveredEndpoint(
                            host = host.hostAddress?.substringBefore('%') ?: host.hostName,
                            port = port,
                        )
                        mdns.stop()
                        cont.resume(endpoint) {}
                    }
                }
                mdns.start()
                cont.invokeOnCancellation { mdns.stop() }
            }
        }
    }

    /**
     * Pair with Wireless Debugging, connect, run the single `pm grant` command,
     * verify permission, then start the logcat watcher when remapping is enabled.
     *
     * @param pairingCode six-digit code from the system pairing dialog
     * @param host optional override (defaults to discovered / loopback IP)
     * @param pairingPort optional override when mDNS did not find the pairing port
     */
    suspend fun pairAndGrant(
        pairingCode: String,
        host: String? = null,
        pairingPort: Int? = null,
    ): GrantResult = withContext(Dispatchers.IO) {
        if (hasReadLogs()) {
            syncWatcherAfterGrant()
            return@withContext GrantResult.AlreadyGranted
        }

        val code = pairingCode.trim()
        if (!code.matches(PAIRING_CODE_REGEX)) {
            return@withContext GrantResult.Failed("Enter the 6-digit pairing code from Wireless debugging.")
        }

        val manager = NordAdbConnectionManager.getInstance(context)
        try {
            val endpoint = when {
                host != null && pairingPort != null && pairingPort > 0 ->
                    DiscoveredEndpoint(host.trim(), pairingPort)
                pairingPort != null && pairingPort > 0 ->
                    DiscoveredEndpoint(host?.trim().orEmpty().ifEmpty { DEFAULT_HOST }, pairingPort)
                else -> discoverPairingEndpoint()
                    ?: return@withContext GrantResult.Failed(
                        "Could not find the pairing port. Open “Pair device with pairing code”, " +
                            "keep that screen open, and try again — or enter the port shown under the code.",
                    )
            }

            manager.hostAddress = endpoint.host
            Log.i(TAG, "Pairing with ${endpoint.host}:${endpoint.port}")
            val paired = manager.pair(endpoint.host, endpoint.port, code)
            if (!paired) {
                return@withContext GrantResult.Failed("Pairing failed. Check the code and try again.")
            }

            // After pairing, connect to the TLS connect service (not the pairing port).
            val connected = manager.connectTls(context, CONNECT_TIMEOUT_MS) ||
                manager.autoConnect(context, CONNECT_TIMEOUT_MS)
            if (!connected) {
                return@withContext GrantResult.Failed(
                    "Paired, but could not connect. Keep Wireless debugging on and try again.",
                )
            }

            runGrantCommand(manager)

            if (!hasReadLogs()) {
                return@withContext GrantResult.Failed(
                    "ADB ran the grant command, but READ_LOGS is still missing. Retry or use USB ADB.",
                )
            }

            syncWatcherAfterGrant()
            GrantResult.Success
        } catch (e: Exception) {
            Log.w(TAG, "Wireless ADB grant failed", e)
            GrantResult.Failed(e.message?.takeIf { it.isNotBlank() } ?: "Grant failed: ${e.javaClass.simpleName}")
        } finally {
            runCatching { manager.disconnect() }
        }
    }

    /**
     * Re-check permission and start the watcher without pairing (e.g. after USB grant).
     */
    suspend fun verifyAndSyncWatcher(): Boolean = withContext(Dispatchers.IO) {
        val ok = hasReadLogs()
        if (ok) syncWatcherAfterGrant()
        ok
    }

    private fun runGrantCommand(manager: NordAdbConnectionManager) {
        val destination = "shell:${ReadLogsGrantHelper.ON_DEVICE_SHELL_COMMAND}"
        manager.openStream(destination).use { stream ->
            val input = stream.openInputStream()
            val buffer = ByteArray(4096)
            while (true) {
                val read = input.read(buffer)
                if (read <= 0) break
                val chunk = String(buffer, 0, read, StandardCharsets.UTF_8)
                Log.d(TAG, "shell: $chunk")
            }
        }
    }

    private suspend fun syncWatcherAfterGrant() {
        val settings = settingsRepository.settings.first()
        DetectionCoordinator.syncLogcatWatcher(
            context = context,
            strategy = settings.detectionStrategy,
            serviceEnabled = settings.serviceEnabled,
        )
    }

    companion object {
        private const val TAG = "ReadLogsWirelessAdb"
        private const val DEFAULT_HOST = "127.0.0.1"
        private const val PAIRING_DISCOVERY_TIMEOUT_MS = 12_000L
        private const val CONNECT_TIMEOUT_MS = 10_000L
        private val PAIRING_CODE_REGEX = Regex("^\\d{6}$")
    }
}
