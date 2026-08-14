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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.BufferedReader
import java.io.InputStreamReader
import javax.inject.Inject

/**
 * Detection strategy B: tails logcat and matches lines emitted by OnePlus's
 * system key handler (KEYLOG_OplusKeyEventUtil by default). This is the
 * strategy that works on OnePlus devices where the Plus Key never reaches
 * the accessibility key-filtering API.
 *
 * Requires READ_LOGS, grantable only via ADB:
 *   adb shell pm grant com.nordairemapper android.permission.READ_LOGS
 */
@AndroidEntryPoint
class LogcatWatcherService : Service() {

    @Inject lateinit var keyEventBus: KeyEventBus
    @Inject lateinit var remapEngine: RemapEngine
    @Inject lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var logcatProcess: Process? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
        )
        remapEngine.start()

        if (!hasReadLogsPermission(this)) {
            Log.w(TAG, "READ_LOGS not granted; stopping. Grant via: $ADB_GRANT_COMMAND")
            stopSelf()
            return START_NOT_STICKY
        }

        scope.launch { watchLogcat() }
        return START_STICKY
    }

    private suspend fun watchLogcat() {
        val pattern = settingsRepository.settings.first().logcatPattern
        val process = ProcessBuilder("logcat", "-T", "1", "-v", "brief")
            .redirectErrorStream(true)
            .start()
        logcatProcess = process

        var pressed = false
        var lastEmittedAtMs = 0L
        var lastEmitted: KeyAction? = null
        var downAtMs = 0L
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            while (scope.isActive) {
                val line = reader.readLine() ?: break
                if (!line.contains(pattern, ignoreCase = true)) continue

                val now = System.currentTimeMillis()
                val parsed = parseKeyAction(line)
                val action = when (parsed) {
                    KeyAction.DOWN -> if (pressed) null else KeyAction.DOWN
                    KeyAction.UP -> if (pressed) KeyAction.UP else null
                    KeyAction.PULSE -> when {
                        !pressed -> KeyAction.DOWN
                        // Same-press echo (ACTION_DOWN + KEYLOG a few ms later)
                        now - downAtMs < ECHO_DEBOUNCE_MS -> null
                        else -> KeyAction.UP
                    }
                } ?: continue

                // Collapse duplicate lines for the same physical edge
                if (action == lastEmitted && now - lastEmittedAtMs < EDGE_DEBOUNCE_MS) continue

                if (action == KeyAction.DOWN) {
                    pressed = true
                    downAtMs = now
                } else {
                    pressed = false
                }
                lastEmitted = action
                lastEmittedAtMs = now

                Log.d(TAG, "edge=$action from: ${line.take(180)}")
                keyEventBus.emit(
                    RawKeyEvent(
                        keyCode = -1,
                        scanCode = -1,
                        action = action,
                        timestampMs = now,
                        source = DetectionStrategy.LOGCAT,
                    )
                )
            }
        }
    }

    companion object {
        private const val TAG = "LogcatWatcher"
        private const val CHANNEL_ID = "key_detection"
        private const val NOTIFICATION_ID = 1
        /** Collapse duplicate log lines for the same physical edge. */
        private const val EDGE_DEBOUNCE_MS = 40L
        /** Ignore KEYLOG echo that follows ACTION_DOWN on the same press. */
        private const val ECHO_DEBOUNCE_MS = 40L

        const val ADB_GRANT_COMMAND =
            "adb shell pm grant com.nordairemapper android.permission.READ_LOGS"

        fun hasReadLogsPermission(context: Context): Boolean =
            context.checkSelfPermission(android.Manifest.permission.READ_LOGS) ==
                PackageManager.PERMISSION_GRANTED

        fun start(context: Context) {
            context.startForegroundService(Intent(context, LogcatWatcherService::class.java))
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, LogcatWatcherService::class.java))
        }

        internal fun parseKeyAction(line: String): KeyAction {
            val lower = line.lowercase()
            return when {
                "action_down" in lower || "action=0" in lower -> KeyAction.DOWN
                "action_up" in lower || "action=1" in lower -> KeyAction.UP
                Regex("""(?<![a-z])down(?![a-z])""").containsMatchIn(lower) -> KeyAction.DOWN
                Regex("""(?<![a-z])up(?![a-z])""").containsMatchIn(lower) -> KeyAction.UP
                else -> KeyAction.PULSE
            }
        }
    }

    private fun buildNotification(): Notification {
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
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Plus Key detection active")
            .setContentText("Watching for Plus Key presses")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        logcatProcess?.destroy()
        scope.cancel()
        ServiceNotifications.notifyDetectionStopped(this)
        super.onDestroy()
    }
}
