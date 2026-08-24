package com.nordairemapper.service.adb

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.util.Log
import com.nordairemapper.domain.repository.SettingsRepository
import com.nordairemapper.service.DetectionCoordinator
import com.nordairemapper.service.ElevatedPermissions
import com.nordairemapper.service.LogcatWatcherService
import com.nordairemapper.service.ReadLogsGrantHelper
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.muntashirakon.adb.android.AdbMdns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.net.Inet4Address
import java.net.InetAddress
import java.nio.charset.StandardCharsets
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * One-time in-app Wireless Debugging flow that grants elevated shell perms
 * via loopback ADB — no Shizuku, no laptop.
 *
 * Grants [ElevatedPermissions.UNLOCK_SHELL_COMMANDS]: READ_LOGS (Plus Key
 * logcat), WRITE_SECURE_SETTINGS + usage stats (hands-free banking
 * Accessibility pause/resume).
 *
 * Pairing uses the temporary pairing port; connecting uses a *different*
 * TLS connect port from the Wireless debugging detail page (IP address & port).
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

    suspend fun discoverPairingEndpoint(
        timeoutMs: Long = PAIRING_DISCOVERY_TIMEOUT_MS,
    ): DiscoveredEndpoint? = discoverMdnsEndpoint(AdbMdns.SERVICE_TYPE_TLS_PAIRING, timeoutMs)

    suspend fun discoverConnectEndpoint(
        timeoutMs: Long = CONNECT_DISCOVERY_TIMEOUT_MS,
    ): DiscoveredEndpoint? = discoverMdnsEndpoint(AdbMdns.SERVICE_TYPE_TLS_CONNECT, timeoutMs)

    private suspend fun discoverMdnsEndpoint(
        serviceType: String,
        timeoutMs: Long,
    ): DiscoveredEndpoint? = withContext(Dispatchers.IO) {
        withTimeoutOrNull(timeoutMs) {
            suspendCancellableCoroutine { cont ->
                lateinit var mdns: AdbMdns
                mdns = AdbMdns(context, serviceType) { host: InetAddress?, port: Int ->
                    if (host != null && port > 0 && cont.isActive) {
                        val endpoint = DiscoveredEndpoint(
                            host = normalizeHost(host.hostAddress ?: host.hostName),
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
     * @param pairingCode six-digit code from the pairing dialog
     * @param host Wi‑Fi IP from the pairing dialog (preferred over 127.0.0.1)
     * @param pairingPort temporary port under the pairing code
     * @param connectPort port from Wireless debugging main page “IP address & port”
     *   (different from [pairingPort])
     */
    suspend fun pairAndGrant(
        pairingCode: String,
        host: String? = null,
        pairingPort: Int? = null,
        connectPort: Int? = null,
    ): GrantResult = withContext(Dispatchers.IO) {
        // Re-run Unlock when READ_LOGS exists but banking auto-resume grants are still missing.
        if (hasReadLogs() && ElevatedPermissions.canAutoResumeAccessibility(context)) {
            syncWatcherAfterGrant()
            return@withContext GrantResult.AlreadyGranted
        }

        val code = pairingCode.trim()
        if (!code.matches(PAIRING_CODE_REGEX)) {
            return@withContext GrantResult.Failed("Enter the 6-digit pairing code from Wireless debugging.")
        }

        val manager = NordAdbConnectionManager.getInstance(context)
        try {
            val wifiIp = resolveWifiIpv4()
            val pairingEndpoint = when {
                host != null && pairingPort != null && pairingPort > 0 ->
                    DiscoveredEndpoint(normalizeHost(host), pairingPort)
                pairingPort != null && pairingPort > 0 ->
                    DiscoveredEndpoint(
                        normalizeHost(host).ifEmpty { wifiIp ?: DEFAULT_HOST },
                        pairingPort,
                    )
                else -> discoverPairingEndpoint()
                    ?: return@withContext GrantResult.Failed(
                        "Could not find the pairing port. Keep “Pair device with pairing code” open " +
                            "and enter the port after the colon under the code.",
                    )
            }

            manager.hostAddress = pairingEndpoint.host
            Log.i(TAG, "Pairing with ${pairingEndpoint.host}:${pairingEndpoint.port}")
            val paired = try {
                manager.pair(pairingEndpoint.host, pairingEndpoint.port, code)
            } catch (e: Exception) {
                Log.w(TAG, "pair() threw", e)
                return@withContext GrantResult.Failed(
                    "Pairing failed: ${e.message ?: e.javaClass.simpleName}. " +
                        "Use a fresh pairing code and keep the pairing dialog open.",
                )
            }
            if (!paired) {
                return@withContext GrantResult.Failed(
                    "Pairing failed. Generate a new pairing code and try again.",
                )
            }

            // Pairing port is temporary. Connect uses the Wireless debugging page port.
            delay(POST_PAIR_DELAY_MS)
            val connected = connectAfterPair(
                manager = manager,
                preferredHost = pairingEndpoint.host,
                wifiIp = wifiIp,
                connectPort = connectPort,
            )
            if (!connected) {
                return@withContext GrantResult.Failed(
                    "Paired, but could not connect. On the Wireless debugging page (not the pairing " +
                        "dialog), note “IP address & port”, enter that port in Connection port, " +
                        "keep Wireless debugging on, and try again.",
                )
            }

            runGrantsVerifying(manager)

            val readOk = hasReadLogs()
            val bankingOk = ElevatedPermissions.canAutoResumeAccessibility(context)
            when {
                readOk && bankingOk -> {
                    syncWatcherAfterGrant()
                    GrantResult.Success
                }
                readOk -> {
                    // Detection works, but OxygenOS silently drops the
                    // security-sensitive grants over WIRELESS adb unless
                    // "USB debugging (Security settings)" is on.
                    syncWatcherAfterGrant()
                    GrantResult.Failed(
                        "Detection unlocked — but OxygenOS blocked the banking permissions over " +
                            "Wireless debugging. In Developer options turn ON “USB debugging " +
                            "(Security settings)” (confirm if asked), then tap Pair now again.",
                    )
                }
                else -> GrantResult.Failed(
                    "ADB ran the grant command, but READ_LOGS is still missing. Retry or use Manual ADB.",
                )
            }
        } catch (e: Exception) {
            Log.w(TAG, "Wireless ADB grant failed", e)
            val detail = e.message?.takeIf { it.isNotBlank() } ?: e.javaClass.simpleName
            GrantResult.Failed(
                "Failed to connect ($detail). After pairing, enter the Connection port from the " +
                    "Wireless debugging page IP address & port (not the pairing port).",
            )
        } finally {
            runCatching { manager.disconnect() }
        }
    }

    private suspend fun connectAfterPair(
        manager: NordAdbConnectionManager,
        preferredHost: String,
        wifiIp: String?,
        connectPort: Int?,
    ): Boolean {
        // 1) Manual connection port from Wireless debugging detail page.
        if (connectPort != null && connectPort > 0) {
            val hosts = linkedSetOf(preferredHost, wifiIp, DEFAULT_HOST).filterNotNull()
            for (h in hosts) {
                if (tryConnect(manager, h, connectPort)) return true
            }
        }

        // 2) mDNS TLS connect (adb-tls-connect) — different service from pairing.
        val discovered = discoverConnectEndpoint()
        if (discovered != null && tryConnect(manager, discovered.host, discovered.port)) {
            return true
        }

        // 3) Library helpers (also mDNS-based).
        for (attempt in 1..CONNECT_ATTEMPTS) {
            Log.i(TAG, "connectTls/autoConnect attempt $attempt")
            val ok = runCatching {
                manager.connectTls(context, CONNECT_TIMEOUT_MS) ||
                    manager.autoConnect(context, CONNECT_TIMEOUT_MS)
            }.onFailure { Log.w(TAG, "connect attempt $attempt failed", it) }
                .getOrDefault(false)
            if (ok) return true
            delay(POST_PAIR_DELAY_MS)
        }

        // 4) Last resort: same hosts with discovered port only if we got one mid-retry.
        return false
    }

    private fun tryConnect(manager: NordAdbConnectionManager, host: String, port: Int): Boolean {
        return runCatching {
            manager.hostAddress = host
            Log.i(TAG, "Connecting to $host:$port")
            manager.connect(host, port)
        }.onFailure { Log.w(TAG, "connect($host, $port) failed", it) }
            .getOrDefault(false)
    }

    private fun resolveWifiIpv4(): String? {
        val cm = context.getSystemService(ConnectivityManager::class.java) ?: return null
        val network = cm.activeNetwork ?: return null
        val props: LinkProperties = cm.getLinkProperties(network) ?: return null
        return props.linkAddresses
            .asSequence()
            .map { it.address }
            .filterIsInstance<Inet4Address>()
            .map { it.hostAddress }
            .firstOrNull { !it.isNullOrBlank() && it != "127.0.0.1" }
    }

    suspend fun verifyAndSyncWatcher(): Boolean = withContext(Dispatchers.IO) {
        val ok = hasReadLogs()
        if (ok) syncWatcherAfterGrant()
        ok
    }

    /**
     * Runs each Unlock command independently and VERIFIES it took effect
     * in-process (all three are readable via CheckSelfPermission/AppOps),
     * retrying up to [GRANT_VERIFY_ATTEMPTS] times. Every attempt is appended
     * to filesDir/unlock_grants.log so an OEM that silently drops security-
     * sensitive ops over Wireless adb leaves hard evidence behind.
     */
    private fun runGrantsVerifying(manager: NordAdbConnectionManager) {
        val logFile = File(context.filesDir, "unlock_grants.log")
        fun log(s: String) = runCatching {
            logFile.appendText("${SimpleDateFormat("HH:mm:ss.SSS", Locale.US).format(Date())} $s\n")
        }

        for (command in ReadLogsGrantHelper.ON_DEVICE_SHELL_COMMANDS) {
            Log.i(TAG, "Running: $command")
            log("RUN $command")
            var verified = false
            for (attempt in 1..GRANT_VERIFY_ATTEMPTS) {
                runCatching {
                    manager.openStream("shell:$command").use { stream ->
                        // RCA: silent commands (pm grant prints nothing) never
                        // deliver EOF promptly over TLS — a blocking drain hung
                        // forever and killed the grant flow after command #1.
                        // Bounded non-blocking read instead; output is optional,
                        // verification below is authoritative.
                        val input = stream.openInputStream()
                        val buffer = ByteArray(4096)
                        val out = StringBuilder()
                        val deadline = android.os.SystemClock.elapsedRealtime() +
                            SHELL_OUTPUT_WINDOW_MS
                        while (android.os.SystemClock.elapsedRealtime() < deadline) {
                            val n = runCatching { input.available() }.getOrDefault(0)
                            if (n > 0) {
                                val r = input.read(buffer, 0, minOf(n, buffer.size))
                                if (r <= 0) break
                                out.append(String(buffer, 0, r, StandardCharsets.UTF_8))
                            } else {
                                Thread.sleep(SHELL_OUTPUT_POLL_MS)
                            }
                        }
                        if (out.isNotBlank()) log("OUT $out")
                    }
                }.onFailure {
                    log("ERR attempt=$attempt ${it.message}")
                    Log.w(TAG, "Command failed (continuing): $command", it)
                }
                Thread.sleep(GRANT_VERIFY_DELAY_MS)
                verified = verifyCommand(command)
                log("attempt=$attempt verified=$verified")
                if (verified) break
            }
            if (!verified) log("NOT APPLIED: $command")
        }
    }

    /** In-process truth for whichever permission a given shell command grants. */
    private fun verifyCommand(command: String): Boolean = when {
        command.contains("READ_LOGS") ->
            hasReadLogs()
        command.contains("WRITE_SECURE") ->
            ElevatedPermissions.hasWriteSecureSettings(context)
        command.contains("GET_USAGE_STATS") ->
            ElevatedPermissions.hasUsageAccess(context)
        else -> true
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
        private const val CONNECT_DISCOVERY_TIMEOUT_MS = 12_000L
        private const val CONNECT_TIMEOUT_MS = 8_000L
        private const val POST_PAIR_DELAY_MS = 1_200L
        private const val CONNECT_ATTEMPTS = 3
        private const val GRANT_VERIFY_ATTEMPTS = 3
        private const val GRANT_VERIFY_DELAY_MS = 400L
        private const val SHELL_OUTPUT_WINDOW_MS = 300L
        private const val SHELL_OUTPUT_POLL_MS = 40L
        private val PAIRING_CODE_REGEX = Regex("^\\d{6}$")

        fun normalizeHost(host: String?): String =
            host?.trim()?.substringBefore('%').orEmpty()
    }
}
