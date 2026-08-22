# 05 — Service hardening: exclusion filter, media, glow, backup rules

## Why
- Shade/QS window-state events flipped the foreground tracker away from the
  real app (exclusions misfired when notifications were pulled).
- Media keys dispatched to ALL active sessions → one skip per extra session.
- Overlay scrim glow center hard-coded `0.3*1080 × 0.2*2400 px`.
- `allowBackup` had no `dataExtractionRules` (API 31+), so backup scope was implicit.
- Settings "Version" fallback claimed a fake `0.1.0`.

## What
| File | Change |
|------|--------|
| `service/ForegroundAppTracker.kt`, `PlusKeyAccessibilityService.kt` | Tracker setter now filters `com.android.systemui` / `android`; service calls `onWindowStateChanged(pkg)` |
| `service/RemapActionExecutor.kt` | Media key goes to most recent active session, AudioManager fallback unchanged |
| `service/FloatingOverlayService.kt` | Glow drawn in `drawBehind` from real canvas size (center at 30%/20%, radius 60% of min dimension) |
| `res/xml/data_extraction_rules.xml`, manifest | Explicit rules: only Room DB + DataStore transfer via cloud backup/device migration |
| `presentation/settings/SettingsViewModel.kt` | Version fallback `"unknown"` |

## Verify
1. Exclusion: enable shade over an excluded app → gesture still skipped.
2. Two media apps with sessions; Next Track advances once.
3. Overlay on any resolution: glow hugs top-left region as designed.

## Debug tips
| Symptom | Likely cause |
|---|---|
| Exclusion not applying inside an app | Tracker updates only on `TYPE_WINDOW_STATE_CHANGED`; first launch before any window event may be untracked — known approximation |
