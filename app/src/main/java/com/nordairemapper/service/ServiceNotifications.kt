package com.nordairemapper.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.nordairemapper.R
import com.nordairemapper.presentation.MainActivity

object ServiceNotifications {
    private const val CHANNEL_ID = "service_alerts"
    private const val DEATH_NOTIFICATION_ID = 99
    private const val ACCESSIBILITY_PAUSED_ID = 100
    private const val LOGS_BLIND_ID = 102

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Service alerts",
                NotificationManager.IMPORTANCE_HIGH,
            )
        )
    }

    fun notifyDetectionStopped(context: Context) {
        ensureChannel(context)
        val intent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Key detection stopped")
            .setContentText("Tap to reopen Keyforge and re-enable detection")
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(DEATH_NOTIFICATION_ID, notification)
    }

    /**
     * Shown after [AccessibilityService.disableSelf] for an excluded banking/UPI app.
     * Tap opens system Accessibility settings so the user can turn Keyforge back on.
     */
    fun notifyAccessibilityPaused(context: Context, appLabel: String) {
        ensureChannel(context)
        val intent = PendingIntent.getActivity(
            context,
            ACCESSIBILITY_PAUSED_ID,
            Intent(android.provider.Settings.ACTION_ACCESSIBILITY_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Keyforge paused for $appLabel")
            .setContentText("Accessibility turned off so this app can run. Tap to turn Keyforge back on when you’re done.")
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(ACCESSIBILITY_PAUSED_ID, notification)
    }
    fun clearAccessibilityPaused(context: Context) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(ACCESSIBILITY_PAUSED_ID)
    }

    /**
     * Posted when the logcat tail receives no non-self lines while the screen
     * is on: READ_LOGS is granted but logd is not honoring it (seen on
     * OxygenOS after reinstall/re-grant). Plus Key lines from system_server
     * never arrive, so detection is blind. Tap opens Keyforge to re-run Unlock.
     */
    fun notifyLogsBlind(context: Context) {
        ensureChannel(context)
        val intent = PendingIntent.getActivity(
            context,
            LOGS_BLIND_ID,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Plus Key detection can’t see key presses")
            .setContentText("Keyforge lost access to system logs. Reboot the phone, then run Unlock again in the app.")
            .setContentIntent(intent)
            .setAutoCancel(true)
            .setOngoing(false)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(LOGS_BLIND_ID, notification)
    }

    fun clearLogsBlind(context: Context) {
        context.getSystemService(NotificationManager::class.java)
            .cancel(LOGS_BLIND_ID)
    }

}
