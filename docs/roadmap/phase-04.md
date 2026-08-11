# Phase 4 — Logcat watcher + Developer settings

**Status:** ✅ Done  
**Gate:** `./gradlew assembleDebug`

## Goal

Add Strategy B for OnePlus devices where Accessibility never sees the Plus Key.

## Deliverables

| Component | Behavior |
|-----------|----------|
| `LogcatWatcherService` | FGS `specialUse`; `logcat -T 1`; match pattern; emit DOWN/UP/PULSE |
| Permissions | `READ_LOGS` (ADB), FGS special use property |
| Developer screen | Strategy chips, ADB copy, pattern editor, timing sliders, link to key learning |
| Home | Entry button to Developer |

## Acceptance

- [x] Switching to LOGCAT starts/stops watcher when `READ_LOGS` granted
- [x] Missing permission shows copyable ADB command
- [x] Pattern and timings persist

## Manual device test

```bash
adb shell pm grant com.nordairemapper android.permission.READ_LOGS
```

Enable Logcat strategy → press Plus Key → events appear in Key setup.

## Out of scope

Full Settings screen; overlay.
