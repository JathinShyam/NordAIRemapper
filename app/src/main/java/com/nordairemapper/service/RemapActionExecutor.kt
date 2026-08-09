package com.nordairemapper.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes remap actions. Global system actions (screenshot, lock, recents...)
 * require the accessibility service to be connected; everything else works
 * from the application context.
 */
@Singleton
class RemapActionExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ActionDispatcher {

    private val audioManager get() = context.getSystemService(AudioManager::class.java)
    private val notificationManager get() = context.getSystemService(NotificationManager::class.java)
    private val cameraManager get() = context.getSystemService(CameraManager::class.java)

    /** Tracks torch state via callback since there is no direct getter. */
    @Volatile
    private var torchOn = false

    init {
        runCatching {
            cameraManager.registerTorchCallback(
                object : CameraManager.TorchCallback() {
                    override fun onTorchModeChanged(cameraId: String, enabled: Boolean) {
                        torchOn = enabled
                    }
                },
                null,
            )
        }
    }

    override suspend fun execute(action: RemapAction) {
        if (action != RemapAction.None && settingsRepository.settings.first().hapticFeedback) {
            performHaptic()
        }
        runCatching { dispatch(action) }
            .onFailure { Log.w(TAG, "Failed to execute $action", it) }
    }

    private fun dispatch(action: RemapAction) {
        when (action) {
            is RemapAction.LaunchApp -> launchApp(action.packageName)
            is RemapAction.OpenAssistant -> openAssistant()
            is RemapAction.OpenCamera -> openCamera(action.front)
            is RemapAction.ToggleFlashlight -> toggleFlashlight()
            is RemapAction.TakeScreenshot -> globalAction(AccessibilityService.GLOBAL_ACTION_TAKE_SCREENSHOT)
            is RemapAction.ToggleDoNotDisturb -> toggleDnd()
            is RemapAction.CycleRingerMode -> cycleRingerMode()
            is RemapAction.OpenNotificationShade -> globalAction(AccessibilityService.GLOBAL_ACTION_NOTIFICATIONS)
            is RemapAction.OpenQuickSettings -> globalAction(AccessibilityService.GLOBAL_ACTION_QUICK_SETTINGS)
            is RemapAction.PlayPauseMedia -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)
            is RemapAction.NextTrack -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_NEXT)
            is RemapAction.PreviousTrack -> dispatchMediaKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS)
            is RemapAction.AdjustMediaVolume -> adjustVolume(action.up)
            is RemapAction.OpenRecents -> globalAction(AccessibilityService.GLOBAL_ACTION_RECENTS)
            is RemapAction.GoHome -> globalAction(AccessibilityService.GLOBAL_ACTION_HOME)
            is RemapAction.GoBack -> globalAction(AccessibilityService.GLOBAL_ACTION_BACK)
            is RemapAction.LockScreen -> globalAction(AccessibilityService.GLOBAL_ACTION_LOCK_SCREEN)
            is RemapAction.ToggleAutoRotate -> toggleAutoRotate()
            is RemapAction.OpenUrl -> openUrl(action.url)
            is RemapAction.ShowOverlay -> Log.i(TAG, "Overlay not implemented yet (Phase 9)")
            is RemapAction.None -> Unit
        }
    }

    private fun launchApp(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            Log.w(TAG, "No launch intent for $packageName")
            return
        }
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun openAssistant() {
        val assist = Intent(Intent.ACTION_VOICE_COMMAND).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(assist) }.onFailure {
            context.startActivity(Intent(Intent.ACTION_ASSIST).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }

    private fun openCamera(front: Boolean) {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (front) {
            intent.putExtra("android.intent.extras.CAMERA_FACING", 1)
            intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
        }
        context.startActivity(intent)
    }

    private fun toggleFlashlight() {
        val backCameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val chars = cameraManager.getCameraCharacteristics(id)
            chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        } ?: return
        cameraManager.setTorchMode(backCameraId, !torchOn)
    }

    private fun toggleDnd() {
        if (!notificationManager.isNotificationPolicyAccessGranted) {
            context.startActivity(
                Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        val dndActive =
            notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        notificationManager.setInterruptionFilter(
            if (dndActive) NotificationManager.INTERRUPTION_FILTER_ALL
            else NotificationManager.INTERRUPTION_FILTER_PRIORITY
        )
    }

    private fun cycleRingerMode() {
        audioManager.ringerMode = when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
            else -> AudioManager.RINGER_MODE_NORMAL
        }
    }

    private fun dispatchMediaKey(keyCode: Int) {
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        audioManager.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))
    }

    private fun adjustVolume(up: Boolean) {
        audioManager.adjustStreamVolume(
            AudioManager.STREAM_MUSIC,
            if (up) AudioManager.ADJUST_RAISE else AudioManager.ADJUST_LOWER,
            AudioManager.FLAG_SHOW_UI,
        )
    }

    private fun toggleAutoRotate() {
        if (!Settings.System.canWrite(context)) {
            context.startActivity(
                Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS, Uri.parse("package:${context.packageName}"))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
            return
        }
        val current = Settings.System.getInt(
            context.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            0,
        )
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            if (current == 1) 0 else 1,
        )
    }

    private fun openUrl(url: String) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun globalAction(action: Int) {
        val service = AccessibilityServiceHolder.service
        if (service == null) {
            Log.w(TAG, "Accessibility service not connected; cannot run global action $action")
            return
        }
        service.performGlobalAction(action)
    }

    private fun performHaptic() {
        val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            ?: context.getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    }

    private companion object {
        const val TAG = "RemapActionExecutor"
    }
}
