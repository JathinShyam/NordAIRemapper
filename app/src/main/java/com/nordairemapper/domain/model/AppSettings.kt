package com.nordairemapper.domain.model

enum class ThemeMode { DARK, LIGHT, SYSTEM }

data class AppSettings(
    val serviceEnabled: Boolean = true,
    val detectionStrategy: DetectionStrategy = DetectionStrategy.ACCESSIBILITY,
    val keyIdentity: KeyIdentity = KeyIdentity.UNCONFIGURED,
    val doublePressWindowMs: Long = 300L,
    val longPressThresholdMs: Long = 500L,
    val logcatPattern: String = DEFAULT_LOGCAT_PATTERN,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = false,
    val showServiceNotification: Boolean = true,
    val hapticFeedback: Boolean = true,
    val excludedApps: Set<String> = emptySet(),
    val onboardingCompleted: Boolean = false,
) {
    companion object {
        const val DEFAULT_LOGCAT_PATTERN = "KEYCODE_ACTION_BUTTON_CLICK"
        const val LEGACY_LOGCAT_PATTERN = "KEYLOG_OplusKeyEventUtil"
        val DOUBLE_PRESS_WINDOW_RANGE = 200L..500L
        val LONG_PRESS_THRESHOLD_RANGE = 300L..1000L
    }
}
