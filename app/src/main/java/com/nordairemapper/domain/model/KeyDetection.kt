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
    /** AccessibilityService with FLAG_REQUEST_FILTER_KEY_EVENTS. */
    ACCESSIBILITY("accessibility"),

    /** Foreground service tailing logcat for OplusKeyEventUtil entries (needs READ_LOGS). */
    LOGCAT("logcat");

    companion object {
        fun fromKey(key: String): DetectionStrategy =
            entries.firstOrNull { it.key == key } ?: ACCESSIBILITY
    }
}

enum class Gesture {
    SINGLE_PRESS,
    DOUBLE_PRESS,
    LONG_PRESS,
}
