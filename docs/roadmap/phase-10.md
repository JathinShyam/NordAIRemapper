# Phase 10 — Backup & Restore (SAF)

**Status:** ✅ Done  
**Depends on:** Phase 2 (Room); better after 7 & 9 so payload is meaningful  
**Gate:** `./gradlew assembleDebug`  
**Commit:** `Phase 10: Backup & Restore — SAF export/import + local snapshots`

---

## 1. Goal

Export full config as versioned JSON, import via document picker, and manage named local snapshots (Room `config_snapshots`).

---

## 2. Payload schema

```json
{
  "schemaVersion": 1,
  "exportedAtEpochMs": 0,
  "remap": {
    "single": { /* RemapAction JSON */ },
    "double": { },
    "long": { }
  },
  "overlay": { /* OverlayConfig */ },
  "settings": {
    "doublePressWindowMs": 300,
    "longPressThresholdMs": 500,
    "detectionStrategy": "accessibility",
    "keyIdentity": { "keyCode": 0, "scanCode": 250 },
    "hapticFeedback": true,
    "excludedApps": []
  }
}
```

Omit secrets; include key identity so restore works on same device.

Use `kotlinx.serialization` DTO `BackupPayload`.

---

## 3. UI

| Feature | Spec |
|---------|------|
| Export | `CreateDocument("application/json")` |
| Import | `OpenDocument` → parse → confirm replace → apply |
| Snapshots | Name field + Save; list with timestamp; swipe delete; tap restore + confirm sheet |
| Errors | Snackbar on parse failure |

---

## 4. Files

| Path | Action |
|------|--------|
| `domain/model/BackupPayload.kt` | Create |
| `domain/repository/BackupRepository.kt` | Create |
| `data/repository/BackupRepositoryImpl.kt` | Create |
| `presentation/backup/BackupScreen.kt` | Create |
| `presentation/backup/BackupViewModel.kt` | Create |
| Nav + Settings entry | Wire |

---

## 5. Acceptance

- [ ] Export file opens and round-trips  
- [ ] Import restores Home card actions  
- [ ] Snapshot save/restore/delete works  
- [ ] No storage permissions added  
- [ ] Build green  

## 6. Out of scope

Cloud sync.
