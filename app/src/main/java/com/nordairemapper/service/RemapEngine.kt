package com.nordairemapper.service

import android.app.KeyguardManager
import android.content.Context
import android.util.Log
import com.nordairemapper.domain.model.AppSettings
import com.nordairemapper.domain.model.DetectionStrategy
import com.nordairemapper.domain.model.Gesture
import com.nordairemapper.domain.model.PressType
import com.nordairemapper.domain.repository.RemapConfigRepository
import com.nordairemapper.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Central detection pipeline: collects raw key events from all detector
 * sources, filters them down to the learned Plus Key, classifies gestures,
 * resolves the configured action and dispatches it.
 */
@Singleton
class RemapEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val keyEventBus: KeyEventBus,
    private val settingsRepository: SettingsRepository,
    private val remapConfigRepository: RemapConfigRepository,
    private val actionDispatcher: ActionDispatcher,
    private val foregroundAppTracker: ForegroundAppTracker,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    @Volatile
    private var settings = AppSettings()

    private var started = false

    private val classifier = GestureClassifier(
        scope = scope,
        timings = {
            GestureClassifier.Timings(
                doublePressWindowMs = settings.doublePressWindowMs,
                longPressThresholdMs = settings.longPressThresholdMs,
            )
        },
        onGesture = ::onGesture,
    )

    fun start() {
        if (started) return
        started = true

        scope.launch {
            settingsRepository.settings.collect { next ->
                val previous = settings
                settings = next
                // Only restart/sync the watcher when detection-relevant fields
                // actually change; every unrelated toggle used to call start()
                // again (LogcatWatcherService now also self-guards).
                if (previous.detectionStrategy != next.detectionStrategy ||
                    previous.serviceEnabled != next.serviceEnabled
                ) {
                    DetectionCoordinator.syncLogcatWatcher(
                        context = context,
                        strategy = next.detectionStrategy,
                        serviceEnabled = next.serviceEnabled,
                    )
                }
            }
        }
        scope.launch {
            keyEventBus.rawEvents.collect { event ->
                if (!settings.serviceEnabled) return@collect
                if (!DetectionCoordinator.acceptsSource(settings.detectionStrategy, event.source)) {
                    return@collect
                }
                when (event.action) {
                    KeyAction.DOWN -> if (isPlusKeyEvent(event)) classifier.onKeyDown()
                    KeyAction.UP -> if (isPlusKeyEvent(event)) classifier.onKeyUp()
                    KeyAction.PULSE -> classifier.onPulse()
                }
            }
        }
    }

    fun currentStrategy(): DetectionStrategy = settings.detectionStrategy

    fun isServiceEnabled(): Boolean = settings.serviceEnabled

    /**
     * Logcat events are already filtered by the match pattern and use
     * placeholder key codes (-1). Accessibility events must match the
     * learned Plus Key identity.
     */
    private fun isPlusKeyEvent(event: RawKeyEvent): Boolean =
        event.source == DetectionStrategy.LOGCAT ||
            matchesPlusKey(event.keyCode, event.scanCode)

    /**
     * Whether an accessibility key event belongs to the learned Plus Key.
     * When the key reports KEYCODE_UNKNOWN (0), the scanCode is the only
     * reliable identity, so it takes priority when configured.
     */
    fun matchesPlusKey(keyCode: Int, scanCode: Int): Boolean {
        val identity = settings.keyIdentity
        if (!identity.isConfigured) return false
        return if (identity.scanCode > 0) {
            scanCode == identity.scanCode
        } else {
            keyCode == identity.keyCode
        }
    }

    /** Whether the accessibility service should consume this key event.
     *  Never consumes while Key setup is open: learning must not swallow keys. */
    fun shouldConsume(keyCode: Int, scanCode: Int): Boolean {
        if (LearningMode.active) return false
        val strategy = settings.detectionStrategy
        val consumeViaAccessibility =
            strategy == DetectionStrategy.ACCESSIBILITY || strategy == DetectionStrategy.AUTO
        return settings.serviceEnabled &&
            consumeViaAccessibility &&
            matchesPlusKey(keyCode, scanCode)
    }

    private fun onGesture(gesture: Gesture) {
        val pressType = when (gesture) {
            Gesture.SINGLE_PRESS -> PressType.SINGLE
            Gesture.DOUBLE_PRESS -> PressType.DOUBLE
            Gesture.LONG_PRESS -> PressType.LONG
        }
        scope.launch {
            // Detection health signal (Home / Key setup): throttled to avoid a
            // DataStore write per physical press. Recorded even in learning mode
            // so "Last Plus Key press" stays truthful during setup.
            val now = System.currentTimeMillis()
            if (now - settings.lastPlusKeySeenAtMs > 1_000) {
                settingsRepository.setLastPlusKeySeen(now)
            }
            if (LearningMode.active) {
                Log.d(TAG, "Learning mode active; not dispatching $pressType")
                return@launch
            }
            val foreground = foregroundAppTracker.packageName
            if (foreground != null && foreground in settings.excludedApps) {
                Log.d(TAG, "Skipping gesture in excluded app: $foreground")
                return@launch
            }
            if (isDeviceLocked() && !isPressAllowedOnLockScreen(pressType)) {
                Log.d(TAG, "Skipping $pressType while locked")
                return@launch
            }
            val action = remapConfigRepository.getAction(pressType)
            Log.d(TAG, "Gesture $gesture -> $action")
            actionDispatcher.execute(action)
        }
    }

    private fun isDeviceLocked(): Boolean {
        val km = context.getSystemService(KeyguardManager::class.java) ?: return false
        return km.isKeyguardLocked
    }

    private fun isPressAllowedOnLockScreen(pressType: PressType): Boolean = when (pressType) {
        PressType.SINGLE -> settings.lockScreenSingleEnabled
        PressType.DOUBLE -> settings.lockScreenDoubleEnabled
        PressType.LONG -> settings.lockScreenLongEnabled
    }

    companion object {
        private const val TAG = "RemapEngine"
    }
}
