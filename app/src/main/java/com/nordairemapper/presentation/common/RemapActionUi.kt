package com.nordairemapper.presentation.common

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.VolumeDown
import androidx.compose.material.icons.automirrored.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.Campaign
import androidx.compose.material.icons.outlined.Crop
import androidx.compose.material.icons.outlined.DoNotDisturbOn
import androidx.compose.material.icons.outlined.FlashlightOn
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Layers
import androidx.compose.material.icons.outlined.Link
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PhotoCameraFront
import androidx.compose.material.icons.outlined.ScreenRotation
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SkipNext
import androidx.compose.material.icons.outlined.SkipPrevious
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.ui.graphics.vector.ImageVector
import com.nordairemapper.domain.model.RemapAction

fun RemapAction.displayName(): String = when (this) {
    is RemapAction.LaunchApp -> label.ifBlank { packageName }
    RemapAction.OpenAssistant -> "Assistant"
    is RemapAction.OpenCamera -> if (front) "Front camera" else "Rear camera"
    RemapAction.ToggleFlashlight -> "Flashlight"
    RemapAction.TakeScreenshot -> "Screenshot"
    RemapAction.ToggleDoNotDisturb -> "Do Not Disturb"
    RemapAction.CycleRingerMode -> "Ring / vibrate / silent"
    RemapAction.OpenNotificationShade -> "Notification shade"
    RemapAction.OpenQuickSettings -> "Quick settings"
    RemapAction.PlayPauseMedia -> "Play / pause"
    RemapAction.NextTrack -> "Next track"
    RemapAction.PreviousTrack -> "Previous track"
    is RemapAction.AdjustMediaVolume -> if (up) "Volume up" else "Volume down"
    RemapAction.OpenRecents -> "Recents"
    RemapAction.GoHome -> "Home"
    RemapAction.GoBack -> "Back"
    RemapAction.LockScreen -> "Lock screen"
    RemapAction.ToggleAutoRotate -> "Auto-rotate"
    is RemapAction.OpenUrl -> "Open link"
    RemapAction.ShowOverlay -> "Show floating menu"
    RemapAction.None -> "No action"
}

fun RemapAction.displayDescription(): String = when (this) {
    is RemapAction.LaunchApp -> packageName
    RemapAction.OpenAssistant -> "Open the default voice assistant"
    is RemapAction.OpenCamera -> "Open the camera app"
    RemapAction.ToggleFlashlight -> "Toggle the LED torch"
    RemapAction.TakeScreenshot -> "Capture the screen"
    RemapAction.ToggleDoNotDisturb -> "Toggle DND (needs Do Not Disturb access)"
    RemapAction.CycleRingerMode -> "Cycle ring → vibrate → silent (Silent uses Zen; Ring/Vibrate turn Zen off)"
    RemapAction.OpenNotificationShade -> "Expand the notification shade"
    RemapAction.OpenQuickSettings -> "Expand quick settings"
    RemapAction.PlayPauseMedia -> "Toggle media playback"
    RemapAction.NextTrack -> "Skip to the next track"
    RemapAction.PreviousTrack -> "Go to the previous track"
    is RemapAction.AdjustMediaVolume -> "Adjust media stream volume"
    RemapAction.OpenRecents -> "Open recent apps"
    RemapAction.GoHome -> "Go to the home screen"
    RemapAction.GoBack -> "Perform back"
    RemapAction.LockScreen -> "Lock the device"
    RemapAction.ToggleAutoRotate -> "Toggle accelerometer rotation"
    is RemapAction.OpenUrl -> url
    RemapAction.ShowOverlay -> "Open the floating menu"
    RemapAction.None -> "This press type does nothing"
}

fun RemapAction.icon(): ImageVector = when (this) {
    is RemapAction.LaunchApp -> Icons.Outlined.Apps
    RemapAction.OpenAssistant -> Icons.Outlined.Mic
    is RemapAction.OpenCamera -> if (front) Icons.Outlined.PhotoCameraFront else Icons.Outlined.CameraAlt
    RemapAction.ToggleFlashlight -> Icons.Outlined.FlashlightOn
    RemapAction.TakeScreenshot -> Icons.Outlined.Crop
    RemapAction.ToggleDoNotDisturb -> Icons.Outlined.DoNotDisturbOn
    RemapAction.CycleRingerMode -> Icons.Outlined.Campaign
    RemapAction.OpenNotificationShade -> Icons.Outlined.Notifications
    RemapAction.OpenQuickSettings -> Icons.Outlined.Settings
    RemapAction.PlayPauseMedia -> Icons.Outlined.Pause
    RemapAction.NextTrack -> Icons.Outlined.SkipNext
    RemapAction.PreviousTrack -> Icons.Outlined.SkipPrevious
    is RemapAction.AdjustMediaVolume ->
        if (up) Icons.AutoMirrored.Outlined.VolumeUp else Icons.AutoMirrored.Outlined.VolumeDown
    RemapAction.OpenRecents -> Icons.Outlined.Layers
    RemapAction.GoHome -> Icons.Outlined.Home
    RemapAction.GoBack -> Icons.AutoMirrored.Outlined.ArrowBack
    RemapAction.LockScreen -> Icons.Outlined.Lock
    RemapAction.ToggleAutoRotate -> Icons.Outlined.ScreenRotation
    is RemapAction.OpenUrl -> Icons.Outlined.Link
    RemapAction.ShowOverlay -> Icons.Outlined.Smartphone
    RemapAction.None -> Icons.Outlined.Block
}

/** Stable identity for conflict detection (same action type + params). */
fun RemapAction.conflictKey(): String = when (this) {
    is RemapAction.LaunchApp -> "launch:$packageName"
    is RemapAction.OpenCamera -> "camera:$front"
    is RemapAction.AdjustMediaVolume -> "volume:$up"
    is RemapAction.OpenUrl -> "url:$url"
    RemapAction.None -> "none"
    else -> this::class.simpleName ?: "unknown"
}
