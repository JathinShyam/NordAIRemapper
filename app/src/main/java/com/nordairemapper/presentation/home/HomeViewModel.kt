package com.nordairemapper.presentation.home

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nordairemapper.domain.model.DetectionStrategy
import com.nordairemapper.domain.model.PressType
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.domain.repository.RemapConfigRepository
import com.nordairemapper.domain.repository.SettingsRepository
import com.nordairemapper.presentation.common.conflictKey
import com.nordairemapper.service.AccessibilityUtils
import com.nordairemapper.service.DetectionCoordinator
import com.nordairemapper.service.KeyAction
import com.nordairemapper.service.KeyEventBus
import com.nordairemapper.service.LogVisibilityProbe
import com.nordairemapper.service.LogcatWatcherService
import com.nordairemapper.service.ServiceNotifications
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import android.util.Log
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val settingsRepository: SettingsRepository,
    private val remapConfigRepository: RemapConfigRepository,
    private val keyEventBus: KeyEventBus,
) : ViewModel() {

    private val runtimeFlags = MutableStateFlow(
        RuntimeFlags(
            accessibilityEnabled = AccessibilityUtils.isServiceEnabled(context),
            readLogsGranted = LogcatWatcherService.hasReadLogsPermission(context),
        )
    )

    /**
     * 10s heartbeat so "last used Xs ago" stays honest while Home is visible.
     * Runs only while the UI subscribes (stateIn WhileSubscribed).
     */
    private val ticker = flow {
        while (true) {
            emit(System.currentTimeMillis())
            delay(10_000L)
        }
    }

    private val _plusKeyPulse = MutableSharedFlow<Unit>(extraBufferCapacity = 8)
    /** Fires when a Plus Key DOWN/PULSE arrives so Home can flash the silhouette. */
    val plusKeyPulse: SharedFlow<Unit> = _plusKeyPulse.asSharedFlow()

    init {
        viewModelScope.launch {
            // Only flash for the actual Plus Key: logcat lines are pre-matched
            // by pattern, accessibility events must match the learned identity.
            // Volume/power arriving via Accessibility must not flash it.
            combine(keyEventBus.rawEvents, settingsRepository.settings) { event, settings ->
                event to settings.keyIdentity
            }.collect { (event, identity) ->
                if (event.action != KeyAction.DOWN && event.action != KeyAction.PULSE) return@collect
                val isPlusKey = event.source == DetectionStrategy.LOGCAT ||
                    (identity.isConfigured && run {
                        if (identity.scanCode > 0) {
                            event.scanCode == identity.scanCode
                        } else {
                            event.keyCode == identity.keyCode
                        }
                    })
                if (isPlusKey) _plusKeyPulse.tryEmit(Unit)
            }
        }
    }

    val uiState: StateFlow<HomeUiState> = combine(
        settingsRepository.settings,
        remapConfigRepository.observeConfigs(),
        runtimeFlags,
        ticker,
    ) { settings, actions, flags, nowMs ->
        HomeUiState(
            serviceEnabled = settings.serviceEnabled,
            accessibilityEnabled = flags.accessibilityEnabled,
            detectionStrategy = settings.detectionStrategy,
            keyConfigured = settings.keyIdentity.isConfigured,
            readLogsGranted = flags.readLogsGranted,
            tailSawNonSelf = flags.tailSawNonSelf,
            notificationsEnabled = flags.notificationsEnabled,
            actions = actions,
            conflictPressTypes = conflictPressTypes(actions),
            lastPlusKeySeenAtMs = settings.lastPlusKeySeenAtMs,
            nowMs = nowMs,
            banner = buildBanner(
                serviceEnabled = settings.serviceEnabled,
                accessibilityEnabled = flags.accessibilityEnabled,
                strategy = settings.detectionStrategy,
                keyConfigured = settings.keyIdentity.isConfigured,
                readLogsGranted = flags.readLogsGranted,
                hasAnyAction = actions.values.any { it !is RemapAction.None },
            ),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun refreshRuntimeFlags() {
        runtimeFlags.update {
            RuntimeFlags(
                accessibilityEnabled = AccessibilityUtils.isServiceEnabled(context),
                readLogsGranted = LogcatWatcherService.hasReadLogsPermission(context),
                tailSawNonSelf = LogcatWatcherService.hasTailSeenNonSelf(),
                notificationsEnabled = isNotificationsEnabled(),
            )
        }
        healBlindTailIfNeeded()
    }

    private fun isNotificationsEnabled(): Boolean =
        context.getSystemService(NotificationManager::class.java)
            ?.areNotificationsEnabled() ?: true

    /**
     * Post-boot self-heal for ColorOS per-boot log consent: spawns made in the
     * background are denied even with READ_LOGS granted, so the boot-time
     * watcher tail is born blind while this foreground probe can succeed.
     * When visibility is proven and the live tail is blind, reconnect it —
     * no adb, no Settings trip. Gated so we do not probe on every resume.
     */
    private fun healBlindTailIfNeeded() {
        viewModelScope.launch {
            ServiceNotifications.clearOpenAfterBoot(context)
            if (!LogcatWatcherService.hasReadLogsPermission(context)) return@launch
            if (LogcatWatcherService.hasTailSeenNonSelf()) {
                ServiceNotifications.clearLogsBlind(context)
                return@launch
            }
            // Tail never saw another app's line: per-boot consent is missing,
            // or it was granted only after this connection already existed.
            // ColorOS applies Allow to FUTURE connections, so probe once to
            // surface the prompt, then reconnect unconditionally — the fresh
            // tail inherits whatever consent is now registered. Deterministic:
            // no timed windows, at most one extra open after an Allow.
            LogVisibilityProbe.probe(CONSENT_PROBE_MS)
            Log.i("HomeHeal", "Reconnecting consent-blind tail")
            LogcatWatcherService.restart(context)
            ServiceNotifications.clearLogsBlind(context)
        }
    }

    fun setServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.setServiceEnabled(enabled)
            DetectionCoordinator.syncLogcatWatcher(
                context = context,
                strategy = uiState.value.detectionStrategy,
                serviceEnabled = enabled,
            )
        }
    }

    fun openAccessibilitySettings() = AccessibilityUtils.openAccessibilitySettings(context)

    /** Deep link to Keyforge's system notification settings (failure alerts). */
    fun openAppNotificationSettings() {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        runCatching { context.startActivity(intent) }
    }

    private data class RuntimeFlags(
        val accessibilityEnabled: Boolean,
        val readLogsGranted: Boolean,
        val tailSawNonSelf: Boolean = true,
        val notificationsEnabled: Boolean = true,
    )

    companion object {
        /** Long enough for the consent prompt to be surfaced by the spawn. */
        private const val CONSENT_PROBE_MS = 1_500L

        fun conflictPressTypes(actions: Map<PressType, RemapAction>): Set<PressType> {
            val groups = actions.entries
                .filter { it.value !is RemapAction.None }
                .groupBy { it.value.conflictKey() }
            return groups.values
                .filter { it.size > 1 }
                .flatten()
                .map { it.key }
                .toSet()
        }

        fun buildBanner(
            serviceEnabled: Boolean,
            accessibilityEnabled: Boolean,
            strategy: DetectionStrategy,
            keyConfigured: Boolean,
            readLogsGranted: Boolean,
            hasAnyAction: Boolean = false,
        ): HomeBanner? {
            if (!accessibilityEnabled) {
                return HomeBanner(
                    title = "Accessibility is off",
                    body = "Enable Keyforge in Accessibility settings so key presses and system actions can run. Tip: set the stock Plus Key to a harmless default — the system action may still fire.",
                    primaryLabel = "Open Accessibility",
                    primaryAction = HomeBannerAction.OPEN_ACCESSIBILITY,
                )
            }
            when (strategy) {
                DetectionStrategy.ACCESSIBILITY -> {
                    if (!readLogsGranted && !keyConfigured) {
                        return HomeBanner(
                            title = "Plus Key needs logcat on Nord 5",
                            body = "Accessibility sees volume keys, but OxygenOS handles the Plus Key in system code so it never reaches Key setup. Unlock detection once (USB preferred; Wireless is advanced) so logcat can detect it. Accessibility stays on for system actions.",
                            primaryLabel = "Unlock detection",
                            primaryAction = HomeBannerAction.OPEN_ENABLE_DETECTION,
                        )
                    }
                    if (!keyConfigured && readLogsGranted) {
                        return HomeBanner(
                            title = "Test Plus Key detection",
                            body = "READ_LOGS is granted. Open Key setup and press the Plus Key — you should see a logcat row. No keyCode save is needed for logcat.",
                            primaryLabel = "Key setup",
                            primaryAction = HomeBannerAction.OPEN_KEY_LEARNING,
                        )
                    }
                }
                DetectionStrategy.AUTO -> {
                    if (!readLogsGranted) {
                        return HomeBanner(
                            title = "READ_LOGS required on Nord 5",
                            body = "Auto mode uses Accessibility when the OS delivers the key, and logcat when it does not (Nord 5). Unlock once with USB (preferred) or Wireless (advanced).",
                            primaryLabel = "Unlock detection",
                            primaryAction = HomeBannerAction.OPEN_ENABLE_DETECTION,
                        )
                    }
                }
                DetectionStrategy.LOGCAT -> if (!readLogsGranted) {
                    return HomeBanner(
                        title = "READ_LOGS required",
                        body = "Logcat detection needs a one-time READ_LOGS grant. Preferred: USB from a computer. Wireless debugging is the advanced path.",
                        primaryLabel = "Unlock detection",
                        primaryAction = HomeBannerAction.OPEN_ENABLE_DETECTION,
                    )
                }
            }
            if (!serviceEnabled) {
                // Paused must outrank "assign presses": the watcher is stopped,
                // so claiming "Keyforge sees every press" here would be false.
                return HomeBanner(
                    title = "Remapping is paused",
                    body = "Turn on the master toggle when you want the Plus Key to run your actions.",
                    primaryLabel = "Key setup",
                    primaryAction = HomeBannerAction.OPEN_KEY_LEARNING,
                )
            }
            if (!hasAnyAction) {
                // Keyforge SEES every press, but a fresh install (or wiped
                // data) ships with no actions — presses look like "detection
                // is broken" when detection is actually fine.
                return HomeBanner(
                    title = "Assign your presses",
                    body = "Keyforge sees every Plus Key press. Choose what Single, Double, and Long press should do.",
                    primaryLabel = "Key setup",
                    primaryAction = HomeBannerAction.OPEN_KEY_LEARNING,
                )
            }
            return null
        }
    }
}
