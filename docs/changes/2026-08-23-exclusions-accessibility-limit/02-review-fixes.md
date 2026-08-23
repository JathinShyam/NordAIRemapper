# 02 — Review fixes: robust toggle, crash guard, watch cap

## Why
Pre-ship review of the hands-free Auto-Pause feature (research + code audit) found four defects that would surface as "bank still blocks me" or a crash during daily UPI use.

Research confirmed the core approach is sound: BHIM/UPI apps detect accessibility via
`AccessibilityManager.getEnabledAccessibilityServiceList()` (live list driven by
`Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES`). Soft-removing Keyforge from that setting unbinds the service and defeats the check. `WRITE_SECURE_SETTINGS` via `pm grant` is the established technique (Key Mapper, DarQ, etc.).

## What
| File | Change |
|------|--------|
| `service/AccessibilityAutoResumeService.kt` | **Exclusion-aware handoff:** injects `SettingsRepository`, keeps `excludedApps` live; when the user leaves the watched app straight into *another excluded app*, it re-targets the watch (notification title updates) instead of restoring — Accessibility stays off across the whole chain and only resumes when the foreground lands outside every excluded app. Resume decision additionally requires two consecutive identical samples (`prevForeground`) to avoid flapping on fast transitions. |
| `service/AccessibilitySecureToggle.kt` | Compare entries via `ComponentName.unflattenFromString` instead of exact string equality. Settings UI / adb / other tools may store the **short form** (`pkg/.Cls`); exact matching made removal silently fail → bank still blocked, and re-add could create duplicate entries. Add now dedupes all variants of our component and writes canonical `flattenToString()`. |
| `service/AccessibilityUtils.kt` | Same parse-based comparison for `isServiceEnabled`, so Home banner / BootReceiver agree with the toggle on short-form entries. |
| `service/AccessibilityAutoResumeService.kt` | 1) `queryEvents` wrapped in `runCatching` — usage access revoked mid-watch threw `SecurityException` inside the coroutine (app crash). Now treated as unknown sample. 2) `MAX_WATCH_MS = 6h` cap: if "left the banking app" is never detected (screen off in app all day, stats unavailable), post the manual re-enable notification and stop instead of polling forever. |
| `presentation/settings/ExclusionsScreen.kt` | Drop unused `NordGhostButton` import. |
| `service/DetectionCoordinator.kt` | Remove stale single-command `ON_DEVICE_SHELL_COMMAND` (superseded by `ON_DEVICE_SHELL_COMMANDS`). |

## Known limitations (accepted, document to user)
- **First-open race:** BHIM's security scan can run in the ~100–300 ms before our soft-disable propagates. The warning may appear once right after enabling Auto-Pause; reopening BHIM then works. Cannot be eliminated without pre-launch detection.
- **Screen-off resume delay:** if the user locks the phone while still in an excluded app, resume happens after unlock when the launcher registers.
- **Split-screen with one excluded app:** window-state flapping between two visible apps can pause/resume repeatedly; rare and self-correcting.

## Verify
1. `./gradlew :app:assembleDebug` green.
2. With Unlock granted: enable Auto-Pause → open BHIM → Keyforge gone from live accessibility list (`adb shell settings get secure enabled_accessibility_services`) → leave → entry restored exactly once (no duplicates across cycles).
3. **Chain:** exclude two apps (e.g. BHIM + PhonePe) → open first → switch directly to second → logcat `A11yAutoResume: Handoff …` and NO restore between them; leaving both to launcher restores once.
4. Revoke usage access while watcher active → no crash; timeout eventually posts manual notification.
5. Toggle Auto-Pause off/on repeatedly with an excluded app open → no duplicate entries in the secure setting.

## Debug tips
| Symptom | Likely cause |
|---------|----------------|
| Bank blocks even though setting shows us removed | OEM ignores Secure write — verify with `settings get secure enabled_accessibility_services`; check `A11ySecureToggle` logs |
| Warning appears once on first open after enabling | Known first-open race; reopen once |
| Handoff never triggers, restores between excluded apps | Exclusion list not loaded yet / package mismatch — check exact package name in exclusions; check `A11yAutoResume` logs |
| Watcher stops but no notification | FGS killed by battery optimizer — whitelist Keyforge |
