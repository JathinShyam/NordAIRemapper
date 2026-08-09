package com.nordairemapper.service

import android.content.Context
import android.content.Intent
import android.provider.Settings

object AccessibilityUtils {

    fun isServiceEnabled(context: Context): Boolean {
        val expected = "${context.packageName}/${PlusKeyAccessibilityService::class.java.name}"
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { it.equals(expected, ignoreCase = true) }
    }

    fun openAccessibilitySettings(context: Context) {
        context.startActivity(
            Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }
}
