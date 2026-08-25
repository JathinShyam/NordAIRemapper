package com.nordairemapper.service.adb

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nordairemapper.R
import com.nordairemapper.presentation.MainActivity

/**
 * WhatsApp-style direct-reply notification for the Built-In pairing flow:
 * once the pairing port is found, the user can type the 6-digit code into the
 * heads-up notification without leaving the system pairing dialog.
 */
object PairingNotifier {

    /** Shared by [PairingGrantService] for its FGS notification. */
    internal const val CHANNEL_ID = "pairing_reply"
    const val NOTIFICATION_ID = 2001
    const val KEY_CODE = "pairing_code"

    /** Step-3 state before the port is found: keep the heads-up alive. */
    fun postWaiting(context: Context) {
        if (!notificationsEnabled(context)) return
        ensureChannel(context)
        notify(
            context,
            Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Watching for the pairing dialog…")
                .setContentText(
                    "Open Wireless debugging → Pair device with pairing code. " +
                        "This turns into a code box the moment the dialog appears.",
                )
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setOngoing(true)
                .build(),
        )
    }

    fun postPrompt(context: Context, port: Int?, errorLine: String? = null) {
        Log.i("PairingNotifier", "postPrompt port=$port error=$errorLine enabled=${notificationsEnabled(context)}")
        if (!notificationsEnabled(context)) return
        ensureChannel(context)

        val remoteInput = RemoteInput.Builder(KEY_CODE)
            .setLabel("6-digit pairing code")
            .build()

        val replyIntent = Intent(context, PairingReplyReceiver::class.java)
        val replyPendingIntent = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID,
            replyIntent,
            // MUTABLE: RemoteInput results are written into this intent.
            PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val replyAction = Notification.Action.Builder(
            null,
            "Enter pairing code",
            replyPendingIntent,
        ).addRemoteInput(remoteInput).build()

        val contentIntent = PendingIntent.getActivity(
            context,
            NOTIFICATION_ID,
            Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val portLine = port?.let { "Pairing port $port detected. " } ?: ""
        val body = errorLine
            ?: "${portLine}Type the 6-digit code shown in the pairing dialog — no app switching needed."
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(if (errorLine != null) "Try that code again" else "Finish pairing right here")
            .setContentText(body)
            .setContentIntent(contentIntent)
            .addAction(replyAction)
            .setCategory(Notification.CATEGORY_PROGRESS)
            .setOngoing(true)
            .setOnlyAlertOnce(false)
            .build()
        notify(context, notification)
    }

    fun postProgress(context: Context, message: String) {
        if (!notificationsEnabled(context)) return
        ensureChannel(context)
        notify(
            context,
            Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle("Pairing…")
                .setContentText(message)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setOngoing(true)
                .build(),
        )
    }

    /** Success/failure banner that removes itself after a few seconds. */
    fun postResult(context: Context, ok: Boolean, message: String) {
        if (!notificationsEnabled(context)) return
        ensureChannel(context)
        // Remediation messages are long — BigTextStyle keeps them readable.
        val style = Notification.BigTextStyle().bigText(message)
        notify(
            context,
            Notification.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentTitle(if (ok) "Paired and unlocked" else "Pairing didn't finish")
                .setContentText(message)
                .setStyle(style)
                .setCategory(Notification.CATEGORY_PROGRESS)
                .setAutoCancel(true)
                .setTimeoutAfter(if (ok) 5_000L else 10_000L)
                .build(),
        )
    }

    fun cancel(context: Context) {
        getManager(context).cancel(NOTIFICATION_ID)
    }

    private fun notificationsEnabled(context: Context): Boolean =
        context.getSystemService(android.app.NotificationManager::class.java)
            ?.areNotificationsEnabled() ?: true

    private fun notify(context: Context, notification: Notification) {
        getManager(context).notify(NOTIFICATION_ID, notification)
    }

    internal fun ensureChannel(context: Context) {
        getManager(context).createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Wireless pairing",
                NotificationManager.IMPORTANCE_HIGH,
            ),
        )
    }

    private fun getManager(context: Context): NotificationManager =
        context.getSystemService(NotificationManager::class.java)
}
