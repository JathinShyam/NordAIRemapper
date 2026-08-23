package com.nordairemapper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import com.nordairemapper.R
import com.nordairemapper.domain.repository.SettingsRepository
import com.nordairemapper.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * After Accessibility is soft-disabled for an excluded app, this FGS watches
 * Usage Stats and turns Keyforge Accessibility back on once no excluded app is
 * in the foreground — no manual Settings trip for daily UPI use.
 *
 * Switching directly between two excluded apps hands the watch off to the new
 * app without ever restoring Accessibility in between.
 */
@AndroidEntryPoint
class AccessibilityAutoResumeService : Service() {

    @Inject lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var watchJob: Job? = null

    @Volatile private var excludedApps: Set<String> = emptySet()

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val pausedPkg = intent?.getStringExtra(EXTRA_PAUSED_PACKAGE).orEmpty()
        val label = intent?.getStringExtra(EXTRA_APP_LABEL).orEmpty().ifBlank { pausedPkg }
        if (pausedPkg.isBlank()) {
            stopSelf()
            return START_NOT_STICKY
        }
        startAsForeground(label)
        watchJob?.cancel()
        watchJob = scope.launch {
            // Keep the exclusion set live so handoffs use the latest list.
            launch {
                settingsRepository.settings.collect { excludedApps = it.excludedApps }
            }
            watchUntilLeft(pausedPkg)
        }
        return START_STICKY
    }

    private suspend fun watchUntilLeft(initialPkg: String) {
        var pausedPkg = initialPkg
        // Let the banking app settle as the reported foreground package.
        delay(INITIAL_GRACE_MS)
        var leftStreak = 0
        var prevForeground: String? = null
        val deadline = System.currentTimeMillis() + MAX_WATCH_MS
        while (scope.isActive) {
            if (System.currentTimeMillis() > deadline) {
                Log.w(TAG, "Watch timeout; asking user to re-enable manually")
                ServiceNotifications.notifyAccessibilityPaused(this, pausedPkg)
                stopSelf()
                return
            }
            val foreground = foregroundPackage()
            when {
                foreground == pausedPkg -> leftStreak = 0
                foreground != null && foreground !in TRANSIENT_PACKAGES -> leftStreak++
                // Ignore null/unknown/transient samples; only clear "another app" counts.
                else -> Unit
            }
            if (leftStreak >= RESUME_STABLE_SAMPLES && foreground != null && foreground == prevForeground) {
                if (foreground != pausedPkg && foreground in excludedApps) {
                    // Landed straight in another excluded app — keep Accessibility off
                    // and watch that one instead. No restore flicker between them.
                    Log.i(TAG, "Handoff $pausedPkg -> $foreground (also excluded)")
                    pausedPkg = foreground
                    updatePausedNotification(appLabelFor(pausedPkg))
                    leftStreak = 0
                    delay(INITIAL_GRACE_MS)
                } else {
                    Log.i(TAG, "Left $pausedPkg (now=$foreground); restoring Accessibility")
                    val ok = AccessibilitySecureToggle.setEnabled(this, enabled = true)
                    if (!ok) {
                        ServiceNotifications.notifyAccessibilityPaused(this, pausedPkg)
                    } else {
                        ServiceNotifications.clearAccessibilityPaused(this)
                    }
                    stopSelf()
                    return
                }
            }
            prevForeground = foreground
            delay(POLL_MS)
        }
    }

    private fun appLabelFor(pkg: String): String = runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
    }.getOrDefault(pkg)

    private fun foregroundPackage(): String? = runCatching {
        val usm = getSystemService(UsageStatsManager::class.java) ?: return null
        val end = System.currentTimeMillis()
        val begin = end - EVENT_WINDOW_MS
        val events = usm.queryEvents(begin, end) ?: return null
        val event = UsageEvents.Event()
        var last: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val type = event.eventType
            if (type == UsageEvents.Event.MOVE_TO_FOREGROUND ||
                type == UsageEvents.Event.ACTIVITY_RESUMED
            ) {
                last = event.packageName
            }
        }
        last
    }.onFailure { Log.w(TAG, "Usage query failed", it) }.getOrDefault(null)

    private fun buildPausedNotification(appLabel: String): Notification {
        val contentIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Paused for $appLabel")
            .setContentText("Accessibility turns back on automatically when you leave.")
            .setContentIntent(contentIntent)
            .setOngoing(true)
            .build()
    }

    private fun startAsForeground(appLabel: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Banking pause",
                NotificationManager.IMPORTANCE_LOW,
            ),
        )
        val notification = buildPausedNotification(appLabel)
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
            )
        } else {
            startForeground(NOTIFICATION_ID, notification)
        }
    }

    /** Refresh the "Paused for X" title when handing off between excluded apps. */
    private fun updatePausedNotification(appLabel: String) {
        getSystemService(NotificationManager::class.java)
            .notify(NOTIFICATION_ID, buildPausedNotification(appLabel))
    }

    override fun onDestroy() {
        watchJob?.cancel()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "A11yAutoResume"
        private const val CHANNEL_ID = "accessibility_auto_resume"
        private const val NOTIFICATION_ID = 101
        private const val EXTRA_PAUSED_PACKAGE = "paused_package"
        private const val EXTRA_APP_LABEL = "app_label"
        private const val POLL_MS = 750L
        private const val INITIAL_GRACE_MS = 1_200L
        private const val EVENT_WINDOW_MS = 15_000L
        private const val RESUME_STABLE_SAMPLES = 2

        /** Safety cap so the FGS never polls forever if "left" is never detected. */
        private const val MAX_WATCH_MS = 6 * 60 * 60 * 1000L

        /** Overlays / system UI that should not count as "left the banking app". */
        private val TRANSIENT_PACKAGES = setOf(
            "com.android.systemui",
            "android",
            "com.android.intentresolver",
            "com.google.android.permissioncontroller",
            "com.android.permissioncontroller",
        )

        fun start(context: Context, pausedPackage: String, appLabel: String) {
            val intent = Intent(context, AccessibilityAutoResumeService::class.java)
                .putExtra(EXTRA_PAUSED_PACKAGE, pausedPackage)
                .putExtra(EXTRA_APP_LABEL, appLabel)
            context.startForegroundService(intent)
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, AccessibilityAutoResumeService::class.java))
        }
    }
}
