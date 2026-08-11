package com.nordairemapper.domain.repository

import com.nordairemapper.domain.model.BackupPayload
import com.nordairemapper.domain.model.ConfigSnapshot
import kotlinx.coroutines.flow.Flow
import java.io.InputStream
import java.io.OutputStream

interface BackupRepository {
    fun observeSnapshots(): Flow<List<ConfigSnapshot>>
    suspend fun createSnapshot(name: String): Long
    suspend fun deleteSnapshot(id: Long)
    suspend fun restoreSnapshot(id: Long)
    suspend fun exportTo(stream: OutputStream)
    suspend fun importFrom(stream: InputStream)
    suspend fun buildPayload(): BackupPayload
    suspend fun applyPayload(payload: BackupPayload)
}
