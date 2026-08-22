package com.nordairemapper.service

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import com.nordairemapper.domain.model.DetectionStrategy

/**
 * Central rules for which detector feeds RemapEngine and when the logcat
 * watcher should run.
 *
 * RCA (Nord 5 / OxygenOS): the Plus Key is handled by `OplusKeyEventUtil` and
 * almost never arrives in Accessibility `onKeyEvent`. Volume/power do. Strategy
 * Accessibility alone therefore cannot detect the AI key on stock Nord 5.
 * Logcat (pattern `KEYCODE_ACTION_BUTTON_CLICK`) is the working path; Accessibility
 * remains required for system actions and for devices where the key *does* arrive.
 */
object DetectionCoordinator {

    /** Whether RemapEngine should accept an event from [source] under [strategy]. */
    fun acceptsSource(strategy: DetectionStrategy, source: DetectionStrategy): Boolean =
        when (strategy) {
            DetectionStrategy.AUTO -> true
            DetectionStrategy.ACCESSIBILITY ->
                source == DetectionStrategy.ACCESSIBILITY || source == DetectionStrategy.LOGCAT
            DetectionStrategy.LOGCAT -> source == DetectionStrategy.LOGCAT
        }

    /**
     * Logcat watcher is required for LOGCAT/AUTO, and as a companion for
     * ACCESSIBILITY on OnePlus where the Plus Key never reaches `onKeyEvent`.
     */
    fun needsLogcatWatcher(strategy: DetectionStrategy): Boolean = when (strategy) {
        DetectionStrategy.LOGCAT,
        DetectionStrategy.AUTO,
        DetectionStrategy.ACCESSIBILITY,
        -> true
    }

    fun syncLogcatWatcher(
        context: Context,
        strategy: DetectionStrategy,
        serviceEnabled: Boolean,
    ) {
        val shouldRun = serviceEnabled &&
            needsLogcatWatcher(strategy) &&
            LogcatWatcherService.hasReadLogsPermission(context)
        // Starting an FGS from a background state throws on Android 12+; this
        // runs from collectors/service callbacks where that is possible.
        runCatching {
            if (shouldRun) {
                LogcatWatcherService.start(context)
            } else {
                LogcatWatcherService.stop(context)
            }
        }.onFailure { Log.w("DetectionCoordinator", "syncLogcatWatcher failed", it) }
    }
}

/** Helpers for the READ_LOGS grant UX (primary path is in-app Wireless ADB). */
object ReadLogsGrantHelper {

    const val ON_DEVICE_SHELL_COMMAND =
        "pm grant com.nordairemapper android.permission.READ_LOGS"

    fun openDeveloperOptions(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
            .recoverCatching {
                context.startActivity(
                    Intent(Settings.ACTION_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
    }

    /** Opens Wireless debugging when the OEM exposes it (Android 11+). */
    fun openWirelessDebugging(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val wireless = Intent("android.settings.ADB_WIRELESS_SETTINGS")
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (wireless.resolveActivity(context.packageManager) != null) {
                context.startActivity(wireless)
                return
            }
        }
        openDeveloperOptions(context)
    }
}
