# Exclusions vs banking Accessibility blocks

## Why
Per-app exclusions only skip remap dispatch in `RemapEngine`. They do **not** remove Keyforge from `ENABLED_ACCESSIBILITY_SERVICES`. Banking/UPI apps (BHIM, etc.) check that system list and refuse to run while any service is listed.

Android will not let a normal app re-enable Accessibility after `disableSelf()` without the user. Daily “turn Keyforge back on in Settings” is unacceptable for UPI.

## What (hands-free path)
One-time **Wireless Unlock** (same flow as READ_LOGS) also grants:

1. `WRITE_SECURE_SETTINGS` — soft-remove / soft-add Keyforge in `ENABLED_ACCESSIBILITY_SERVICES`
2. Usage access (`GET_USAGE_STATS`) — detect when the banking app leaves the foreground

Then **Auto-Pause Accessibility** on Exclusions:

1. User opens excluded app (e.g. BHIM)
2. Soft-disable Accessibility → bank error gone
3. `AccessibilityAutoResumeService` (FGS) watches Usage Stats
4. User leaves BHIM → soft-enable Accessibility again — **no Settings trip**
5. Switching directly between two excluded apps hands the watch off — Accessibility stays off until the foreground lands outside *all* excluded apps

Works for **every app in the exclusion list**, not just UPI: pause triggers on any
`TYPE_WINDOW_STATE_CHANGED` from an excluded package, and resume waits until no
excluded package is foreground.

Fallback without Unlock: `disableSelf()` + notification to re-enable manually.

| File | Role |
|------|------|
| `ElevatedPermissions.kt` | Grant checks + Unlock shell command list |
| `AccessibilitySecureToggle.kt` | Edit Secure Settings list |
| `AccessibilityAutoResumeService.kt` | FGS resume after leave |
| `PlusKeyAccessibilityService.kt` | Trigger pause on excluded foreground |
| `ReadLogsGrantViaWirelessAdb.kt` | Runs all Unlock shell commands; re-runs if banking grants missing |
| `ExclusionsScreen.kt` | Auto-Pause toggle + hands-free / Unlock CTA |
| `EnableDetection*` | USB/Wireless Unlock copy includes banking grants |

## User steps (once)
1. Settings → Per-App Exclusions → add BHIM (or UPI app)
2. Enable **Auto-Pause Accessibility**
3. Tap **Complete Wireless Unlock** (or USB ADB paste) once
4. Pay daily — Keyforge pauses and resumes itself

## Verify
1. Unlock → Status chips show READ_LOGS + Banking auto-pause ready
2. Exclude BHIM, Auto-Pause on, open BHIM → bank error gone; low “Paused for …” notification
3. Leave BHIM → Accessibility back on without opening Settings; Plus Key remaps again
4. Chain: BHIM → PhonePe (both excluded) → `Handoff` log, no restore in between; launcher → restored once
5. Without Unlock: Auto-Pause still uses `disableSelf` + tap notification after payment

## Debug tips
| Symptom | Likely cause |
|---------|----------------|
| Bank still blocks after Auto-Pause | Soft toggle failed / Unlock missing; check `WRITE_SECURE_SETTINGS` |
| Warning appears once on first open of BHIM | Race: bank's scan runs before our disable propagates; reopen once — see `02-review-fixes.md` |
| Stays paused after leaving BHIM | Usage access missing; AutoResume FGS killed; check log `A11yAutoResume` |
| Unlock says AlreadyGranted but banking chip warns | Old Unlock only had READ_LOGS — run Unlock again |
| Remap never returns | Soft-enable failed → notification path; check Secure Settings list |

Banking detection mechanism (verified): UPI/banking apps query
`AccessibilityManager.getEnabledAccessibilityServiceList()`, which reflects
`ENABLED_ACCESSIBILITY_SERVICES` live — removing Keyforge there is sufficient.
