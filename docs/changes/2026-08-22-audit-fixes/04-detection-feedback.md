# 04 — Detection feedback: pulse filter; system-key learn guard

## Why
Home's silhouette flashed on ANY DOWN/PULSE — pressing volume looked like Plus
Key detection working. Key setup let users save Volume-Up/Power as the Plus
Key, after which remapping consumed hardware keys people need.

## What
| File | Change |
|------|--------|
| `presentation/home/HomeViewModel.kt` | Pulse emits only for logcat source (pattern-pre-matched) or learned-identity match, mirroring `RemapEngine.isPlusKeyEvent` |
| `presentation/developer/KeyLearningViewModel.kt` | `CapturedPress.isSystemKey` (VOLUME_UP/DOWN, POWER) |
| `presentation/developer/KeyLearningScreen.kt` | Save button disabled for system keys, labeled "System key" |

## Verify
1. On Home press volume → silhouette does NOT flash; Plus Key press → flashes.
2. Key setup volume row → button disabled.

## Debug tips
| Symptom | Likely cause |
|---|---|
| Silhouette still flashing on volume | Pre-fix build or identity saved as a volume key earlier — clear events and re-learn from logcat row |
