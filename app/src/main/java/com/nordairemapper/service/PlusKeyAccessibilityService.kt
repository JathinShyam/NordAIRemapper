package com.nordairemapper.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.nordairemapper.domain.model.DetectionStrategy
import com.nordairemapper.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Detection strategy A: observes hardware key events via the accessibility
 * key-filtering API. Kept deliberately thin — every event is forwarded to the
 * KeyEventBus and all decisions happen in RemapEngine, so the key-dispatch
 * path never blocks.
 *
 * Optional: when [pauseAccessibilityInExcludedApps] is on, opening an excluded
 * app soft-disables Accessibility (via Secure Settings after Wireless Unlock)
 * so banking/UPI apps no longer see Keyforge; [AccessibilityAutoResumeService]
 * turns it back on when the user leaves. Without elevated grants, falls back
 * to [disableSelf] + a notification to re-enable manually.
 */
@AndroidEntryPoint
class PlusKeyAccessibilityService : AccessibilityService() {

    @Inject lateinit var keyEventBus: KeyEventBus
    @Inject lateinit var remapEngine: RemapEngine
    @Inject lateinit var foregroundAppTracker: ForegroundAppTracker
    @Inject lateinit var settingsRepository: SettingsRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var settingsJob: Job? = null

    @Volatile private var pauseAccessibilityInExcludedApps: Boolean = false
    @Volatile private var excludedApps: Set<String> = emptySet()
    @Volatile private var pauseTriggered: Boolean = false

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo?.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        AccessibilityServiceHolder.service = this
        pauseTriggered = false
        remapEngine.start()
        DetectionCoordinator.syncLogcatWatcher(
            context = this,
            strategy = remapEngine.currentStrategy(),
            serviceEnabled = remapEngine.isServiceEnabled(),
        )
        settingsJob?.cancel()
        settingsJob = scope.launch {
            settingsRepository.settings.collectLatest { settings ->
                pauseAccessibilityInExcludedApps = settings.pauseAccessibilityInExcludedApps
                excludedApps = settings.excludedApps
            }
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        val action = when (event.action) {
            KeyEvent.ACTION_DOWN -> KeyAction.DOWN
            KeyEvent.ACTION_UP -> KeyAction.UP
            else -> return false
        }
        keyEventBus.emit(
            RawKeyEvent(
                keyCode = event.keyCode,
                scanCode = event.scanCode,
                action = action,
                timestampMs = System.currentTimeMillis(),
                source = DetectionStrategy.ACCESSIBILITY,
            )
        )
        return remapEngine.shouldConsume(event.keyCode, event.scanCode)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString()
        foregroundAppTracker.onWindowStateChanged(pkg)
        maybePauseForExcludedApp(pkg)
    }

    private fun maybePauseForExcludedApp(pkg: String?) {
        if (!pauseAccessibilityInExcludedApps || pauseTriggered) return
        if (pkg.isNullOrBlank() || pkg == packageName) return
        if (pkg !in excludedApps) return
        pauseTriggered = true
        val label = runCatching {
            packageManager.getApplicationLabel(packageManager.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)
        Log.i(TAG, "Pausing Accessibility for excluded app: $pkg")

        // Hands-free path: soft-disable + Usage-Stats watcher restores us when the
        // banking app leaves. Requires one-time Wireless Unlock (WRITE_SECURE_SETTINGS
        // + usage access). Fallback: disableSelf + tap notification to re-enable.
        if (ElevatedPermissions.canAutoResumeAccessibility(this)) {
            AccessibilityAutoResumeService.start(this, pausedPackage = pkg, appLabel = label)
            val disabled = AccessibilitySecureToggle.setEnabled(this, enabled = false)
            if (disabled) return
            Log.w(TAG, "Secure toggle failed; falling back to disableSelf()")
            AccessibilityAutoResumeService.stop(this)
        }
        ServiceNotifications.notifyAccessibilityPaused(this, label)
        disableSelf()
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        settingsJob?.cancel()
        scope.cancel()
        AccessibilityServiceHolder.service = null
        if (!pauseTriggered) {
            ServiceNotifications.notifyDetectionStopped(this)
        }
        super.onDestroy()
    }

    companion object {
        private const val TAG = "PlusKeyA11y"
    }
}

/**
 * Global handle to the running accessibility service so action executors can
 * call performGlobalAction() (screenshot, lock, recents, ...). Null when the
 * service is not connected.
 */
object AccessibilityServiceHolder {
    @Volatile
    var service: PlusKeyAccessibilityService? = null
}
