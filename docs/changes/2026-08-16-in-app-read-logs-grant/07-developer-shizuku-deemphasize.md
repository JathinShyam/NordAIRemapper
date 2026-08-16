# 07 — Developer / Shizuku de-emphasize

## Why

Shizuku/aShell is not the product path for normal users. Keep USB ADB as advanced fallback only.

## What

| File | Change |
|------|--------|
| `DeveloperScreen.kt` | Primary CTA: “Enable Plus Key detection”; USB command + copy + Recheck; removed Shizuku / copy pm grant as primary |
| `DeveloperViewModel.kt` | Removed `openShizuku`, `openWirelessDebugging`, `copyOnDeviceCommand` |
| `DetectionCoordinator.kt` / `ReadLogsGrantHelper` | Removed `openShizukuOrPlayStore`; kept `ON_DEVICE_SHELL_COMMAND`, `openWirelessDebugging`, `openDeveloperOptions` |
| `KeyLearningScreen.kt` | Hint copy points to Enable detection; button opens that screen |
| `LogcatWatcherService.kt` | KDoc: prefer in-app Wireless pair; USB as fallback |

## Verify

Developer without READ_LOGS shows Enable detection button; no “Open Shizuku” control.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Users still told to install Shizuku | Stale strings in another screen/docs — search `Shizuku` |
| USB grant works but watcher off | Call `verifyAndSyncWatcher` / Recheck / toggle service |
