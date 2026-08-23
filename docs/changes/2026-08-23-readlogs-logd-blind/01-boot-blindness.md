# 01 — Reboot blindness: ColorOS resets the log authorization every boot

## Why (RCA for "detection dies after every reboot")
Timeline proven on-device (2026-08-23, CPH2707 / OxygenOS 16.0.5.1002):

1. `pm grant READ_LOGS` applied via USB → **detection worked** (~14:01; DataStore
   `last_plus_key_seen_ms` = 14:02 local).
2. Reboot at ~14:03.
3. Post-boot: grant still present (`granted=true`, user 0), accessibility bound,
   watcher FGS running, fresh `logcat` child connected — yet `run-as` probe shows
   **0** system_server lines vs 338 shell-side. Detection dead.
4. Idempotent re-grant and revoke+grant cycles do NOT restore visibility.
5. DataStore has no learned key identity → the working channel was definitively
   logcat, so the pre-reboot state truly was logd honoring READ_LOGS.

Conclusion: on this firmware, cross-app log delivery is gated by the OEM
**"USB debugging (Security settings)"** authorization (Developer options), not
just the Android permission. That toggle is what flipped mid-session when the
user enabled it per the remediation ladder — and ColorOS resets it on every
reboot (anti-theft behavior known on OPPO/OnePlus builds). The Android-level
grant persists forever, which makes every in-app/system check lie.

## What (code fix)
| File | Change |
|------|--------|
| `service/LogVisibilityProbe.kt` | Streams a few seconds of main buffer from our uid; VISIBLE on first non-self-pid line, BLIND on timeout. This is ground truth, independent of PM state. |
| `EnableDetectionViewModel` / `EnableDetectionScreen` | New status chip + status message driven by real visibility: "System log access verified" vs "Blind: enable USB debugging (Security settings), then reboot". Shown after refresh() and after Unlock completes. |
| `scripts/device-smoke.sh` | Already fails with the same guidance (logd BLIND line). |

Watchdog from `00-rca-blind-detection.md` remains as the runtime safety net.

## User remediation after each reboot
1. Developer options → enable **USB debugging (Security settings)** (may ask for OPPO account/SIM).
2. Reboot if detection still doesn't return, then re-run Unlock once.
3. Verify via Enable Detection screen chip or `scripts/device-smoke.sh`.

If ColorOS ever stops resetting the toggle, the chip simply stays green and
nothing else needs to change.

## Verify
1. With toggle ON: probe VISIBLE, chip green, key presses fire.
2. After reboot: chip turns red with exact instruction instead of silent death.
