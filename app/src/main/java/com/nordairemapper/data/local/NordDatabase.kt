package com.nordairemapper.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        RemapConfigEntity::class,
        OverlayConfigEntity::class,
        ConfigSnapshotEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class NordDatabase : RoomDatabase() {
    abstract fun remapConfigDao(): RemapConfigDao
    abstract fun overlayConfigDao(): OverlayConfigDao
    abstract fun configSnapshotDao(): ConfigSnapshotDao
}
