---
name: device-testing
description: >-
  ADB, READ_LOGS, OxygenOS permission monitoring, USB debugging vs tethering,
  and capturing Plus Key logcat on a connected Nord 5. Use when granting
  permissions, installing builds, debugging detection on device, or when
  adb is missing from PATH.
---

# Device testing (Nord 5)

Primary device in this project: OnePlus Nord 5 (`CPH2707`, serial often `4d67c626`).

## Tools

`adb` is usually **not** on PATH:

```bash
ADB="$HOME/Android/Sdk/platform-tools/adb"
"$ADB" devices -l
```

JDK for Gradle: `export JAVA_HOME="$HOME/.jdks/jdk17"`.

## USB

- **USB tethering / RNDIS-only** hides the `adb` device. Use **File transfer (MTP)** + USB debugging.
- If `devices` is empty: unplug, disable tethering, re-enable USB debugging, replug.

## READ_LOGS (Strategy B)

**Preferred (phone only, no laptop / no Shizuku):** in the app open **Enable detection**
→ Wireless debugging → **Allow** the Wi‑Fi network if prompted (SSID/BSSID dialog) →
open the Wireless debugging **detail page** → **Pair device with pairing code** →
enter the 6-digit code (and port if asked) → Pair and grant. Then turn Wireless
debugging off; `READ_LOGS` persists.

If connect fails after a correct pairing code: you likely entered only the
**pairing** port. The Wireless debugging **main** page shows a different
**IP address & port** — enter that as Connection port in Enable detection.
See `docs/changes/2026-08-16-in-app-read-logs-grant/14-pairing-vs-connect-port.md`.

### Phone-only test plan

1. Fresh install, no USB cable, no Shizuku.
2. Enable Accessibility → **Enable detection** → pair → grant succeeds.
3. Key setup shows Plus Key via logcat; single / double / long fire.
4. Reboot with Wireless debugging off — remapping still works.
5. Advanced: USB ADB grant still works as fallback (below).

Cannot be granted from a normal system dialog. USB fallback:

```bash
"$ADB" shell pm grant com.nordairemapper android.permission.READ_LOGS
```

OxygenOS may fail with `GRANT_RUNTIME_PERMISSIONS`. On the phone: **disable permission monitoring** (USB debugging / security settings), then retry.

Verify (user 0 is the one that matters):

```bash
"$ADB" shell dumpsys package com.nordairemapper | grep -A2 READ_LOGS
```

`userId=10 granted=false` is another profile — ignore.

After grant the app auto-starts the logcat watcher when remapping is enabled. Accessibility is still required for system actions (screenshot, lock, recents, shade, QS).

## Capture Plus Key lines

```bash
"$ADB" logcat -d | grep -iE 'OplusKeyEventUtil|KEYLOG_OplusKeyEventUtil|undefined keys'
```

Live:

```bash
"$ADB" logcat -v brief | grep -iE 'OplusKeyEventUtil|KEYLOG|nordairemapper'
```

App tags: `LogcatWatcher`, `RemapEngine`.

Use captured lines to tune `logcatPattern` and `parseKeyAction`. Do not assume Tasker filters match ours.

## Permissions checklist

1. Accessibility service enabled  
2. `READ_LOGS` if using logcat  
3. Overlay permission if testing the floating menu  
4. Battery optimization exemption for overnight detection  
5. Notifications if the detection FGS is killed  

Stock Plus Key may still fire without root. Set it to a harmless system action while testing remaps.
