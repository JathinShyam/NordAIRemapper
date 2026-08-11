package com.nordairemapper.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BackupPayload(
    val schemaVersion: Int = 1,
    val exportedAtEpochMs: Long,
    val remap: Map<String, RemapAction>,
    val overlay: OverlayConfig,
    val settings: BackupSettings,
)

@Serializable
data class BackupSettings(
    val doublePressWindowMs: Long = 300L,
    val longPressThresholdMs: Long = 500L,
    val detectionStrategy: String = DetectionStrategy.ACCESSIBILITY.key,
    val keyIdentity: KeyIdentity = KeyIdentity.UNCONFIGURED,
    val hapticFeedback: Boolean = true,
    val excludedApps: Set<String> = emptySet(),
)

data class ConfigSnapshot(
    val id: Long,
    val name: String,
    val createdAtEpochMs: Long,
)
