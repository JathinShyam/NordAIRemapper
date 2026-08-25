package com.nordairemapper.service

import android.app.AppOpsManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Process
import android.provider.Settings

/**
 * Elevated grants needed for hands-free banking Accessibility pause/resume.
 * Granted once via the same Wireless debugging Unlock flow as READ_LOGS.
 */
object ElevatedPermissions {

    fun hasWriteSecureSettings(context: Context): Boolean =
        context.checkSelfPermission(android.Manifest.permission.WRITE_SECURE_SETTINGS) ==
            PackageManager.PERMISSION_GRANTED

    fun hasUsageAccess(context: Context): Boolean {
        val appOps = context.getSystemService(AppOpsManager::class.java) ?: return false
        val mode = appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    /** True when Keyforge can pause Accessibility and turn it back on without the user. */
    fun canAutoResumeAccessibility(context: Context): Boolean =
        hasWriteSecureSettings(context) && hasUsageAccess(context)

    /**
     * Shell lines run during Wireless Unlock so banking auto-pause works hands-free.
     * Safe to re-run; grants are idempotent.
     *
     * ORDER MATTERS: READ_LOGS stays LAST. It is the only grant backed by a
     * Unix group ("log"), so applying it makes PackageManager KILL this app's
     * process to swap the new gids in ("permission grant or revoke changed
     * gids"). First-in-list, it silently murdered commands #2/#3 mid-flow
     * (RCA 2026-08-25). Last-in-list, everything else is already applied when
     * the expected self-kill fires; if the process survives (gids unchanged,
     * e.g. re-run), the caller just finishes the success UX normally.
     */
    val UNLOCK_SHELL_COMMANDS: List<String> = listOf(
        "pm grant com.nordairemapper android.permission.WRITE_SECURE_SETTINGS",
        "appops set com.nordairemapper GET_USAGE_STATS allow",
        "pm grant com.nordairemapper android.permission.READ_LOGS",
    )

    fun openUsageAccessSettings(context: Context) {
        context.startActivity(
            android.content.Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    }
}
