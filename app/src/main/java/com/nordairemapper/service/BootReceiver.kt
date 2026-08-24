package com.nordairemapper.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.nordairemapper.domain.model.DetectionStrategy
import com.nordairemapper.domain.repository.SettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) {
            return
        }
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                runCatching {
                    val settings = settingsRepository.settings.first()
                    if (!settings.serviceEnabled) return@runCatching
                    DetectionCoordinator.syncLogcatWatcher(
                        context = context,
                        strategy = settings.detectionStrategy,
                        serviceEnabled = settings.serviceEnabled,
                    )
                    if (!AccessibilityUtils.isServiceEnabled(context)) {
                        ServiceNotifications.notifyDetectionStopped(context)
                    }
                    // Per-boot log consent: the boot-time watcher tail is blind
                    // until Keyforge is opened once in the foreground. Nudge.
                    if (LogcatWatcherService.hasReadLogsPermission(context) &&
                        settings.detectionStrategy != DetectionStrategy.ACCESSIBILITY
                    ) {
                        ServiceNotifications.notifyOpenAfterBoot(context)
                    }
                }.onFailure { Log.w("BootReceiver", "Boot re-arm failed", it) }
            } finally {
                pending.finish()
            }
        }
    }
}
