package com.nordairemapper.data.repository

import com.nordairemapper.data.local.ConfigSnapshotDao
import com.nordairemapper.data.local.ConfigSnapshotEntity
import com.nordairemapper.domain.model.BackupPayload
import com.nordairemapper.domain.model.BackupSettings
import com.nordairemapper.domain.model.ConfigSnapshot
import com.nordairemapper.domain.model.DetectionStrategy
import com.nordairemapper.domain.model.HapticIntensity
import com.nordairemapper.domain.model.PressType
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.domain.repository.BackupRepository
import com.nordairemapper.domain.repository.RemapConfigRepository
import com.nordairemapper.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.InputStream
import java.io.OutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepositoryImpl @Inject constructor(
    private val snapshotDao: ConfigSnapshotDao,
    private val remapConfigRepository: RemapConfigRepository,
    private val settingsRepository: SettingsRepository,
    private val json: Json,
) : BackupRepository {

    override fun observeSnapshots(): Flow<List<ConfigSnapshot>> =
        snapshotDao.observeAll().map { entities ->
            entities.map {
                ConfigSnapshot(
                    id = it.id,
                    name = it.name,
                    createdAtEpochMs = it.createdAtEpochMs,
                )
            }
        }

    override suspend fun createSnapshot(name: String): Long {
        val payload = buildPayload()
        return snapshotDao.insert(
            ConfigSnapshotEntity(
                name = name.ifBlank { "Snapshot" },
                createdAtEpochMs = System.currentTimeMillis(),
                payloadJson = json.encodeToString(BackupPayload.serializer(), payload),
            )
        )
    }

    override suspend fun deleteSnapshot(id: Long) {
        snapshotDao.get(id)?.let { snapshotDao.delete(it) }
    }

    override suspend fun restoreSnapshot(id: Long) {
        val entity = snapshotDao.get(id) ?: return
        val payload = json.decodeFromString(BackupPayload.serializer(), entity.payloadJson)
        applyPayload(payload)
    }

    override suspend fun exportTo(stream: OutputStream) {
        val text = json.encodeToString(BackupPayload.serializer(), buildPayload())
        stream.bufferedWriter().use { it.write(text) }
    }

    override suspend fun importFrom(stream: InputStream) {
        val text = stream.bufferedReader().use { it.readText() }
        val payload = json.decodeFromString(BackupPayload.serializer(), text)
        applyPayload(payload)
    }

    override suspend fun buildPayload(): BackupPayload {
        val actions = remapConfigRepository.observeConfigs().first()
        val overlay = remapConfigRepository.observeOverlayConfig().first()
        val settings = settingsRepository.settings.first()
        return BackupPayload(
            exportedAtEpochMs = System.currentTimeMillis(),
            remap = PressType.entries.associate { it.key to (actions[it] ?: RemapAction.None) },
            overlay = overlay,
            settings = BackupSettings(
                doublePressWindowMs = settings.doublePressWindowMs,
                longPressThresholdMs = settings.longPressThresholdMs,
                detectionStrategy = settings.detectionStrategy.key,
                keyIdentity = settings.keyIdentity,
                hapticFeedback = settings.hapticFeedback,
                hapticIntensity = settings.hapticIntensity.name,
                visualOverlayEnabled = settings.visualOverlayEnabled,
                lockScreenSingleEnabled = settings.lockScreenSingleEnabled,
                lockScreenDoubleEnabled = settings.lockScreenDoubleEnabled,
                lockScreenLongEnabled = settings.lockScreenLongEnabled,
                excludedApps = settings.excludedApps,
            ),
        )
    }

    override suspend fun applyPayload(payload: BackupPayload) {
        PressType.entries.forEach { pressType ->
            val action = payload.remap[pressType.key] ?: RemapAction.None
            remapConfigRepository.setAction(pressType, action)
        }
        remapConfigRepository.setOverlayConfig(payload.overlay)
        val s = payload.settings
        settingsRepository.setDoublePressWindowMs(s.doublePressWindowMs)
        settingsRepository.setLongPressThresholdMs(s.longPressThresholdMs)
        settingsRepository.setDetectionStrategy(DetectionStrategy.fromKey(s.detectionStrategy))
        settingsRepository.setKeyIdentity(s.keyIdentity)
        settingsRepository.setHapticFeedback(s.hapticFeedback)
        settingsRepository.setHapticIntensity(
            runCatching { HapticIntensity.valueOf(s.hapticIntensity) }.getOrDefault(HapticIntensity.MEDIUM),
        )
        settingsRepository.setVisualOverlayEnabled(s.visualOverlayEnabled)
        settingsRepository.setLockScreenSingleEnabled(s.lockScreenSingleEnabled)
        settingsRepository.setLockScreenDoubleEnabled(s.lockScreenDoubleEnabled)
        settingsRepository.setLockScreenLongEnabled(s.lockScreenLongEnabled)
        settingsRepository.setExcludedApps(s.excludedApps)
    }
}
