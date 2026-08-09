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
        // -T 1: only new lines; avoids replaying old key presses on start
        val process = ProcessBuilder("logcat", "-T", "1", "-v", "brief")
            .redirectErrorStream(true)
            .start()
        logcatProcess = process

        var lastPulseAtMs = 0L
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            while (scope.isActive) {
                val line = reader.readLine() ?: break
                if (!line.contains(pattern, ignoreCase = true)) continue

                val now = System.currentTimeMillis()
                val lower = line.lowercase()
                val action = when {
                    "down" in lower -> KeyAction.DOWN
                    "up" in lower -> KeyAction.UP
                    else -> KeyAction.PULSE
                }
                // The log may print several lines per press; debounce pulses.
                if (action == KeyAction.PULSE && now - lastPulseAtMs < PULSE_DEBOUNCE_MS) continue
                if (action == KeyAction.PULSE) lastPulseAtMs = now

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
        super.onDestroy()
    }

    companion object {
        private const val TAG = "LogcatWatcher"
        private const val CHANNEL_ID = "key_detection"
        private const val NOTIFICATION_ID = 1
        private const val PULSE_DEBOUNCE_MS = 150L

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
    }
}
