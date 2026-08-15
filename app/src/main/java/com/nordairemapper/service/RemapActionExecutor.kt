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
 * - **Ring / vibrate / silent** never requests DND access and never calls
 *   interruption-filter APIs. Silent prefers hidden `setRingerModeInternal`
 *   (aborts if DND filter would change), else stream mute. Plain DND is only
 *   [RemapAction.ToggleDoNotDisturb].
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

    /**
     * Last verified ring/vibrate/silent step for a clean 3-way cycle.
     */
    @Volatile
    private var confirmedRingerProfile: RingerProfile? = null

    /** Silent via hidden [AudioManager.setRingerModeInternal] (Oplus/Lineage pattern). */
    @Volatile
    private var silentInternalActive = false

    /** Silent via stream mute when internal silent is unavailable. */
    @Volatile
    private var silentStreamMuteActive = false

    private var savedRingVolumeForSilent = -1
    private var savedNotificationVolumeForSilent = -1
    private var savedSystemVolumeForSilent = -1
    private var savedVibrateWhenRinging: Int? = null
    private var savedVibrateSettingRinger: Int? = null

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
     * Ring → vibrate → silent with **zero DND involvement**.
     *
     * Hard rules:
     * - Never request Do Not Disturb access.
     * - Never call [NotificationManager.setInterruptionFilter].
     * - Never use public [AudioManager.RINGER_MODE_SILENT] (that API is Zen on N+).
     *
     * Silent research (what works without lasting DND):
     * - Oplus/Lineage alert-slider code uses hidden
     *   `AudioManager.setRingerModeInternal(SILENT)` with Zen left OFF — real silent,
     *   no DND tile. We try that first and **abort if the interruption filter changes**.
     * - Fallback: mute RING + NOTIFICATION + SYSTEM with
     *   [AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE], disable vibrate-when-ringing /
     *   vibrate setting — never `setStreamVolume(..., 0)` (maps to vibrate / DND).
     */
    private fun cycleRingerMode() {
        if (audioManager.isVolumeFixed) {
            toast("Ringer is fixed on this device")
            return
        }

        val current = confirmedRingerProfile ?: readRingerProfileNoDnd()
        val next = when (current) {
            RingerProfile.RING -> RingerProfile.VIBRATE
            RingerProfile.VIBRATE -> RingerProfile.SILENT
            RingerProfile.SILENT -> RingerProfile.RING
        }

        applyRingerProfileNoDnd(next)
        mainHandler.postDelayed({
            // Soft re-assert only if OEM dropped the target.
            if (readRingerProfileNoDnd() != next) {
                applyRingerProfileNoDnd(next)
            }
            val now = readRingerProfileNoDnd()
            if (now == next) {
                confirmedRingerProfile = next
                toast(next.label())
            } else {
                confirmedRingerProfile = now
                Log.w(
                    TAG,
                    "Ringer cycle: wanted=$next got=$now " +
                        "ext=${audioManager.ringerMode} int=${ringerModeInternalOrExt()} " +
                        "mute=${isRingMuted()} internalSilent=$silentInternalActive " +
                        "muteSilent=$silentStreamMuteActive " +
                        "filter=${notificationManager.currentInterruptionFilter}",
                )
                toast(
                    if (next == RingerProfile.SILENT) {
                        "Silent failed (still ${now.label()})"
                    } else {
                        "Ringer stuck on ${now.label()}"
                    },
                )
            }
        }, 100L)
    }

    private enum class RingerProfile { RING, VIBRATE, SILENT }

    private fun RingerProfile.label(): String = when (this) {
        RingerProfile.RING -> "Ring"
        RingerProfile.VIBRATE -> "Vibrate"
        RingerProfile.SILENT -> "Silent"
    }

    private fun readRingerProfileNoDnd(): RingerProfile {
        if (silentInternalActive && isInternalSilentHeld()) return RingerProfile.SILENT
        if (silentStreamMuteActive && isMuteSilentHeld()) return RingerProfile.SILENT

        // Stale flags
        if (silentInternalActive && !isInternalSilentHeld()) silentInternalActive = false
        if (silentStreamMuteActive && !isMuteSilentHeld()) silentStreamMuteActive = false

        return when (audioManager.ringerMode) {
            AudioManager.RINGER_MODE_VIBRATE -> RingerProfile.VIBRATE
            // External SILENT without our internal flag = system/DND silent; leave alone.
            AudioManager.RINGER_MODE_SILENT -> RingerProfile.VIBRATE
            else -> RingerProfile.RING
        }
    }

    private fun isInternalSilentHeld(): Boolean {
        val mode = ringerModeInternalOrExt()
        return mode == AudioManager.RINGER_MODE_SILENT ||
            audioManager.ringerMode == AudioManager.RINGER_MODE_SILENT
    }

    private fun isMuteSilentHeld(): Boolean {
        if (audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE) return false
        if (isRingMuted()) return true
        return silentStreamMuteActive &&
            audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL
    }

    private fun isRingMuted(): Boolean =
        runCatching { audioManager.isStreamMute(AudioManager.STREAM_RING) }.getOrDefault(false)

    private fun applyRingerProfileNoDnd(profile: RingerProfile): Boolean {
        return when (profile) {
            RingerProfile.RING -> applyRingNoDnd()
            RingerProfile.VIBRATE -> applyVibrateNoDnd()
            RingerProfile.SILENT -> applySilentNoDnd()
        }
    }

    private fun applyRingNoDnd(): Boolean {
        exitSilentLayers()
        restoreVibrationPrefs()
        setRingerModePreferInternal(AudioManager.RINGER_MODE_NORMAL)
        ensureRingAudible()
        return audioManager.ringerMode == AudioManager.RINGER_MODE_NORMAL ||
            ringerModeInternalOrExt() == AudioManager.RINGER_MODE_NORMAL
    }

    private fun applyVibrateNoDnd(): Boolean {
        exitSilentLayers()
        restoreVibrationPrefs()
        setRingerModePreferInternal(AudioManager.RINGER_MODE_VIBRATE)
        return audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE ||
            ringerModeInternalOrExt() == AudioManager.RINGER_MODE_VIBRATE
    }

    /**
     * Silent without turning DND on.
     *
     * 1) `setRingerModeInternal(SILENT)` if the interruption filter stays unchanged.
     * 2) Else comprehensive stream mute + vibration off (no volume-0, no public SILENT).
     */
    private fun applySilentNoDnd(): Boolean {
        val filterBefore = notificationManager.currentInterruptionFilter

        // Leave vibrate mode first so mute/internal silent is not "vibrate".
        setRingerModePreferInternal(AudioManager.RINGER_MODE_NORMAL)
        disableVibrationForSilent()

        if (tryInternalSilentWithoutDnd(filterBefore)) {
            silentInternalActive = true
            silentStreamMuteActive = false
            return true
        }

        silentInternalActive = false
        return applyMuteSilentWithoutDnd()
    }

    /**
     * Oplus/Lineage pattern: internal silent keeps Zen off. If filter moves, undo.
     */
    private fun tryInternalSilentWithoutDnd(filterBefore: Int): Boolean {
        if (!setRingerModeInternal(AudioManager.RINGER_MODE_SILENT)) return false

        val filterAfter = notificationManager.currentInterruptionFilter
        if (filterAfter != filterBefore) {
            Log.i(TAG, "Internal silent changed interruption filter ($filterBefore→$filterAfter); undoing")
            setRingerModeInternal(AudioManager.RINGER_MODE_NORMAL)
            runCatching { audioManager.ringerMode = AudioManager.RINGER_MODE_NORMAL }
            return false
        }

        val held = isInternalSilentHeld()
        if (!held) {
            Log.i(TAG, "Internal silent did not stick (ext=${audioManager.ringerMode})")
        }
        return held
    }

    private fun applyMuteSilentWithoutDnd(): Boolean {
        if (!savedVolumesYet()) {
            saveStreamVolumesForSilent()
        }

        muteStream(AudioManager.STREAM_RING)
        muteStream(AudioManager.STREAM_NOTIFICATION)
        muteStream(AudioManager.STREAM_SYSTEM)
        // Deprecated but still effective on many OEMs for call buzz.
        runCatching {
            @Suppress("DEPRECATION")
            audioManager.setStreamMute(AudioManager.STREAM_RING, true)
            @Suppress("DEPRECATION")
            audioManager.setStreamMute(AudioManager.STREAM_NOTIFICATION, true)
        }

        if (audioManager.ringerMode == AudioManager.RINGER_MODE_VIBRATE) {
            Log.w(TAG, "Mute silent collapsed to vibrate")
            clearMuteSilentLayers()
            restoreVibrationPrefs()
            return false
        }

        // Guard: never leave DND changed by mute either.
        silentStreamMuteActive = true
        return isMuteSilentHeld()
    }

    private fun muteStream(stream: Int) {
        runCatching {
            audioManager.adjustStreamVolume(
                stream,
                AudioManager.ADJUST_MUTE,
                AudioManager.FLAG_REMOVE_SOUND_AND_VIBRATE,
            )
        }.onFailure { Log.w(TAG, "ADJUST_MUTE failed for stream=$stream", it) }
    }

    private fun unmuteStream(stream: Int) {
        runCatching {
            audioManager.adjustStreamVolume(stream, AudioManager.ADJUST_UNMUTE, 0)
        }
        runCatching {
            @Suppress("DEPRECATION")
            audioManager.setStreamMute(stream, false)
        }
    }

    private fun exitSilentLayers() {
        if (silentInternalActive) {
            silentInternalActive = false
            setRingerModePreferInternal(AudioManager.RINGER_MODE_NORMAL)
        }
        clearMuteSilentLayers()
    }

    private fun clearMuteSilentLayers() {
        if (!silentStreamMuteActive &&
            savedRingVolumeForSilent < 0 &&
            savedNotificationVolumeForSilent < 0 &&
            savedSystemVolumeForSilent < 0
        ) {
            return
        }
        silentStreamMuteActive = false
        unmuteStream(AudioManager.STREAM_RING)
        unmuteStream(AudioManager.STREAM_NOTIFICATION)
        unmuteStream(AudioManager.STREAM_SYSTEM)
        restoreStreamVolumesAfterSilent()
    }

    private fun savedVolumesYet(): Boolean =
        savedRingVolumeForSilent > 0 ||
            savedNotificationVolumeForSilent > 0 ||
            savedSystemVolumeForSilent > 0

    private fun saveStreamVolumesForSilent() {
        val ring = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        val notif = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        val system = audioManager.getStreamVolume(AudioManager.STREAM_SYSTEM)
        if (ring > 0) savedRingVolumeForSilent = ring
        if (notif > 0) savedNotificationVolumeForSilent = notif
        if (system > 0) savedSystemVolumeForSilent = system
    }

    private fun restoreStreamVolumesAfterSilent() {
        val ringMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        val notifMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
        val systemMax = audioManager.getStreamMaxVolume(AudioManager.STREAM_SYSTEM)
        if (savedRingVolumeForSilent > 0) {
            runCatching {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_RING,
                    savedRingVolumeForSilent.coerceAtMost(ringMax),
                    0,
                )
            }
        }
        if (savedNotificationVolumeForSilent > 0) {
            runCatching {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_NOTIFICATION,
                    savedNotificationVolumeForSilent.coerceAtMost(notifMax),
                    0,
                )
            }
        }
        if (savedSystemVolumeForSilent > 0) {
            runCatching {
                audioManager.setStreamVolume(
                    AudioManager.STREAM_SYSTEM,
                    savedSystemVolumeForSilent.coerceAtMost(systemMax),
                    0,
                )
            }
        }
        savedRingVolumeForSilent = -1
        savedNotificationVolumeForSilent = -1
        savedSystemVolumeForSilent = -1
    }

    private fun ensureRingAudible() {
        if (isRingMuted()) {
            unmuteStream(AudioManager.STREAM_RING)
        }
        val vol = audioManager.getStreamVolume(AudioManager.STREAM_RING)
        if (vol > 0) return
        runCatching {
            audioManager.adjustStreamVolume(
                AudioManager.STREAM_RING,
                AudioManager.ADJUST_RAISE,
                0,
            )
        }
        if (audioManager.getStreamVolume(AudioManager.STREAM_RING) > 0) return
        val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING)
        runCatching {
            audioManager.setStreamVolume(
                AudioManager.STREAM_RING,
                (max / 2).coerceAtLeast(1),
                0,
            )
        }
    }

    private fun disableVibrationForSilent() {
        runCatching {
            @Suppress("DEPRECATION")
            if (savedVibrateSettingRinger == null) {
                savedVibrateSettingRinger =
                    audioManager.getVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER)
            }
            @Suppress("DEPRECATION")
            audioManager.setVibrateSetting(
                AudioManager.VIBRATE_TYPE_RINGER,
                AudioManager.VIBRATE_SETTING_OFF,
            )
        }
        if (!Settings.System.canWrite(context)) return
        runCatching {
            if (savedVibrateWhenRinging == null) {
                savedVibrateWhenRinging = Settings.System.getInt(
                    context.contentResolver,
                    Settings.System.VIBRATE_WHEN_RINGING,
                    0,
                )
            }
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.VIBRATE_WHEN_RINGING,
                0,
            )
        }
    }

    private fun restoreVibrationPrefs() {
        savedVibrateSettingRinger?.let { saved ->
            savedVibrateSettingRinger = null
            runCatching {
                @Suppress("DEPRECATION")
                audioManager.setVibrateSetting(AudioManager.VIBRATE_TYPE_RINGER, saved)
            }
        }
        val saved = savedVibrateWhenRinging ?: return
        savedVibrateWhenRinging = null
        if (!Settings.System.canWrite(context)) return
        runCatching {
            Settings.System.putInt(
                context.contentResolver,
                Settings.System.VIBRATE_WHEN_RINGING,
                saved,
            )
        }
    }

    /** Prefer hidden internal setter (does not go through Zen external path). */
    private fun setRingerModePreferInternal(mode: Int) {
        if (setRingerModeInternal(mode)) return
        runCatching { audioManager.ringerMode = mode }
            .onFailure { Log.w(TAG, "setRingerMode($mode) failed", it) }
    }

    private fun setRingerModeInternal(mode: Int): Boolean =
        runCatching {
            AudioManager::class.java
                .getMethod("setRingerModeInternal", Int::class.javaPrimitiveType)
                .invoke(audioManager, mode)
            true
        }.getOrDefault(false)

    private fun ringerModeInternalOrExt(): Int =
        runCatching {
            AudioManager::class.java
                .getMethod("getRingerModeInternal")
                .invoke(audioManager) as Int
        }.getOrDefault(audioManager.ringerMode)

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
