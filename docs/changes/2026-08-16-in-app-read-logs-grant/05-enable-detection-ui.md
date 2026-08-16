# 05 — Enable detection UI

## Why

Consumer path must not be buried only under Developer. Users need a guided Wireless debugging pair flow.

## What

**New files:**

- `presentation/detection/EnableDetectionScreen.kt`
- `presentation/detection/EnableDetectionViewModel.kt`

### UI state (`EnableDetectionUiState`)

`readLogsGranted`, `pairingCode`, `manualPort`, `discoveredHost/Port`, `isDiscovering`, `isGranting`, status/error messages, `showAdvanced`.

### ViewModel API (`EnableDetectionViewModel`)

| Method | Behavior |
|--------|----------|
| `refresh()` | `verifyAndSyncWatcher()`; update granted/status |
| `onPairingCodeChange` | Digits only, max 6 |
| `onManualPortChange` | Digits only, max 5 |
| `setShowAdvanced` | Toggle USB fallback section |
| `openWirelessDebugging` | Settings intent + `startDiscovery()` |
| `startDiscovery` | mDNS pairing endpoint → host/port in state |
| `pairAndGrant` | Calls grant API with code + manual/discovered port |
| `copyUsbAdbCommand` | Clipboard `LogcatWatcherService.ADB_GRANT_COMMAND` |

Hilt: `@HiltViewModel`, injects `@ApplicationContext` + `ReadLogsGrantViaWirelessAdb`.

### Route

`Routes.ENABLE_DETECTION = "enable_detection"` in `NordNavHost`.

## Verify

Navigate from Home banner “Enable detection”; complete pair; status shows granted; Done/Continue works.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Button stuck on “Granting…” | Exception not mapped to `Failed`; check logcat tag `ReadLogsWirelessAdb` |
| Code field accepts non-digits | Should filter to 6 digits in ViewModel |
| Resume doesn’t refresh grant | `ON_RESUME` → `refresh()` / `verifyAndSyncWatcher` |
