package com.nordairemapper.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import com.nordairemapper.domain.model.DetectionStrategy
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Detection strategy A: observes hardware key events via the accessibility
 * key-filtering API. Kept deliberately thin — every event is forwarded to the
 * KeyEventBus and all decisions happen in RemapEngine, so the key-dispatch
 * path never blocks.
 */
@AndroidEntryPoint
class PlusKeyAccessibilityService : AccessibilityService() {

    @Inject lateinit var keyEventBus: KeyEventBus
    @Inject lateinit var remapEngine: RemapEngine
    @Inject lateinit var foregroundAppTracker: ForegroundAppTracker

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo?.apply {
            flags = flags or AccessibilityServiceInfo.FLAG_REQUEST_FILTER_KEY_EVENTS
        }
        AccessibilityServiceHolder.service = this
        remapEngine.start()
        // Nord 5: Plus Key rarely reaches onKeyEvent — start logcat companion when permitted.
        DetectionCoordinator.syncLogcatWatcher(
            context = this,
            strategy = remapEngine.currentStrategy(),
            serviceEnabled = remapEngine.isServiceEnabled(),
        )
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
        // Consume only the learned Plus Key; every other key passes through untouched.
        return remapEngine.shouldConsume(event.keyCode, event.scanCode)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        foregroundAppTracker.packageName = pkg
    }

    override fun onInterrupt() = Unit

    override fun onDestroy() {
        AccessibilityServiceHolder.service = null
        ServiceNotifications.notifyDetectionStopped(this)
        super.onDestroy()
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
