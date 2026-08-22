package com.nordairemapper.presentation.remap

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Enumerates launchable apps. Binder-heavy — never call from Main. */
suspend fun queryLaunchableApps(context: Context): List<InstalledAppInfo> =
    withContext(Dispatchers.Default) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        pm.queryIntentActivities(intent, PackageManager.MATCH_ALL)
            .mapNotNull { resolve ->
                val info = resolve.activityInfo ?: return@mapNotNull null
                val label = resolve.loadLabel(pm)?.toString().orEmpty()
                InstalledAppInfo(packageName = info.packageName, label = label)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }
