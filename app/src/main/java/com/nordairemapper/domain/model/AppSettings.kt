package com.nordairemapper.domain.model

enum class ThemeMode { DARK, LIGHT, SYSTEM }

enum class HapticIntensity { LIGHT, MEDIUM, HEAVY }

data class AppSettings(
    val serviceEnabled: Boolean = true,
    val detectionStrategy: DetectionStrategy = DetectionStrategy.AUTO,
    val keyIdentity: KeyIdentity = KeyIdentity.UNCONFIGURED,
    val doublePressWindowMs: Long = 300L,
    val longPressThresholdMs: Long = 500L,
    val logcatPattern: String = DEFAULT_LOGCAT_PATTERN,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val showServiceNotification: Boolean = true,
    val hapticFeedback: Boolean = true,
    val hapticIntensity: HapticIntensity = HapticIntensity.MEDIUM,
    val visualOverlayEnabled: Boolean = true,
    val lockScreenSingleEnabled: Boolean = false,
    val lockScreenDoubleEnabled: Boolean = false,
    val lockScreenLongEnabled: Boolean = false,
    val excludedApps: Set<String> = emptySet(),
    /** When true, opening an excluded app disables Keyforge Accessibility (for banking/UPI). Re-enable via the pause notification. */
    val pauseAccessibilityInExcludedApps: Boolean = false,
    val onboardingCompleted: Boolean = false,
    /** Detection health: epoch ms of the last classified Plus Key gesture. */
    val lastPlusKeySeenAtMs: Long = 0L,
) {
    companion object {
        const val DEFAULT_LOGCAT_PATTERN = "KEYCODE_ACTION_BUTTON_CLICK"
        const val LEGACY_LOGCAT_PATTERN = "KEYLOG_OplusKeyEventUtil"
        val DOUBLE_PRESS_WINDOW_RANGE = 200L..500L
        val LONG_PRESS_THRESHOLD_RANGE = 300L..1000L
        val HOLD_DURATION_RANGE_MS = 300L..2000L
    }
}
