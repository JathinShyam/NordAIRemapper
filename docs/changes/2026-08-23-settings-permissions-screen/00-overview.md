# Settings Permissions screen

## Why
Users need one place to see which grants Keyforge relies on and whether each is satisfied, without digging through onboarding or Lab.

## What
| File | Change |
|------|--------|
| `AppPermissions.kt` | Snapshot of accessibility, READ_LOGS, overlay, notifications, battery, banking grants |
| `PermissionsViewModel.kt` | Refresh on resume; optional log visibility probe when READ_LOGS granted |
| `PermissionsScreen.kt` | Grouped list with status chips; tap opens matching settings / Unlock flow |
| `SettingsUi.kt` | `PermissionStatusRow` |
| `SettingsScreen.kt` | Permissions hub row with summary chip |
| `SettingsViewModel.kt` | Fast hub summary on resume |
| `NordNavHost.kt` | `permissions` route |

## Verify
1. Settings → **Permissions** — hub chip shows **All OK** or **N Need Attention**.
2. Subpage lists Core / Overlays / Reliability / Advanced sections with green or amber chips.
3. With READ_LOGS granted, **Device Log Visibility** appears after a short probe (Visible or Blind).
4. Tap rows: Accessibility → system Accessibility; READ_LOGS → Enable Detection; overlay → Display over apps; etc.
5. Return from system settings — statuses refresh on resume.

```bash
export JAVA_HOME="$HOME/.jdks/jdk17"
./gradlew :app:assembleDebug :app:testDebugUnitTest --tests com.nordairemapper.service.AppPermissionsTest
```

## Debug tips
| Symptom | Likely cause |
|---------|----------------|
| READ_LOGS granted but Blind | OxygenOS per-boot log consent — re-run Unlock or allow USB debugging security |
| Hub All OK but detection dead | Log visibility blind; open Permissions subpage for the probe result |
| Advanced grants missing | Normal until Wireless/USB Unlock completes banking auto-pause grants |
