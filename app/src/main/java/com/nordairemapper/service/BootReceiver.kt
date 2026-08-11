package com.nordairemapper.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED &&
            intent?.action != Intent.ACTION_LOCKED_BOOT_COMPLETED
        ) {
            return
        }
        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = settingsRepository.settings.first()
                if (!settings.serviceEnabled) return@launch
                if (settings.detectionStrategy == DetectionStrategy.LOGCAT &&
                    LogcatWatcherService.hasReadLogsPermission(context)
                ) {
                    LogcatWatcherService.start(context)
                }
                if (!AccessibilityUtils.isServiceEnabled(context)) {
                    ServiceNotifications.notifyDetectionStopped(context)
                }
            } finally {
                pending.finish()
            }
        }
    }
}
