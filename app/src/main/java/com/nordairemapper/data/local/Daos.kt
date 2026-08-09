package com.nordairemapper.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RemapConfigDao {
    @Query("SELECT * FROM remap_configs")
    fun observeAll(): Flow<List<RemapConfigEntity>>

    @Query("SELECT * FROM remap_configs WHERE pressType = :pressType")
    suspend fun get(pressType: String): RemapConfigEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: RemapConfigEntity)

    @Query("DELETE FROM remap_configs")
    suspend fun clear()
}

@Dao
interface OverlayConfigDao {
    @Query("SELECT * FROM overlay_config WHERE id = 0")
    fun observe(): Flow<OverlayConfigEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(entity: OverlayConfigEntity)
}

@Dao
interface ConfigSnapshotDao {
    @Query("SELECT * FROM config_snapshots ORDER BY createdAtEpochMs DESC")
    fun observeAll(): Flow<List<ConfigSnapshotEntity>>

    @Query("SELECT * FROM config_snapshots WHERE id = :id")
    suspend fun get(id: Long): ConfigSnapshotEntity?

    @Insert
    suspend fun insert(entity: ConfigSnapshotEntity): Long

    @Delete
    suspend fun delete(entity: ConfigSnapshotEntity)
}
