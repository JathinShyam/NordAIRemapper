# Hands-free banking Accessibility auto-resume

## Why
Users pay with BHIM/UPI daily. Requiring a trip to Accessibility settings after every payment is the wrong product.

## What
| File | Change |
|------|--------|
| `ElevatedPermissions.kt` | `WRITE_SECURE_SETTINGS` + usage access checks; Unlock shell list |
| `AccessibilitySecureToggle.kt` | Soft enable/disable via Secure Settings |
| `AccessibilityAutoResumeService.kt` | FGS polls UsageStats; restores Accessibility after leave |
| `PlusKeyAccessibilityService.kt` | Hands-free path vs `disableSelf` fallback |
| `ReadLogsGrantViaWirelessAdb.kt` | Runs all Unlock commands; re-runs if banking grants missing |
| `LogcatWatcherService.ADB_GRANT_COMMAND` | Multi-line USB paste (all Unlock grants) |
| `ExclusionsScreen.kt` | Auto-Pause + Hands-free / Unlock CTA |
| `EnableDetectionScreen/ViewModel` | Banking chip + re-Unlock when grants incomplete |
| `AndroidManifest.xml` | Permissions + AutoResume FGS |
| `DetectionCoordinator` / `ReadLogsGrantHelper` | Shell command list → ElevatedPermissions |

## Verify
See `00-overview.md`.

## Debug tips
| Symptom | Likely cause |
|---------|----------------|
| AutoResume never starts | `canAutoResumeAccessibility` false |
| Soft-disable no effect | OEM blocks Secure Settings writes |
| Resume too early (mid-payment) | UsageStats flicker; raise `RESUME_STABLE_SAMPLES` / grace |
| Resume never | Usage access revoked; FGS killed by OEM battery |
