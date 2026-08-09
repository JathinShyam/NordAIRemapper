package com.nordairemapper.data.repository

import com.nordairemapper.data.local.OverlayConfigDao
import com.nordairemapper.data.local.OverlayConfigEntity
import com.nordairemapper.data.local.RemapConfigDao
import com.nordairemapper.data.local.RemapConfigEntity
import com.nordairemapper.domain.model.OverlayConfig
import com.nordairemapper.domain.model.PressType
import com.nordairemapper.domain.model.RemapAction
import com.nordairemapper.domain.repository.RemapConfigRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RemapConfigRepositoryImpl @Inject constructor(
    private val remapConfigDao: RemapConfigDao,
    private val overlayConfigDao: OverlayConfigDao,
    private val json: Json,
) : RemapConfigRepository {

    override fun observeConfigs(): Flow<Map<PressType, RemapAction>> =
        remapConfigDao.observeAll().map { entities ->
            val stored = entities.associate { entity ->
                PressType.fromKey(entity.pressType) to decodeAction(entity.actionJson)
            }
            PressType.entries.associateWith { stored[it] ?: RemapAction.None }
        }

    override suspend fun getAction(pressType: PressType): RemapAction =
        remapConfigDao.get(pressType.key)?.let { decodeAction(it.actionJson) } ?: RemapAction.None

    override suspend fun setAction(pressType: PressType, action: RemapAction) {
        remapConfigDao.upsert(
            RemapConfigEntity(
                pressType = pressType.key,
                actionJson = json.encodeToString(RemapAction.serializer(), action),
            )
        )
    }

    override fun observeOverlayConfig(): Flow<OverlayConfig> =
        overlayConfigDao.observe().map { entity ->
            entity?.let {
                runCatching { json.decodeFromString(OverlayConfig.serializer(), it.configJson) }
                    .getOrDefault(OverlayConfig())
            } ?: OverlayConfig()
        }

    override suspend fun setOverlayConfig(config: OverlayConfig) {
        overlayConfigDao.upsert(
            OverlayConfigEntity(configJson = json.encodeToString(OverlayConfig.serializer(), config))
        )
    }

    private fun decodeAction(actionJson: String): RemapAction =
        runCatching { json.decodeFromString(RemapAction.serializer(), actionJson) }
            .getOrDefault(RemapAction.None)
}
