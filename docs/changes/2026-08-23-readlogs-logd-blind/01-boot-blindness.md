# 01 — Reboot blindness: per-boot log consent + blind-tail auto-heal

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
6. Later probes: a spawn made **while Keyforge was foreground** saw other pids'
   logs immediately (chip = Verified), and a gesture was classified at ~14:52
   right after Enable Detection ran its probe/restart path.

## Root cause
Matches AOSP "Log Info Disclosure" guidance (source.android.com, 2026): on this
Android-16-based build, third-party access to device logs is gated per-boot by a
**foreground consent** mechanism — background spawns (BootReceiver → watcher at
boot) are auto-denied even with READ_LOGS granted; once the app runs its probe in
the foreground, spawns succeed for that boot. The boot-born tail never re-checks,
so it stays blind until something reconnects it. The earlier "USB debugging
(Security settings)" theory is secondary; grants alone were never sufficient.

## What (code fix)
| File | Change |
|------|--------|
| `service/LogVisibilityProbe.kt` | Ground-truth visibility probe (foreground spawn sees other pids?). |
| `service/LogcatWatcherService.kt` | Companion-level tail-health state + `isTailBlindNow()`; new `restart()` (stop→start with suppressed death notification); watchdog/alerts unchanged. |
| `presentation/home/HomeViewModel.kt` | **Auto-heal on Home open**: if READ_LOGS granted and gestures are stale-or-tail-blind, run the probe; when a foreground spawn proves visibility while the live tail is still blind, restart the tail silently. |
| `EnableDetectionViewModel.kt` | Same heal after refresh() / Unlock completes. |
| `service/BootReceiver.kt` + `ServiceNotifications.kt` | Post a "Open Keyforge to restore Plus Key detection" notification right after boot (only when logcat detection is relevant); cleared automatically on first app open. |

Post-reboot user flow is now: tap the boot notification (or open Keyforge) —
the app proves access and reconnects itself; no adb, no Settings trip.

## Verify
1. Reboot device → open Home → within seconds press Plus Key: gesture fires
   (logcat `LogcatWatcher: edge=`, Home silhouette flashes).
2. Blind case (if consent denied): watchdog notification fires; chip on Enable
   Detection reads "Blind" with guidance.
