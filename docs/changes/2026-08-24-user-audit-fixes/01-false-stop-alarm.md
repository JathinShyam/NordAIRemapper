# 01 — No false "detection stopped" alarm on deliberate stop

## Why

`LogcatWatcherService.onDestroy` posted the outage notification ("Key detection
stopped — tap to re-enable") on every destroy except planned `restart()`. When
the user turned the master toggle off, `DetectionCoordinator.syncLogcatWatcher`
stopped the service → the user got a scary alarm for an action they took. This
erodes trust in every future alert (cry-wolf).

## What

| File | Change |
|------|--------|
| `service/LogcatWatcherService.kt` | Companion `sSuppressDeathNotify` + `suppressDeathNotification()`; `onStartCommand` re-arms it to false; `onDestroy` suppresses only when flagged |
| `service/DetectionCoordinator.kt` | Sets suppression before `stop(context)` **only when `serviceEnabled == false`** (deliberate stop). A stop caused by a lost READ_LOGS grant while enabled still alarms |

Behavior matrix after fix:

| Stop cause | Alarm? |
|---|---|
| User master toggle off | no |
| Planned `restart()` (consent heal) | no |
| System FGS kill while enabled | yes |
| logd tail death / crash loop | yes |
| READ_LOGS lost while enabled | yes |
| BootReceiver path (enabled) | start, not stop |

## Verify

1. Home → toggle Remapping off → no "Key detection stopped" notification.
2. Toggle back on → watcher FGS returns; `adb shell dumpsys activity services LogcatWatcher`.
3. `adb shell pm revoke` is not possible for READ_LOGS; simulate grant loss by
   reinstalling — detection-stopped/blind notifications must still appear.

## Debug tips

| Symptom | Likely cause |
|---------|--------------|
| No alarm when detection actually died | something called `suppressDeathNotification()` without a following real start (check `syncLogcatWatcher` callers) |
| Alarm still fires on master-off | stop came from a caller that bypasses `syncLogcatWatcher` |
