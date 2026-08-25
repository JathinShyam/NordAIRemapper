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
                    title = "Accessibility service",
                    subtitle = "Key events and system actions",
                    statusLabel = if (AccessibilityUtils.isServiceEnabled(context)) {
                        "Enabled"
                    } else {
                        "Not enabled"
                    },
                    isOk = AccessibilityUtils.isServiceEnabled(context),
                    section = Section.CORE,
                ),
            )
            add(
                Item(
                    id = Id.READ_LOGS,
                    title = "READ_LOGS",
                    subtitle = "Plus Key detection via logcat on Nord 5",
                    statusLabel = if (readLogsGranted) "Granted" else "Not granted",
                    isOk = readLogsGranted,
                    section = Section.CORE,
                ),
            )
            add(
                Item(
                    id = Id.OVERLAY,
                    title = "Display over other apps",
                    subtitle = "Floating Menu and Visual Overlay",
                    statusLabel = if (Settings.canDrawOverlays(context)) "Granted" else "Not granted",
                    isOk = Settings.canDrawOverlays(context),
                    section = Section.OVERLAYS,
                ),
            )
            add(
                Item(
                    id = Id.NOTIFICATIONS,
                    title = "Notifications",
                    subtitle = "Detection health and service status",
                    statusLabel = if (notificationsGranted) "Granted" else "Not granted",
                    isOk = notificationsGranted,
                    section = Section.RELIABILITY,
                ),
            )
            add(
                Item(
                    id = Id.BATTERY,
                    title = "Battery optimization",
                    subtitle = "Keep detection running in the background",
                    statusLabel = if (pm?.isIgnoringBatteryOptimizations(context.packageName) == true) {
                        "Exempt"
                    } else {
                        "Not exempt"
                    },
                    isOk = pm?.isIgnoringBatteryOptimizations(context.packageName) == true,
                    section = Section.RELIABILITY,
                ),
            )
            add(
                Item(
                    id = Id.WRITE_SECURE_SETTINGS,
                    title = "Modify system settings",
                    subtitle = "Hands-free banking Accessibility pause",
                    statusLabel = if (ElevatedPermissions.hasWriteSecureSettings(context)) {
                        "Granted"
                    } else {
                        "Not granted"
                    },
                    isOk = ElevatedPermissions.hasWriteSecureSettings(context),
                    section = Section.ADVANCED,
                ),
            )
            add(
                Item(
                    id = Id.USAGE_ACCESS,
                    title = "Usage access",
                    subtitle = "Auto-resume after banking apps",
                    statusLabel = if (ElevatedPermissions.hasUsageAccess(context)) {
                        "Granted"
                    } else {
                        "Not granted"
                    },
                    isOk = ElevatedPermissions.hasUsageAccess(context),
                    section = Section.ADVANCED,
                ),
            )
        }
    }

    fun logVisibilityItem(result: LogVisibilityProbe.Result): Item = Item(
        id = Id.LOG_VISIBILITY,
        title = "Device log visibility",
        subtitle = "Logcat can see other apps (OxygenOS consent)",
        statusLabel = when (result) {
            LogVisibilityProbe.Result.VISIBLE -> "Visible"
            LogVisibilityProbe.Result.BLIND -> "Blind"
        },
        isOk = result == LogVisibilityProbe.Result.VISIBLE,
        section = Section.CORE,
    )

    fun logVisibilityCheckingItem(): Item = Item(
        id = Id.LOG_VISIBILITY,
        title = "Device log visibility",
        subtitle = "Logcat can see other apps (OxygenOS consent)",
        statusLabel = "Checking",
        isOk = false,
        section = Section.CORE,
    )

    /** Items that affect the Settings hub chip (excludes optional advanced grants). */
    fun hubAttentionCount(items: List<Item>): Int =
        items.count { !it.isOk && it.section != Section.ADVANCED }

    fun hubSummaryLabel(attentionCount: Int): String = when (attentionCount) {
        0 -> "All OK"
        1 -> "1 needs attention"
        else -> "$attentionCount need attention"
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
