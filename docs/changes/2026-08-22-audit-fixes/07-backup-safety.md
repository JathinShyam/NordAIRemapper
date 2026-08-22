# 07 — Backup: confirmations, schema guard, snapshot cap

## Why
Import applied instantly and overwrote the whole setup while snapshot restore
confirmed — inconsistent destructive safety. Snapshot delete was unconfirmed.
Import never checked `schemaVersion`, so newer-version files would partially
apply. Snapshots grew unbounded.

## What
| File | Change |
|------|--------|
| `presentation/backup/BackupScreen.kt` | Import picker result now stages a confirm dialog ("replaces your current remaps, overlay, and related settings"); snapshot delete confirms too |
| `data/repository/BackupRepositoryImpl.kt` | `applyPayload` rejects `schemaVersion > 1` with a clear message (surfaced via existing failure snackbar); snapshots prune to newest 20 after insert |
| `data/local/Daos.kt` | `ConfigSnapshotDao.pruneOldest(limit)` |

## Verify
1. Import → dialog → Cancel changes nothing; Confirm applies + "Imported".
2. Import a file with `"schemaVersion": 99` → "Export failed/Import failed:"
   snackbar mentions newer app version.
3. Save 21+ snapshots → list caps at 20 newest.

## Debug tips
| Symptom | Likely cause |
|---|---|
| "Import failed: Backup was exported by a newer…" | Expected guard, not corruption |
