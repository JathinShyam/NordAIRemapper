# READ_LOGS granted but logd blind — Plus Key detection silent death

## Why
2026-08-23: after reinstalling the CI debug APK (13:08) and re-running Unlock, the
user reported zero Plus Key detection. Everything *looked* healthy on device:
accessibility bound, `READ_LOGS: granted=true` (user 0), watcher FGS foreground,
fresh `logcat` child owned by the live app pid, KEYLOG lines flowing in main
buffer, no crashes, no SELinux denials. Yet DataStore never recorded
`last_plus_key_seen_ms` — not a single gesture classified.

## Root cause (proven by probe)
logd serves the app uid **only its own logs**. Measured with
`run-as com.nordairemapper logcat -d -b main -v brief` (same uid as the spawned
watcher child):

| view | system_server (pid 4261) lines | KEYLOG lines |
|------|-------------------------------|--------------|
| app uid (run-as) | **0** | **0** |
| shell (adb), same moment | 338 | present |

So the watcher's tail connects, reads forever, and never sees a single
system_server `KEYCODE_ACTION_BUTTON_CLICK` line. Pattern matching, coalescer,
engine gating (`AUTO` accepts LOGCAT) are all fine — input never arrives.
A fresh process + fresh logcat connection did **not** clear it, so it is active
enforcement at logd (per-boot cache or OxygenOS security layer), not staleness.
Trigger correlates with today's uninstall/reinstall + re-grant.

## What (code fix)
Detection must never die silently again.

| File | Change |
|------|--------|
| `service/LogcatWatcherService.kt` | Blindness watchdog: every streamed line's origin pid is parsed (`( 1234)` in brief format); `lastNonSelfLineAtMs` tracks the last line from any other pid. A watchdog coroutine posts an actionable alert when no non-self line arrives for 3 min **while the screen is interactive**, and auto-clears when logs flow again. Healthy devices always see system noise within minutes; only logd enforcement failure leaves the stream all-self. Tail start now logs `pattern`/`selfPid`. |
| `service/ServiceNotifications.kt` | `notifyLogsBlind` / `clearLogsBlind`: "Plus Key detection can't see key presses — reboot the phone, then run Unlock again." |

## User remediation ladder (for this device)
1. **Reboot the phone** — logd permission caches can be boot-scoped; re-granted READ_LOGS then takes effect.
2. If still blind: Developer options → enable **USB debugging (Security settings)** (OxygenOS gates debug grants behind it), reboot, re-run Unlock.
3. Re-run Unlock from Enable Detection either way; the new watchdog confirms recovery automatically (alert clears).

## Verify
1. Build green, unit tests green.
2. On healthy state: press key → `LogcatWatcher: edge=DOWN/UP` lines appear; no blind alert.
3. Simulate blindness is hard without root — trust the probe method above (`run-as … grep -c '( *4261)'`) when diagnosing future reports.

## Debug tips
| Symptom | Likely cause |
|---------|----------------|
| Blind alert fires repeatedly | logd still filtering → finish remediation ladder; check `run-as com.nordairemapper logcat -d -b main \| grep -c 4261` |
| No alert but no detection | Watcher dead or pattern mismatch — check `LogcatWatcher` logs and stored `logcat_pattern` |
| Works after reboot, dies after update | Boot-scoped logd cache; re-run Unlock after updates until OEM fixes enforcement |
