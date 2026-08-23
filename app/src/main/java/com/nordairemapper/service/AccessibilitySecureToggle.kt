package com.nordairemapper.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import android.util.Log

/**
 * Soft-enable / soft-disable Keyforge Accessibility by editing
 * [Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES]. Requires
 * [android.Manifest.permission.WRITE_SECURE_SETTINGS] (ADB Unlock).
 */
object AccessibilitySecureToggle {

    private const val TAG = "A11ySecureToggle"

    private fun selfComponent(context: Context): ComponentName =
        ComponentName(context, PlusKeyAccessibilityService::class.java)

    /**
     * Entries in ENABLED_ACCESSIBILITY_SERVICES may be stored flattened
     * ("pkg/pkg.Cls") or short-flattened ("pkg/.Cls") depending on who wrote
     * them (Settings UI, adb, other tools). Parse before comparing.
     */
    private fun isSelfEntry(entry: String, self: ComponentName): Boolean =
        ComponentName.unflattenFromString(entry)?.let { it == self } == true

    fun isListedEnabled(context: Context): Boolean {
        val self = selfComponent(context)
        val enabled = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ) ?: return false
        return enabled.split(':').any { isSelfEntry(it.trim(), self) }
    }

    fun setEnabled(context: Context, enabled: Boolean): Boolean {
        if (!ElevatedPermissions.hasWriteSecureSettings(context)) {
            Log.w(TAG, "WRITE_SECURE_SETTINGS missing; cannot toggle Accessibility")
            return false
        }
        val self = selfComponent(context)
        val current = Settings.Secure.getString(
            context.contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
        ).orEmpty()
        val services = current.split(':')
            .map { it.trim() }
            .filter { it.isNotEmpty() && !isSelfEntry(it, self) }
            .toMutableSet()
        if (enabled) services.add(self.flattenToString())
        val joined = services.joinToString(":")
        return runCatching {
            Settings.Secure.putString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
                joined,
            )
            Settings.Secure.putInt(
                context.contentResolver,
                Settings.Secure.ACCESSIBILITY_ENABLED,
                if (services.isEmpty()) 0 else 1,
            )
            Log.i(TAG, "Accessibility listed=$enabled services=$joined")
            true
        }.onFailure { Log.e(TAG, "Failed to toggle Accessibility", it) }
            .getOrDefault(false)
    }
}
