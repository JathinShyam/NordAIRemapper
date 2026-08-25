package com.nordairemapper.service.adb

import android.app.Notification
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.nordairemapper.R
import com.nordairemapper.presentation.MainActivity
import com.nordairemapper.service.LogVisibilityProbe
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Completes the Built-In pairing + grant flow OUTSIDE the broadcast grace
 * window.
 *
 * RCA 2026-08-25: [PairingReplyReceiver] ran the whole pair → connect →
 * three-grant sequence inside `goAsync()`. The ~10s broadcast timeout killed
 * the process right after grant command #1: READ_LOGS applied (adbd had
 * executed it), WRITE_SECURE_SETTINGS + usage access never sent, no result
 * banner — a silent partial failure. `unlock_grants.log` ended at
 * "RUN pm grant …READ_LOGS" with no attempt line.
 *
 * A shortService FGS is exempt from background-start limits and gives ~3
 * minutes: full mDNS timeouts, three verified grants, and a watchdog all fit.
 */
@AndroidEntryPoint
class PairingGrantService : Service() {

    @Inject lateinit var grantViaWirelessAdb: ReadLogsGrantViaWirelessAdb

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // FGS contract: startForeground unconditionally, even for bad input.
        startInForeground()

        val code = intent?.getStringExtra(EXTRA_CODE).orEmpty().filter { it.isDigit() }
        val host = intent?.getStringExtra(EXTRA_HOST)
        val pairingPort = intent?.getIntExtra(EXTRA_PAIRING_PORT, -1)?.takeIf { it > 0 }
        val connectPort = intent?.getIntExtra(EXTRA_CONNECT_PORT, -1)?.takeIf { it > 0 }

        if (code.length != 6) {
            Log.w(TAG, "Missing 6-digit code; stopping")
            detachAndStop()
            return START_NOT_STICKY
        }

        scope.launch {
            Log.i(TAG, "Running pair+grant (host=$host pairingPort=$pairingPort connectPort=$connectPort)")
            val result = try {
                withTimeoutOrNull(GRANT_TIMEOUT_MS) {
                    grantViaWirelessAdb.pairAndGrant(
                        pairingCode = code,
                        host = host,
                        pairingPort = pairingPort,
                        connectPort = connectPort,
                        quick = false,
                    )
                } ?: ReadLogsGrantViaWirelessAdb.GrantResult.Failed(
                    "Pairing took too long and was stopped. Toggle Wireless debugging off/on, " +
                        "then tap Pair now again.",
                )
            } catch (t: Throwable) {
                Log.w(TAG, "pairAndGrant crashed", t)
                ReadLogsGrantViaWirelessAdb.GrantResult.Failed(
                    t.message?.takeIf { it.isNotBlank() } ?: t.javaClass.simpleName,
                )
            }
            handleResult(applicationContext, result)
            detachAndStop()
        }
        return START_NOT_STICKY
    }

    private fun startInForeground() {
        PairingNotifier.ensureChannel(this)
        val notification = Notification.Builder(this, PairingNotifier.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Finishing pairing…")
            .setContentText("Applying Unlock grants — this takes a few seconds.")
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startForeground(
                PairingNotifier.NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SHORT_SERVICE,
            )
        } else {
            @Suppress("DEPRECATION")
            startForeground(PairingNotifier.NOTIFICATION_ID, notification)
        }
    }

    private fun detachAndStop() {
        // DETACH keeps the result banner (posted to the same id) alive after
        // the service goes away.
        runCatching { stopForeground(STOP_FOREGROUND_DETACH) }
        stopSelf()
    }

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "PairingGrant"
        private const val EXTRA_CODE = "code"
        private const val EXTRA_HOST = "host"
        private const val EXTRA_PAIRING_PORT = "pairing_port"
        private const val EXTRA_CONNECT_PORT = "connect_port"

        /** Well under the shortService ~3-minute cap; normal runs take <30s. */
        private const val GRANT_TIMEOUT_MS = 150_000L

        fun intent(
            context: Context,
            code: String,
            host: String?,
            pairingPort: Int?,
            connectPort: Int?,
        ): Intent = Intent(context, PairingGrantService::class.java)
            .putExtra(EXTRA_CODE, code)
            .putExtra(EXTRA_HOST, host)
            .putExtra(EXTRA_PAIRING_PORT, pairingPort ?: -1)
            .putExtra(EXTRA_CONNECT_PORT, connectPort ?: -1)

        /**
         * Shared completion UX for every Built-In path (FGS and the legacy
         * API-33 broadcast fallback): honest log-visibility check, result
         * banner, relaunch into the app on success, session cleanup.
         */
        suspend fun handleResult(context: Context, result: ReadLogsGrantViaWirelessAdb.GrantResult) {
            when (result) {
                is ReadLogsGrantViaWirelessAdb.GrantResult.AlreadyGranted,
                is ReadLogsGrantViaWirelessAdb.GrantResult.Success,
                -> {
                    // Honest success: verify logd delivers other apps' logs
                    // (post-boot consent can leave a fresh tail blind).
                    val blind = runCatching {
                        LogVisibilityProbe.probe() == LogVisibilityProbe.Result.BLIND
                    }.getOrDefault(false)
                    if (blind) {
                        PairingNotifier.postResult(
                            context,
                            ok = false,
                            "Grants applied, but OxygenOS is still filtering system logs. " +
                                "Open Keyforge and allow log access when prompted.",
                        )
                    } else {
                        // Bring the user back into the app — allowed because
                        // Keyforge usually holds SYSTEM_ALERT_WINDOW; if
                        // blocked, the result banner taps through.
                        runCatching {
                            context.startActivity(
                                Intent(context, MainActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                            )
                        }
                        PairingNotifier.postResult(
                            context,
                            ok = true,
                            "You're all set — assign presses on Home.",
                        )
                    }
                }
                is ReadLogsGrantViaWirelessAdb.GrantResult.Failed ->
                    PairingNotifier.postResult(context, ok = false, result.message)
            }
            // Endpoints are one-shot; never leave a stale session behind.
            PairingSession.clear()
        }
    }
}
