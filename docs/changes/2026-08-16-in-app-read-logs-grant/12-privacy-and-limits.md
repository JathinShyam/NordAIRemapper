# 12 — Privacy, shell surface, known limits

## Why

Honest product constraints and a locked-down shell surface matter for trust and for debugging “why won’t Accessibility alone work?”

## What we claim

- Grant is **one-time**; Wireless debugging can be turned **off** afterward; `READ_LOGS` persists across reboots.
- App runs **only** `pm grant com.nordairemapper android.permission.READ_LOGS` over the ADB shell stream — no general shell UI.
- ADB key/cert stay in **app private storage** (`filesDir/adb/`).
- mDNS is used only to find the pairing port on the local device.

## What we do **not** claim

- Accessibility alone can detect the Nord 5 Plus Key on stock OxygenOS.
- Setup needs zero Developer options (Wireless debugging lives there).
- Stock Plus Key settings can open this app.
- We suppress the stock Plus Key action without root.

## Log tags

| Tag | Source |
|-----|--------|
| `ReadLogsWirelessAdb` | `ReadLogsGrantViaWirelessAdb` |
| `LogcatWatcher` | existing watcher |
| `RemapEngine` | existing engine |

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| User expects zero Developer options | Android platform limit — explain Wireless debugging step |
| Stock AI action still fires | Expected without root — set stock action to something harmless |
| Fear of “full ADB shell” | Point at single `pm grant` constant; no command field in UI |
