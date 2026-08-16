# 04 — ReadLogsGrantViaWirelessAdb

## Why

Centralize the elevated grant so the UI never exposes arbitrary shell — only `pm grant … READ_LOGS`.

## What

**New file:** `app/src/main/java/com/nordairemapper/service/adb/ReadLogsGrantViaWirelessAdb.kt`

`@Singleton` Hilt injectable.

### API

| Method | Behavior |
|--------|----------|
| `hasReadLogs()` | Delegates to `LogcatWatcherService.hasReadLogsPermission` |
| `discoverPairingEndpoint(timeoutMs)` | `AdbMdns` + `SERVICE_TYPE_TLS_PAIRING`; resumes on first local host/port |
| `pairAndGrant(code, host?, pairingPort?)` | Validate 6-digit code → pair → `connectTls` / `autoConnect` → `shell:pm grant …` → verify → `syncWatcherAfterGrant` → disconnect |
| `verifyAndSyncWatcher()` | Recheck after USB grant / resume |

### Shell surface (hard constraint)

Only:

```text
pm grant com.nordairemapper android.permission.READ_LOGS
```

(`ReadLogsGrantHelper.ON_DEVICE_SHELL_COMMAND`)

### GrantResult

- `AlreadyGranted` — still syncs watcher
- `Success`
- `Failed(message)` — user-facing string

### Constants

- Pairing discovery timeout: 12s
- Connect timeout: 10s
- Default host if port known but host missing: `127.0.0.1`

## Verify

On device: pair with code → Home remaps work without opening Developer. `dumpsys package … \| grep READ_LOGS` shows granted for user 0.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| “Could not find the pairing port” | Dialog not open; enter port manually from IP:port line |
| “Pairing failed” | Wrong/expired code; dialog closed mid-pair |
| “Paired, but could not connect” | Wireless debugging toggled off; wait and retry `connectTls` |
| Grant command ran but permission missing | Permission monitoring / work profile; try USB ADB; check user 0 vs 10 |
| Double resume / crash on discover | Ensure `cont.isActive` + single `resume`; stop mDNS after success |
