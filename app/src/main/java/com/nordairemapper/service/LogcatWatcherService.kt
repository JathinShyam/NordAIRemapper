package com.nordairemapper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.os.IBinder
import android.util.Log
import com.nordairemapper.R
import com.nordairemapper.domain.model.DetectionStrategy
import com.nordairemapper.domain.repository.SettingsRepository
import com.nordairemapper.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

/**
 * Detection strategy B: tails logcat and matches lines emitted by OnePlus's
 * system key handler (`KEYCODE_ACTION_BUTTON_CLICK` by default). Older builds
 * used `KEYLOG_OplusKeyEventUtil`, which logs several pulses per press.
 * This is the strategy that works on OnePlus devices where the Plus Key never
 * reaches the accessibility key-filtering API.
 *
 * Requires READ_LOGS. Prefer in-app Wireless debugging pair (Enable detection
 * screen). USB fallback:
 *   adb shell pm grant com.nordairemapper android.permission.READ_LOGS
 */
@AndroidEntryPoint
class LogcatWatcherService : Service() {

    @Inject lateinit var keyEventBus: KeyEventBus
    @Inject lateinit var remapEngine: RemapEngine
    @Inject lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var logcatProcess: Process? = null

    /**
     * The one active tail loop. Every settings write used to call [start] again,
     * which spawned an additional concurrent `logcat` process per call; this job
     * reference is the "already watching" guard.
     */
    private var watchJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NOTIFICATION_ID,
            buildNotification(showDetails = true),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        remapEngine.start()

        if (!hasReadLogsPermission(this)) {
            Log.w(TAG, "READ_LOGS not granted; stopping. Grant via: $ADB_GRANT_COMMAND")
            stopSelf()
            return START_NOT_STICKY
        }

        // Exactly one tail loop regardless of how many times start() is called.
        if (watchJob?.isActive != true) {
            watchJob = scope.launch {
                val showDetails = settingsRepository.settings.first().showServiceNotification
                if (!showDetails) {
                    getSystemService(NotificationManager::class.java)
                        .notify(NOTIFICATION_ID, buildNotification(showDetails = false))
                }
                watchLogcat()
            }
        }
        return START_STICKY
    }

    /**
     * Tails logcat forever while the service lives. The pattern is observed, so
     * edits in Lab hot-reload (collectLatest tears down the old process first).
     * If the stream ends on its own (logd restart etc.) we retry with backoff
     * and post the death notification once per outage instead of dying silently.
     */
    private suspend fun watchLogcat() {
        settingsRepository.settings
            .map { LogcatKeyParser.migratePattern(it.logcatPattern) }
            .distinctUntilChanged()
            .collectLatest { pattern ->
                // Persist migration so Developer shows the new default, not the legacy pattern.
                settingsRepository.setLogcatPattern(pattern)
                var backoffMs = RECONNECT_DELAY_MS
                var outageNotified = false
                while (scope.isActive) {
                    val startedAtMs = System.currentTimeMillis()
                    try {
                        tailLogcat(pattern)
                    } catch (t: Throwable) {
                        if (!scope.isActive) break
                        Log.w(TAG, "logcat tail crashed", t)
                    }
                    if (!scope.isActive) break
                    if (!outageNotified) {
                        outageNotified = true
                        ServiceNotifications.notifyDetectionStopped(this@LogcatWatcherService)
                    }
                    delay(backoffMs)
                    backoffMs = (backoffMs * 2).coerceAtMost(MAX_RECONNECT_DELAY_MS)
                    // A long stable run means the outage was resolved; re-arm
                    // so a future outage notifies again.
                    if (System.currentTimeMillis() - startedAtMs > STABLE_RUN_MS) {
                        backoffMs = RECONNECT_DELAY_MS
                        outageNotified = false
                    }
                }
            }
    }

    /** Streams matching logcat lines until EOF, an error, or cancellation. */
    private suspend fun tailLogcat(pattern: String) {
        val process = ProcessBuilder("logcat", "-b", "main", "-T", "1", "-v", "brief")
            .redirectErrorStream(true)
            .start()
        logcatProcess = process
        try {
            val coalescer = LogcatKeyEdgeCoalescer()
            BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
                while (scope.isActive) {
                    val line = reader.readLine() ?: return
                    if (LogcatKeyParser.isSelfLog(line)) continue
                    if (!line.contains(pattern, ignoreCase = true)) continue

                    val action = coalescer.accept(
                        LogcatKeyParser.parseKeyAction(line),
                        System.currentTimeMillis(),
                    ) ?: continue

                    Log.d(TAG, "edge=$action")
                    keyEventBus.emit(
                        RawKeyEvent(
                            keyCode = -1,
                            scanCode = -1,
                            action = action,
                            timestampMs = System.currentTimeMillis(),
                            source = DetectionStrategy.LOGCAT,
                        )
                    )
                }
            }
        } finally {
            if (logcatProcess === process) {
                logcatProcess = null
            }
            runCatching { process.destroy() }
        }
    }

    companion object {
        private const val TAG = "LogcatWatcher"
        private const val CHANNEL_ID = "key_detection"
        private const val NOTIFICATION_ID = 1
        private const val RECONNECT_DELAY_MS = 2_000L
        private const val MAX_RECONNECT_DELAY_MS = 30_000L
        private const val STABLE_RUN_MS = 60_000L

        /** USB paste block: detection + hands-free banking Accessibility pause/resume. */
        val ADB_GRANT_COMMAND: String =
            ElevatedPermissions.UNLOCK_SHELL_COMMANDS.joinToString("\n") { "adb shell $it" }

        fun hasReadLogsPermission(context: Context): Boolean =
            context.checkSelfPermission(android.Manifest.permission.READ_LOGS) ==
                PackageManager.PERMISSION_GRANTED

        fun start(context: Context) {
            context.startForegroundService(Intent(context, LogcatWatcherService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LogcatWatcherService::class.java))
        }
    }

    private fun buildNotification(showDetails: Boolean): Notification {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Key detection",
                NotificationManager.IMPORTANCE_MIN,
            )
        )
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        // FGS notification is required while the watcher runs. When the user
        // turns Service notification off, keep a minimal silent ongoing entry.
        val builder = Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(contentIntent)
            .setOngoing(true)
        return if (showDetails) {
            builder
                .setContentTitle("Plus Key detection active")
                .setContentText("Watching for Plus Key presses")
                .build()
        } else {
            builder
                .setContentTitle("Keyforge")
                .setContentText("Running")
                .build()
        }
    }

    override fun onDestroy() {
        logcatProcess?.destroy()
        scope.cancel()
        watchJob = null
        ServiceNotifications.notifyDetectionStopped(this)
        super.onDestroy()
    }
}
