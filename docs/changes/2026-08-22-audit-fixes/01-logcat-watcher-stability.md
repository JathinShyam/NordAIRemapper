# 01 — Logcat watcher: single tail, hot-reload, reconnect

## Why
`onStartCommand` had no "already watching" guard and `RemapEngine`'s settings
collector called `syncLogcatWatcher` on **every** DataStore write, so any toggle
spawned another concurrent `logcat -T 1` process (sliders wrote per drag tick).
Old processes were never destroyed until service death, which killed only the
last handle. Separately: pattern edits needed a restart, and a dead logcat
stream exited silently while the FGS notification still said detection was on.

## What
| File | Change |
|------|--------|
| `service/LogcatWatcherService.kt` | Single `watchJob` guard; `watchLogcat()` now observes the pattern (`collectLatest`) so Lab edits hot-reload; EOF/error retries with exponential backoff 2s→30s; posts the death notification once per outage, re-armed after a stable (>60s) run; process destroyed in `finally` |
| `service/RemapEngine.kt` | Collector syncs watcher only when `detectionStrategy`/`serviceEnabled` actually change |
| `presentation/developer/DeveloperScreen.kt`, `presentation/settings/VisualOverlayScreen.kt` | Timing + hold-duration sliders keep local state and commit in `onValueChangeFinished` |

## Verify
1. Enable detection, flip theme/haptics repeatedly → one app logcat process
   (`adb shell ps -A | grep com.nordairemapper`). Before: N processes.
2. Drag Double-press window slider → single DataStore write per gesture.
3. Edit match pattern in Lab → next press uses it without restarting anything.
4. `adb shell killall logd`-style disruption (or reboot logd) → notification
   appears once, watcher reconnects within seconds.

## Debug tips
| Symptom | Likely cause |
|---|---|
| Gestures fire twice / battery drain grows over a session | Pre-fix build leaking tails; check process count |
| Pattern edit ignored | Old build reads pattern once at start; update |
