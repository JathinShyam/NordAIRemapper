# 02 — Boot receiver hardening; guarded FGS starts

## Why
Receiver was `directBootAware` and filtered `LOCKED_BOOT_COMPLETED`, reading
DataStore from credential-encrypted storage before unlock inside an unguarded
coroutine → potential boot crash. Also, background `startForegroundService`
(Android 12+) throws; sync paths run from collectors/service callbacks.

## What
| File | Change |
|------|--------|
| `AndroidManifest.xml` | Dropped `LOCKED_BOOT_COMPLETED` + `directBootAware`; `BOOT_COMPLETED` suffices since the FGS needs unlocked storage anyway |
| `service/BootReceiver.kt` | Body wrapped in `runCatching { … }.onFailure(Log.w)`; `pending.finish()` still guaranteed |
| `service/DetectionCoordinator.kt` | `syncLogcatWatcher` start/stop wrapped in `runCatching`, failures logged not thrown |

## Verify
1. Reboot with detection enabled → watcher re-arms after unlock, no crash.
2. No `ForegroundServiceStartNotAllowedException` in logs when toggling
   settings quickly from recents.

## Debug tips
| Symptom | Likely cause |
|---|---|
| App missing after reboot / crash loop at boot | Old build's direct-boot DataStore read |
