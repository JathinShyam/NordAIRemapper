package com.nordairemapper.service

import android.accessibilityservice.AccessibilityService
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.media.session.MediaSessionManager
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Executes remap actions. Soft-fails with logs + short toasts when a special
 * permission or Accessibility connection is missing — never crash the remap
 * pipeline.
 *
 * Permission traps (OxygenOS / Android N+):
 * - Ringer silent / DND: needs **Do Not Disturb access**
 *   (`NotificationManager.isNotificationPolicyAccessGranted`), not just the
 *   manifest `ACCESS_NOTIFICATION_POLICY` declaration.
 * - Auto-rotate: needs **Modify system settings**.
 * - Global actions: Accessibility must be connected.
 */
@Singleton
class RemapActionExecutor @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
) : ActionDispatcher {

    private val mainHandler = Handler(Looper.getMainLooper())
    private val audioManager get() = context.getSystemService(AudioManager::class.java)
    private val notificationManager get() = context.getSystemService(NotificationManager::class.java)
    private val cameraManager get() = context.getSystemService(CameraManager::class.java)

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
            .onFailure {
                Log.w(TAG, "Failed to execute $action", it)
                toast("Action failed: ${it.message ?: action.javaClass.simpleName}")
            }
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
            is RemapAction.ShowOverlay -> FloatingOverlayService.show(context)
            is RemapAction.None -> Unit
        }
    }

    private fun launchApp(packageName: String) {
        val intent = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            Log.w(TAG, "No launch intent for $packageName")
            toast("App not found")
            return
        }
        context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun openAssistant() {
        val candidates = listOf(
            Intent(Intent.ACTION_VOICE_COMMAND),
            Intent(Intent.ACTION_ASSIST),
            Intent("android.intent.action.VOICE_ASSIST"),
        )
        for (intent in candidates) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            val ok = runCatching { context.startActivity(intent) }.isSuccess
            if (ok) return
        }
        toast("No assistant app found")
    }

    private fun openCamera(front: Boolean) {
        val intent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (front) {
            intent.putExtra("android.intent.extras.CAMERA_FACING", 1)
            intent.putExtra("android.intent.extra.USE_FRONT_CAMERA", true)
            intent.putExtra("camerafacing", "front")
        }
        runCatching { context.startActivity(intent) }
            .onFailure {
                context.startActivity(
                    Intent(MediaStore.ACTION_IMAGE_CAPTURE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                )
            }
    }

    private fun toggleFlashlight() {
        val backCameraId = cameraManager.cameraIdList.firstOrNull { id ->
            val chars = cameraManager.getCameraCharacteristics(id)
            chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true &&
                chars.get(CameraCharacteristics.LENS_FACING) == CameraCharacteristics.LENS_FACING_BACK
        }
        if (backCameraId == null) {
            toast("No flashlight on this device")
            return
        }
        runCatching {
            val next = !torchOn
            cameraManager.setTorchMode(backCameraId, next)
            torchOn = next
            toast(if (next) "Flashlight on" else "Flashlight off")
        }.onFailure {
            Log.w(TAG, "Torch failed", it)
            toast("Flashlight blocked by system")
        }
    }

    private fun toggleDnd() {
        if (!ensureNotificationPolicyAccess("Do Not Disturb")) return
        val dndActive =
            notificationManager.currentInterruptionFilter != NotificationManager.INTERRUPTION_FILTER_ALL
        notificationManager.setInterruptionFilter(
            if (dndActive) NotificationManager.INTERRUPTION_FILTER_ALL
            else NotificationManager.INTERRUPTION_FILTER_PRIORITY,
        )
        toast(if (dndActive) "DND off" else "DND on")
    }

    /**
     * Android N+ silently ignores ringer changes that affect DND unless the user
     * has granted **Do Not Disturb access** (special app access), even when the
     * manifest permission is present.
     */
    private fun cycleRingerMode() {
        if (audioManager.isVolumeFixed) {
            toast("Ringer is fixed on this device")
            return
        }
        if (!ensureNotificationPolicyAccess("Ring / vibrate / silent")) return

        val current = audioManager.ringerMode
        val next = when (current) {
            AudioManager.RINGER_MODE_NORMAL -> AudioManager.RINGER_MODE_VIBRATE
            AudioManager.RINGER_MODE_VIBRATE -> AudioManager.RINGER_MODE_SILENT
            else -> AudioManager.RINGER_MODE_NORMAL
        }
        audioManager.ringerMode = next

        // Some OEMs keep interruption filter elevated; align filter with silent.
        when (next) {
            AudioManager.RINGER_MODE_SILENT -> {
                if (notificationManager.currentInterruptionFilter ==
                    NotificationManager.INTERRUPTION_FILTER_ALL
                ) {
                    notificationManager.setInterruptionFilter(
                        NotificationManager.INTERRUPTION_FILTER_PRIORITY,
                    )
                }
            }
            AudioManager.RINGER_MODE_NORMAL, AudioManager.RINGER_MODE_VIBRATE -> {
                if (notificationManager.currentInterruptionFilter !=
                    NotificationManager.INTERRUPTION_FILTER_ALL
                ) {
                    notificationManager.setInterruptionFilter(
                        NotificationManager.INTERRUPTION_FILTER_ALL,
                    )
                }
            }
        }

        val applied = audioManager.ringerMode
        toast(
            when (applied) {
                AudioManager.RINGER_MODE_NORMAL -> "Ring"
                AudioManager.RINGER_MODE_VIBRATE -> "Vibrate"
                AudioManager.RINGER_MODE_SILENT -> "Silent"
                else -> "Ringer updated"
            },
        )
        if (applied != next) {
            Log.w(TAG, "Ringer set to $next but device reports $applied")
        }
    }

    private fun dispatchMediaKey(keyCode: Int) {
        val down = KeyEvent(KeyEvent.ACTION_DOWN, keyCode)
        val up = KeyEvent(KeyEvent.ACTION_UP, keyCode)

        // Prefer the active media session when the platform allows it.
        val sentToSession = runCatching {
            val sessions = context.getSystemService(MediaSessionManager::class.java)
                ?.getActiveSessions(null)
                .orEmpty()
            if (sessions.isEmpty()) return@runCatching false
            sessions.forEach { controller ->
                controller.dispatchMediaButtonEvent(down)
                controller.dispatchMediaButtonEvent(up)
            }
            true
        }.getOrDefault(false)

        if (!sentToSession) {
            audioManager.dispatchMediaKeyEvent(down)
            audioManager.dispatchMediaKeyEvent(up)
        }
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
            toast("Allow Modify system settings")
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_WRITE_SETTINGS,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
            return
        }
        val current = Settings.System.getInt(
            context.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            0,
        )
        val next = if (current == 1) 0 else 1
        Settings.System.putInt(
            context.contentResolver,
            Settings.System.ACCELEROMETER_ROTATION,
            next,
        )
        toast(if (next == 1) "Auto-rotate on" else "Auto-rotate off")
    }

    private fun openUrl(url: String) {
        val normalized = when {
            url.startsWith("http://") || url.startsWith("https://") -> url
            url.contains("://") -> url
            else -> "https://$url"
        }
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(normalized))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }.onFailure {
            toast("Cannot open link")
        }
    }

    private fun globalAction(action: Int) {
        val service = AccessibilityServiceHolder.service
        if (service == null) {
            Log.w(TAG, "Accessibility service not connected; cannot run global action $action")
            toast("Enable Accessibility for this action")
            return
        }
        val ok = service.performGlobalAction(action)
        if (!ok) {
            Log.w(TAG, "performGlobalAction($action) returned false")
            toast("System action failed")
        }
    }

    /**
     * Manifest `ACCESS_NOTIFICATION_POLICY` is not enough — the user must toggle
     * Do Not Disturb access for this app in Special app access.
     */
    private fun ensureNotificationPolicyAccess(featureLabel: String): Boolean {
        if (notificationManager.isNotificationPolicyAccessGranted) return true
        Log.w(TAG, "Notification policy access missing for $featureLabel")
        toast("Allow Do Not Disturb access for $featureLabel")
        context.startActivity(
            Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
        return false
    }

    private fun performHaptic() {
        val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            ?: context.getSystemService(Vibrator::class.java)
        vibrator?.vibrate(VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK))
    }

    private fun toast(message: String) {
        mainHandler.post {
            Toast.makeText(context.applicationContext, message, Toast.LENGTH_SHORT).show()
        }
    }

    private companion object {
        const val TAG = "RemapActionExecutor"
    }
}
