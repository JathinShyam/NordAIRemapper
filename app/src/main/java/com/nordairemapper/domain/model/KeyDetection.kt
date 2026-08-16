package com.nordairemapper.domain.model

import kotlinx.serialization.Serializable

/**
 * Identity of the Plus Key as learned at runtime. The key may report
 * KEYCODE_UNKNOWN (0), in which case the scanCode is the only stable identity.
 */
@Serializable
data class KeyIdentity(
    val keyCode: Int,
    val scanCode: Int,
) {
    val isConfigured: Boolean get() = keyCode != -1 || scanCode != -1

    companion object {
        val UNCONFIGURED = KeyIdentity(keyCode = -1, scanCode = -1)
    }
}

enum class DetectionStrategy(val key: String) {
    /**
     * Prefer whichever detector sees the Plus Key: Accessibility when the OS
     * delivers it, Logcat on Nord 5 / OxygenOS. Recommended default.
     */
    AUTO("auto"),

    /** AccessibilityService with FLAG_REQUEST_FILTER_KEY_EVENTS (+ logcat companion). */
    ACCESSIBILITY("accessibility"),

    /** Foreground service tailing logcat for KEYCODE_ACTION_BUTTON_CLICK (needs READ_LOGS). */
    LOGCAT("logcat");

    companion object {
        fun fromKey(key: String): DetectionStrategy =
            entries.firstOrNull { it.key == key } ?: AUTO
    }
}

enum class Gesture {
    SINGLE_PRESS,
    DOUBLE_PRESS,
    LONG_PRESS,
}
