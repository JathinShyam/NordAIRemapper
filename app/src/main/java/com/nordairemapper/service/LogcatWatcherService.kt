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
        val pattern = LogcatKeyParser.migratePattern(
            settingsRepository.settings.first().logcatPattern,
        )
        // Persist migration so Developer shows the new default, not the legacy pattern.
        settingsRepository.setLogcatPattern(pattern)
        val process = ProcessBuilder("logcat", "-b", "main", "-T", "1", "-v", "brief")
            .redirectErrorStream(true)
            .start()
        logcatProcess = process

        val coalescer = LogcatKeyEdgeCoalescer()
        BufferedReader(InputStreamReader(process.inputStream)).use { reader ->
            while (scope.isActive) {
                val line = reader.readLine() ?: break
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
    }

    companion object {
        private const val TAG = "LogcatWatcher"
        private const val CHANNEL_ID = "key_detection"
        private const val NOTIFICATION_ID = 1

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
