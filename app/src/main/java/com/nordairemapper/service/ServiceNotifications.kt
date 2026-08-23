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

}
