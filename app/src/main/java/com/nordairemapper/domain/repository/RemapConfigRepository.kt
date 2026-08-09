package com.nordairemapper.domain.repository

import com.nordairemapper.domain.model.OverlayConfig
import com.nordairemapper.domain.model.PressType
import com.nordairemapper.domain.model.RemapAction
import kotlinx.coroutines.flow.Flow

interface RemapConfigRepository {
    /** All three press types, defaulting to [RemapAction.None] when unset. */
    fun observeConfigs(): Flow<Map<PressType, RemapAction>>

    suspend fun getAction(pressType: PressType): RemapAction

    suspend fun setAction(pressType: PressType, action: RemapAction)

    fun observeOverlayConfig(): Flow<OverlayConfig>

    suspend fun setOverlayConfig(config: OverlayConfig)
}
