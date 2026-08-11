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

    fun ensureChannel(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Service alerts",
                NotificationManager.IMPORTANCE_DEFAULT,
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
            .setContentText("Tap to reopen Nord AI Remapper and re-enable detection")
            .setContentIntent(intent)
            .setAutoCancel(true)
            .build()
        context.getSystemService(NotificationManager::class.java)
            .notify(DEATH_NOTIFICATION_ID, notification)
    }
}
