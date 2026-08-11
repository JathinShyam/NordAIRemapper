package com.nordairemapper.domain.repository

import com.nordairemapper.domain.model.AppSettings
import com.nordairemapper.domain.model.DetectionStrategy
import com.nordairemapper.domain.model.KeyIdentity
import com.nordairemapper.domain.model.ThemeMode
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<AppSettings>

    suspend fun setServiceEnabled(enabled: Boolean)
    suspend fun setDetectionStrategy(strategy: DetectionStrategy)
    suspend fun setKeyIdentity(identity: KeyIdentity)
    suspend fun setDoublePressWindowMs(value: Long)
    suspend fun setLongPressThresholdMs(value: Long)
    suspend fun setLogcatPattern(pattern: String)
    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setShowServiceNotification(enabled: Boolean)
    suspend fun setHapticFeedback(enabled: Boolean)
    suspend fun setExcludedApps(packages: Set<String>)
    suspend fun setOnboardingCompleted(completed: Boolean)
}
