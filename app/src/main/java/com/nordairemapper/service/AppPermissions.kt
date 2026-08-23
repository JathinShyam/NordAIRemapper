package com.nordairemapper.service

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.PowerManager
import android.provider.Settings

/**
 * User-facing permission / special-access status for Settings → Permissions.
 */
object AppPermissions {

    enum class Section { CORE, OVERLAYS, RELIABILITY, ADVANCED }

    enum class Id {
        ACCESSIBILITY,
        READ_LOGS,
        LOG_VISIBILITY,
        OVERLAY,
        NOTIFICATIONS,
        BATTERY,
        WRITE_SECURE_SETTINGS,
        USAGE_ACCESS,
    }

    data class Item(
        val id: Id,
        val title: String,
        val subtitle: String,
        val statusLabel: String,
        val isOk: Boolean,
        val section: Section,
    )

    fun snapshot(context: Context): List<Item> {
        val pm = context.getSystemService(PowerManager::class.java)
        val notificationsGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
        val readLogsGranted = LogcatWatcherService.hasReadLogsPermission(context)

        return buildList {
            add(
                Item(
                    id = Id.ACCESSIBILITY,
                    title = "Accessibility Service",
                    subtitle = "Key Events And System Actions",
                    statusLabel = if (AccessibilityUtils.isServiceEnabled(context)) {
                        "Enabled"
                    } else {
                        "Not Enabled"
                    },
                    isOk = AccessibilityUtils.isServiceEnabled(context),
                    section = Section.CORE,
                ),
            )
            add(
                Item(
                    id = Id.READ_LOGS,
                    title = "READ_LOGS",
                    subtitle = "Plus Key Detection Via Logcat On Nord 5",
                    statusLabel = if (readLogsGranted) "Granted" else "Not Granted",
                    isOk = readLogsGranted,
                    section = Section.CORE,
                ),
            )
            add(
                Item(
                    id = Id.OVERLAY,
                    title = "Display Over Other Apps",
                    subtitle = "Floating Menu And Visual Overlay",
                    statusLabel = if (Settings.canDrawOverlays(context)) "Granted" else "Not Granted",
                    isOk = Settings.canDrawOverlays(context),
                    section = Section.OVERLAYS,
                ),
            )
            add(
                Item(
                    id = Id.NOTIFICATIONS,
                    title = "Notifications",
                    subtitle = "Detection Health And Service Status",
                    statusLabel = if (notificationsGranted) "Granted" else "Not Granted",
                    isOk = notificationsGranted,
                    section = Section.RELIABILITY,
                ),
            )
            add(
                Item(
                    id = Id.BATTERY,
                    title = "Battery Optimization",
                    subtitle = "Keep Detection Running In The Background",
                    statusLabel = if (pm?.isIgnoringBatteryOptimizations(context.packageName) == true) {
                        "Exempt"
                    } else {
                        "Not Exempt"
                    },
                    isOk = pm?.isIgnoringBatteryOptimizations(context.packageName) == true,
                    section = Section.RELIABILITY,
                ),
            )
            add(
                Item(
                    id = Id.WRITE_SECURE_SETTINGS,
                    title = "Modify System Settings",
                    subtitle = "Hands-Free Banking Accessibility Pause",
                    statusLabel = if (ElevatedPermissions.hasWriteSecureSettings(context)) {
                        "Granted"
                    } else {
                        "Not Granted"
                    },
                    isOk = ElevatedPermissions.hasWriteSecureSettings(context),
                    section = Section.ADVANCED,
                ),
            )
            add(
                Item(
                    id = Id.USAGE_ACCESS,
                    title = "Usage Access",
                    subtitle = "Auto-Resume After Banking Apps",
                    statusLabel = if (ElevatedPermissions.hasUsageAccess(context)) {
                        "Granted"
                    } else {
                        "Not Granted"
                    },
                    isOk = ElevatedPermissions.hasUsageAccess(context),
                    section = Section.ADVANCED,
                ),
            )
        }
    }

    fun logVisibilityItem(result: LogVisibilityProbe.Result): Item = Item(
        id = Id.LOG_VISIBILITY,
        title = "Device Log Visibility",
        subtitle = "Logcat Can See Other Apps (OxygenOS Consent)",
        statusLabel = when (result) {
            LogVisibilityProbe.Result.VISIBLE -> "Visible"
            LogVisibilityProbe.Result.BLIND -> "Blind"
        },
        isOk = result == LogVisibilityProbe.Result.VISIBLE,
        section = Section.CORE,
    )

    fun logVisibilityCheckingItem(): Item = Item(
        id = Id.LOG_VISIBILITY,
        title = "Device Log Visibility",
        subtitle = "Logcat Can See Other Apps (OxygenOS Consent)",
        statusLabel = "Checking",
        isOk = false,
        section = Section.CORE,
    )

    /** Items that affect the Settings hub chip (excludes optional advanced grants). */
    fun hubAttentionCount(items: List<Item>): Int =
        items.count { !it.isOk && it.section != Section.ADVANCED }

    fun hubSummaryLabel(attentionCount: Int): String = when (attentionCount) {
        0 -> "All OK"
        1 -> "1 Need Attention"
        else -> "$attentionCount Need Attention"
    }

    fun withLogVisibility(
        items: List<Item>,
        readLogsGranted: Boolean,
        logResult: LogVisibilityProbe.Result?,
        probing: Boolean,
    ): List<Item> {
        if (!readLogsGranted) return items
        val visibilityItem = when {
            probing -> logVisibilityCheckingItem()
            logResult != null -> logVisibilityItem(logResult)
            else -> logVisibilityCheckingItem()
        }
        val insertAt = items.indexOfFirst { it.id == Id.READ_LOGS }.let { idx ->
            if (idx < 0) items.size else idx + 1
        }
        return items.toMutableList().apply { add(insertAt, visibilityItem) }
    }
}
