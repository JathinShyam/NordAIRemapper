package com.nordairemapper.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Every action assignable to a press type or overlay slot. Serialized to JSON
 * (polymorphic, discriminated by @SerialName) for Room storage and backup files.
 */
@Serializable
sealed class RemapAction {

    @Serializable
    @SerialName("launch_app")
    data class LaunchApp(val packageName: String, val label: String) : RemapAction()

    @Serializable
    @SerialName("open_assistant")
    data object OpenAssistant : RemapAction()

    @Serializable
    @SerialName("open_camera")
    data class OpenCamera(val front: Boolean = false) : RemapAction()

    @Serializable
    @SerialName("toggle_flashlight")
    data object ToggleFlashlight : RemapAction()

    @Serializable
    @SerialName("screenshot")
    data object TakeScreenshot : RemapAction()

    @Serializable
    @SerialName("toggle_dnd")
    data object ToggleDoNotDisturb : RemapAction()

    @Serializable
    @SerialName("cycle_ringer")
    data object CycleRingerMode : RemapAction()

    @Serializable
    @SerialName("notification_shade")
    data object OpenNotificationShade : RemapAction()

    @Serializable
    @SerialName("quick_settings")
    data object OpenQuickSettings : RemapAction()

    @Serializable
    @SerialName("play_pause")
    data object PlayPauseMedia : RemapAction()

    @Serializable
    @SerialName("next_track")
    data object NextTrack : RemapAction()

    @Serializable
    @SerialName("previous_track")
    data object PreviousTrack : RemapAction()

    @Serializable
    @SerialName("media_volume")
    data class AdjustMediaVolume(val up: Boolean) : RemapAction()

    @Serializable
    @SerialName("recents")
    data object OpenRecents : RemapAction()

    @Serializable
    @SerialName("home")
    data object GoHome : RemapAction()

    @Serializable
    @SerialName("back")
    data object GoBack : RemapAction()

    @Serializable
    @SerialName("lock_screen")
    data object LockScreen : RemapAction()

    @Serializable
    @SerialName("toggle_auto_rotate")
    data object ToggleAutoRotate : RemapAction()

    @Serializable
    @SerialName("open_url")
    data class OpenUrl(val url: String) : RemapAction()

    @Serializable
    @SerialName("show_overlay")
    data object ShowOverlay : RemapAction()

    @Serializable
    @SerialName("none")
    data object None : RemapAction()
}
