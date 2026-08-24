package com.nordairemapper.domain.model

/**
 * Visual overlay payload for a fired remap. [stateKey] selects a state-specific
 * icon/caption when the action result depends on device state (toggles, cycles).
 */
data class ActionFeedback(
    val action: RemapAction,
    val stateKey: String? = null,
)

object ActionFeedbackState {
    const val AUTO_ROTATE_ON = "auto_rotate_on"
    const val AUTO_ROTATE_OFF = "auto_rotate_off"
    const val FLASHLIGHT_ON = "flashlight_on"
    const val FLASHLIGHT_OFF = "flashlight_off"
    const val DND_ON = "dnd_on"
    const val DND_OFF = "dnd_off"
    const val RINGER_RING = "ringer_ring"
    const val RINGER_VIBRATE = "ringer_vibrate"
    const val RINGER_SILENT = "ringer_silent"

    /** Action could not run (missing Accessibility, system refused, crash). */
    const val ACTION_FAILED = "action_failed"
}
