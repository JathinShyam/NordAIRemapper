# 2026-08-25 — Built-In pairing moves to a shortService FGS (broadcast-timeout RCA)

## Why

User paired via Built-In on the 2026-08-25 build; READ_LOGS landed, but the
Banking auto-pause chip stayed yellow. `files/unlock_grants.log` contained a
single line:

```
21:49:24.718 RUN pm grant com.nordairemapper android.permission.READ_LOGS
```

No `OUT`, no `attempt=` line, no command #2/#3 — the process died within ~0.7s
of starting grant #1. READ_LOGS still applied because adbd had already executed
that one command.

**Cause**: `PairingReplyReceiver.onReceive` ran pair → mDNS connect discovery →
TLS connect → all three verified grants inside `goAsync()`. The broadcast
window is ~10s; pairing + connect consumed almost all of it, and the system
killed the process mid-grants. Result banner never posted either — a silent
partial failure (the exact class change-safety guards against).

The earlier "blocking drain" RCA (2026-08-24) fixed a *hang* after EOF-less
silent commands; this was the second, independent killer: the *host window*
itself. Both are now closed.

## What

| File | Change |
|------|--------|
| `service/adb/PairingGrantService.kt` (new) | shortService FGS that runs `pairAndGrant` with full timeouts + 150s watchdog; owns completion UX (log-visibility probe, result banners, relaunch, session clear); `startForeground` unconditionally first (FGS contract), `STOP_FOREGROUND_DETACH` keeps the banner alive |
| `service/adb/PairingReplyReceiver.kt` | Thin dispatcher: validates digits/session, starts the FGS. API < 34 (no `shortService`) keeps the legacy in-window path with `quick=true` |
| `service/adb/PairingNotifier.kt` | `CHANNEL_ID` / `ensureChannel` private → internal so the FGS reuses the same channel + notification id |
| `service/adb/ReadLogsGrantViaWirelessAdb.kt` | `runGrantsVerifying`: shell stream work moved to a daemon worker bounded by `join(2s)` (`HANG abandoned` line if stalled); whole run capped at 90s (`DEADLINE reached` line). Verify/retry semantics unchanged |
| `AndroidManifest.xml` | `FOREGROUND_SERVICE_SHORT_SERVICE` permission + `<service … foregroundServiceType="shortService">`, exported=false |

Behavior preserved: same three Unlock commands, same per-command verify/retry
(3×, 400ms), same transcript file, same failure messages, same API-33
fallback behavior.

## Verify

1. `./gradlew :app:assembleDebug :app:testDebugUnitTest` — pass.
2. On-device Built-In end-to-end: Pair now → reply code in notification →
   progress FGS appears → result banner → **all three chips green**
   (READ_LOGS, log visibility, Banking auto-pause).
3. `adb shell run-as com.nordairemapper cat files/unlock_grants.log` — must
   now show RUN + attempt lines for ALL THREE commands.
4. `scripts/device-smoke.sh` all PASS.

## Debug tips

| Symptom | Likely cause |
|---------|--------------|
| Banner says "took too long" | Watchdog (150s) fired — check `unlock_grants.log` for which command stalled |
| `HANG abandoned` in transcript | TLS stream never opened/delivered — connection died post-pair; retry Pair now |
| No progress notification at all after reply | FGS start rejected — check `adb logcat -s PairingGrant PairingReply`; API 33 devices use legacy in-window path instead |
| Grants fine but chip yellow after reinstall | Uninstall wipes READ_LOGS/WSS/usage — rerun Unlock (USB: `ElevatedPermissions.UNLOCK_SHELL_COMMANDS`) |

Note from this incident: uninstall/reinstall wipes ALL Unlock grants
(including USB-granted ones); they must be reapplied after any reinstall.

## CORRECTION — true root cause found on-device (same day, ~22:29)

The FGS build was installed fresh and the user re-paired. The FGS started
(system log: `Background started FGS: Allowed … PairingGrantService`) and the
transcript again ended at `RUN pm grant …READ_LOGS` — but this time logcat
showed the real killer:

```
22:29:32.375 RUN pm grant …READ_LOGS
22:29:32.460 I/ActivityManager: Killing 7608:com.nordairemapper (adj 51):
               permission grant or revoke changed gids
22:29:32.464 W/ActivityManager: Short FGS brought down without stopping
```

**READ_LOGS is backed by a Unix group (`log`). Granting it makes PackageManager
kill the TARGET app's process so the new gids can be applied — unconditionally,
regardless of FGS/broadcast/foreground state.** Our app ran the adb session and
was also the grant target, so it killed itself after command #1. The earlier
"broadcast timeout" theory was a timing coincidence; the 21:49 failure had the
same cause.

### Fix

1. **`ElevatedPermissions.UNLOCK_SHELL_COMMANDS` reordered**: WRITE_SECURE_SETTINGS
   → GET_USAGE_STATS → READ_LOGS **last**. The first two are gid-safe; by the
   time the expected self-kill fires, all grants are applied. Applies to every
   consumer (wireless path, Shizuku path, USB paste block).
2. `runGrantsVerifying` posts a heads-up ("Keyforge may blink once") + transcript
   NOTE right before the READ_LOGS command, so silence below it reads as success.
3. If the process survives (gids unchanged on re-run), the normal Success flow
   (banner, relaunch, watcher sync) completes as before.

### Verify (fresh-install Built-In)

- Transcript shows RUN + verified for WSS and usage; ends around the READ_LOGS
  RUN line (expected).
- All three chips green after reopening Keyforge; `scripts/device-smoke.sh` PASS.

## Debug tips (additions)

| Symptom | Likely cause |
|---------|--------------|
| Process dies exactly at the READ_LOGS grant | Expected gids self-kill — check that commands #1/#2 already verified in `unlock_grants.log` |
| App dies at READ_LOGS even over USB/Shizuku | Same mechanism; order in UNLOCK_SHELL_COMMANDS protects those paths too |
