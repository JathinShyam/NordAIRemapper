# Phase 2 — Domain models, Room, DataStore, repositories

**Status:** ✅ Done  
**Gate:** `./gradlew assembleDebug`

## Goal

Define the domain models and local persistence that all later phases read/write.

## Deliverables

| Area | Files / contracts |
|------|-------------------|
| Models | `RemapAction` (sealed + serializable), `PressType`, `KeyIdentity`, `DetectionStrategy`, `OverlayConfig`, `AppSettings` |
| Room | `remap_configs`, `overlay_config`, `config_snapshots` entities + DAOs + `NordDatabase` |
| DataStore | All preference keys for settings |
| Repos | `RemapConfigRepository`, `SettingsRepository` + Hilt-bound impls |
| DI | `AppModule` (Json, DB, DAOs), `RepositoryModule` |

## Acceptance

- [x] Polymorphic encode/decode path for `RemapAction` compiles
- [x] Settings Flow emits defaults
- [x] Observe configs returns all three press types (default `None`)

## Out of scope

UI, services, executors.
