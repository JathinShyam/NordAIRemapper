package com.nordairemapper.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

/** One row per press type; the action is stored as polymorphic JSON. */
@Entity(tableName = "remap_configs")
data class RemapConfigEntity(
    @PrimaryKey val pressType: String,
    val actionJson: String,
)

/** Single-row table (id = 0) holding the overlay configuration as JSON. */
@Entity(tableName = "overlay_config")
data class OverlayConfigEntity(
    @PrimaryKey val id: Int = 0,
    val configJson: String,
)

/** Named backup snapshots of the full configuration. */
@Entity(tableName = "config_snapshots")
data class ConfigSnapshotEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val createdAtEpochMs: Long,
    val payloadJson: String,
)
