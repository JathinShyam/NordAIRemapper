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
import com.nordairemapper.domain.model.HapticIntensity
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
 * - Ring / vibrate / silent: needs **Do Not Disturb access**. On Android N+,
 *   Silent is Zen total silence (filter NONE); Ring/Vibrate turn Zen off (ALL).
 *   Separate [RemapAction.ToggleDoNotDisturb] uses PRIORITY ↔ ALL.
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

    /** Last verified ring / vibrate / silent step. */
    @Volatile
    private var confirmedRingerProfile: RingerProfile? = null

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
        if (action != RemapAction.None) {
            val settings = settingsRepository.settings.first()
            if (settings.hapticFeedback) {
                performHaptic(settings.hapticIntensity)
            }
            if (settings.visualOverlayEnabled) {
                ActionFeedbackOverlayService.show(context, action)
            }
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
     * Ring → vibrate → silent using Zen the way Android N+ expects:
     *
     * - **Silent** = Zen **on** ([NotificationManager.INTERRUPTION_FILTER_NONE])
     *   plus [AudioManager.RINGER_MODE_SILENT] (no sound, no vibrate).
     * - **Ring / Vibrate** = Zen **off** ([NotificationManager.INTERRUPTION_FILTER_ALL])
     *   plus NORMAL / VIBRATE.
     *
     * This is intentional and separate from [toggleDnd] (PRIORITY ↔ ALL).
     * Trying to do Silent without Zen on OxygenOS does not stick and lied in the UI.
     */
    private fun cycleRingerMode() {
        if (audioManager.isVolumeFixed) {
            toast("Ringer is fixed on this device")
            return
        }
        if (!ensureNotificationPolicyAccess("Ring / vibrate / silent")) return

        val current = confirmedRingerProfile ?: readSoundProfile()
        val next = when (current) {
            RingerProfile.RING -> RingerProfile.VIBRATE
            RingerProfile.VIBRATE -> RingerProfile.SILENT
            RingerProfile.SILENT -> RingerProfile.RING
        }

        applySoundProfile(next)
        mainHandler.postDelayed({
            if (readSoundProfile() != next) {
                applySoundProfile(next)
            }
            val now = readSoundProfile()
            if (now == next) {
                confirmedRingerProfile = next
                toast(next.label())
            } else {
                confirmedRingerProfile = now
                Log.w(
                    TAG,
                    "Sound profile: wanted=$next got=$now " +
                        "ringer=${audioManager.ringerMode} " +
                        "filter=${notificationManager.currentInterruptionFilter}",
                )
                toast("Stuck on ${now.label()}")
            }
        }, 120L)
    }

    private enum class RingerProfile { RING, VIBRATE, SILENT }

    private fun RingerProfile.label(): String = when (this) {
        RingerProfile.RING -> "Ring"
        RingerProfile.VIBRATE -> "Vibrate"
        RingerProfile.SILENT -> "Silent"
    }

    private fun readSoundProfile(): RingerProfile {
        val filter = notificationManager.currentInterruptionFilter
        // Total / alarms-only Zen = our Silent step.
        if (filter == NotificationManager.INTERRUPTION_FILTER_NONE ||
            filter == NotificationManager.INTERRUPTION_FILTER_ALARMS
        ) {
            return RingerProfile.SILENT
        }
        return when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_SILENT -> RingerProfile.SILENT
            AudioManager.RINGER_MODE_VIBRATE -> RingerProfile.VIBRATE
            else -> RingerProfile.RING
        }
    }

    private fun applySoundProfile(profile: RingerProfile) {
        when (profile) {
            RingerProfile.RING -> {
                notificationManager.setInterruptionFilter(
                    NotificationManager.INTERRUPTION_FILTER_ALL,
                )
                audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL
            }
            RingerProfile.VIBRATE -> {
                notificationManager.setInterruptionFilter(
                    NotificationManager.INTERRUPTION_FILTER_ALL,
                )
                audioManager.ringerMode = AudioManager.RINGER_MODE_VIBRATE
            }
            RingerProfile.SILENT -> {
                // Zen on first — OxygenOS applies true silence through Zen.
                notificationManager.setInterruptionFilter(
                    NotificationManager.INTERRUPTION_FILTER_NONE,
                )
                runCatching {
                    audioManager.ringerMode = AudioManager.RINGER_MODE_SILENT
                }.onFailure { Log.w(TAG, "RINGER_MODE_SILENT rejected", it) }
            }
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

    private fun performHaptic(intensity: HapticIntensity) {
        val vibrator = context.getSystemService(VibratorManager::class.java)?.defaultVibrator
            ?: context.getSystemService(Vibrator::class.java)
        val effect = when (intensity) {
            HapticIntensity.LIGHT ->
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_TICK)
            HapticIntensity.MEDIUM ->
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_CLICK)
            HapticIntensity.HEAVY ->
                VibrationEffect.createPredefined(VibrationEffect.EFFECT_HEAVY_CLICK)
        }
        vibrator?.vibrate(effect)
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
