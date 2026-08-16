# 08 — Detection sync + device-testing docs

## Why

After grant, remaps must work from Home without a trip to Developer. Agents need a phone-only test checklist.

## What

### Auto-start watcher

`ReadLogsGrantViaWirelessAdb.syncWatcherAfterGrant()` reads current settings via `settingsRepository.settings.first()` and calls:

```kotlin
DetectionCoordinator.syncLogcatWatcher(context, strategy, serviceEnabled)
```

Triggered on:

- Successful grant
- Already granted
- `verifyAndSyncWatcher()` (Enable detection Recheck / ON_RESUME when granted)

`DetectionCoordinator.needsLogcatWatcher` already true for AUTO / ACCESSIBILITY / LOGCAT (companion on Nord 5).

### Skill update

`.cursor/skills/device-testing/SKILL.md`:

- Preferred phone-only Enable detection flow
- Phone-only test plan (fresh install → pair → logcat → reboot → USB fallback)
- USB `pm grant` remains documented as fallback

## Verify

1. Fresh install, no USB, no Shizuku  
2. Accessibility → Enable detection → pair → grant  
3. Key setup shows Plus Key logcat rows; single/double/long fire  
4. Reboot, Wireless debugging off — remaps still work  
5. Advanced USB grant still works  

```bash
"$HOME/Android/Sdk/platform-tools/adb" shell dumpsys package com.nordairemapper | grep -A2 READ_LOGS
```

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Grant OK but no logcat events | Watcher not started (`serviceEnabled` false); pattern mismatch; permission monitoring |
| Works until reboot | Unlikely for READ_LOGS; more often FGS killed — battery exemption / notification |
| USB grant OK, in-app pair fails | Network/mDNS/TLS; use Advanced USB while debugging pair |
