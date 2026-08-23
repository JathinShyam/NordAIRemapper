package com.nordairemapper.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nordairemapper.domain.model.AppSettings
import com.nordairemapper.domain.model.DetectionStrategy
import com.nordairemapper.domain.model.HapticIntensity
import com.nordairemapper.domain.model.KeyIdentity
import com.nordairemapper.domain.model.ThemeMode
import com.nordairemapper.domain.repository.SettingsRepository
import com.nordairemapper.service.LogcatKeyParser
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
) : SettingsRepository {

    private object Keys {
        val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val DETECTION_STRATEGY = stringPreferencesKey("detection_strategy")
        val KEY_CODE = intPreferencesKey("key_code")
        val SCAN_CODE = intPreferencesKey("scan_code")
        val DOUBLE_PRESS_WINDOW = longPreferencesKey("double_press_window_ms")
        val LONG_PRESS_THRESHOLD = longPreferencesKey("long_press_threshold_ms")
        val LOGCAT_PATTERN = stringPreferencesKey("logcat_pattern")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val SHOW_NOTIFICATION = booleanPreferencesKey("show_service_notification")
        val HAPTIC_FEEDBACK = booleanPreferencesKey("haptic_feedback")
        val HAPTIC_INTENSITY = stringPreferencesKey("haptic_intensity")
        val VISUAL_OVERLAY_ENABLED = booleanPreferencesKey("visual_overlay_enabled")
        val LOCK_SINGLE = booleanPreferencesKey("lock_screen_single_enabled")
        val LOCK_DOUBLE = booleanPreferencesKey("lock_screen_double_enabled")
        val LOCK_LONG = booleanPreferencesKey("lock_screen_long_enabled")
        val EXCLUDED_APPS = stringSetPreferencesKey("excluded_apps")
        val PAUSE_ACCESSIBILITY_EXCLUDED = booleanPreferencesKey("pause_accessibility_in_excluded_apps")
        val ONBOARDING_COMPLETED = booleanPreferencesKey("onboarding_completed")
        val LAST_PLUS_KEY_SEEN = longPreferencesKey("last_plus_key_seen_ms")
    }

    private val defaults = AppSettings()

    override val settings: Flow<AppSettings> = context.settingsDataStore.data.map { prefs ->
        AppSettings(
            serviceEnabled = prefs[Keys.SERVICE_ENABLED] ?: defaults.serviceEnabled,
            detectionStrategy = DetectionStrategy.fromKey(
                prefs[Keys.DETECTION_STRATEGY] ?: defaults.detectionStrategy.key
            ),
            keyIdentity = KeyIdentity(
                keyCode = prefs[Keys.KEY_CODE] ?: KeyIdentity.UNCONFIGURED.keyCode,
                scanCode = prefs[Keys.SCAN_CODE] ?: KeyIdentity.UNCONFIGURED.scanCode,
            ),
            doublePressWindowMs = prefs[Keys.DOUBLE_PRESS_WINDOW] ?: defaults.doublePressWindowMs,
            longPressThresholdMs = prefs[Keys.LONG_PRESS_THRESHOLD] ?: defaults.longPressThresholdMs,
            logcatPattern = LogcatKeyParser.migratePattern(prefs[Keys.LOGCAT_PATTERN]),
            themeMode = prefs[Keys.THEME_MODE]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                ?: defaults.themeMode,
            dynamicColor = prefs[Keys.DYNAMIC_COLOR] ?: defaults.dynamicColor,
            showServiceNotification = prefs[Keys.SHOW_NOTIFICATION] ?: defaults.showServiceNotification,
            hapticFeedback = prefs[Keys.HAPTIC_FEEDBACK] ?: defaults.hapticFeedback,
            hapticIntensity = prefs[Keys.HAPTIC_INTENSITY]
                ?.let { runCatching { HapticIntensity.valueOf(it) }.getOrNull() }
                ?: defaults.hapticIntensity,
            visualOverlayEnabled = prefs[Keys.VISUAL_OVERLAY_ENABLED] ?: defaults.visualOverlayEnabled,
            lockScreenSingleEnabled = prefs[Keys.LOCK_SINGLE] ?: defaults.lockScreenSingleEnabled,
            lockScreenDoubleEnabled = prefs[Keys.LOCK_DOUBLE] ?: defaults.lockScreenDoubleEnabled,
            lockScreenLongEnabled = prefs[Keys.LOCK_LONG] ?: defaults.lockScreenLongEnabled,
            excludedApps = prefs[Keys.EXCLUDED_APPS] ?: defaults.excludedApps,
            pauseAccessibilityInExcludedApps = prefs[Keys.PAUSE_ACCESSIBILITY_EXCLUDED]
                ?: defaults.pauseAccessibilityInExcludedApps,
            onboardingCompleted = prefs[Keys.ONBOARDING_COMPLETED] ?: defaults.onboardingCompleted,
            lastPlusKeySeenAtMs = prefs[Keys.LAST_PLUS_KEY_SEEN] ?: defaults.lastPlusKeySeenAtMs,
        )
    }

    override suspend fun setServiceEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SERVICE_ENABLED] = enabled }
    }

    override suspend fun setDetectionStrategy(strategy: DetectionStrategy) {
        context.settingsDataStore.edit { it[Keys.DETECTION_STRATEGY] = strategy.key }
    }

    override suspend fun setKeyIdentity(identity: KeyIdentity) {
        context.settingsDataStore.edit {
            it[Keys.KEY_CODE] = identity.keyCode
            it[Keys.SCAN_CODE] = identity.scanCode
        }
    }

    override suspend fun setDoublePressWindowMs(value: Long) {
        context.settingsDataStore.edit {
            it[Keys.DOUBLE_PRESS_WINDOW] = value.coerceIn(AppSettings.DOUBLE_PRESS_WINDOW_RANGE)
        }
    }

    override suspend fun setLongPressThresholdMs(value: Long) {
        context.settingsDataStore.edit {
            it[Keys.LONG_PRESS_THRESHOLD] = value.coerceIn(AppSettings.LONG_PRESS_THRESHOLD_RANGE)
        }
    }

    override suspend fun setLogcatPattern(pattern: String) {
        context.settingsDataStore.edit { it[Keys.LOGCAT_PATTERN] = pattern }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.THEME_MODE] = mode.name }
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.DYNAMIC_COLOR] = enabled }
    }

    override suspend fun setShowServiceNotification(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SHOW_NOTIFICATION] = enabled }
    }

    override suspend fun setHapticFeedback(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.HAPTIC_FEEDBACK] = enabled }
    }

    override suspend fun setHapticIntensity(intensity: HapticIntensity) {
        context.settingsDataStore.edit { it[Keys.HAPTIC_INTENSITY] = intensity.name }
    }

    override suspend fun setVisualOverlayEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.VISUAL_OVERLAY_ENABLED] = enabled }
    }

    override suspend fun setLockScreenSingleEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.LOCK_SINGLE] = enabled }
    }

    override suspend fun setLockScreenDoubleEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.LOCK_DOUBLE] = enabled }
    }

    override suspend fun setLockScreenLongEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.LOCK_LONG] = enabled }
    }

    override suspend fun setExcludedApps(packages: Set<String>) {
        context.settingsDataStore.edit { it[Keys.EXCLUDED_APPS] = packages }
    }

    override suspend fun setPauseAccessibilityInExcludedApps(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.PAUSE_ACCESSIBILITY_EXCLUDED] = enabled }
    }

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        context.settingsDataStore.edit { it[Keys.ONBOARDING_COMPLETED] = completed }
    }

    override suspend fun setLastPlusKeySeen(epochMs: Long) {
        context.settingsDataStore.edit { it[Keys.LAST_PLUS_KEY_SEEN] = epochMs }
    }
}
