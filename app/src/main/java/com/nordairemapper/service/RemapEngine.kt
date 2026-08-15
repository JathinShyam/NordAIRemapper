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
            settingsRepository.settings.collect { settings = it }
        }
        scope.launch {
            keyEventBus.rawEvents.collect { event ->
                if (!settings.serviceEnabled) return@collect
                if (event.source != settings.detectionStrategy) return@collect
                when (event.action) {
                    KeyAction.DOWN -> if (isPlusKeyEvent(event)) classifier.onKeyDown()
                    KeyAction.UP -> if (isPlusKeyEvent(event)) classifier.onKeyUp()
                    KeyAction.PULSE -> classifier.onPulse()
                }
            }
        }
    }

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

    /** Whether the accessibility service should consume this key event. */
    fun shouldConsume(keyCode: Int, scanCode: Int): Boolean =
        settings.serviceEnabled &&
            settings.detectionStrategy == DetectionStrategy.ACCESSIBILITY &&
            matchesPlusKey(keyCode, scanCode)

    private fun onGesture(gesture: Gesture) {
        val pressType = when (gesture) {
            Gesture.SINGLE_PRESS -> PressType.SINGLE
            Gesture.DOUBLE_PRESS -> PressType.DOUBLE
            Gesture.LONG_PRESS -> PressType.LONG
        }
        scope.launch {
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
