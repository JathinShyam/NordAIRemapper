# 09 — Built-In pairing: finish from a heads-up notification

## Why

During Built-In Wireless pairing, the system "Pair device with pairing code"
dialog covers Keyforge, so the user had to memorize the code, switch back to
the app, type it, and submit. Classic context-switch friction — the exact case
WhatsApp-style direct-reply notifications solve.

## What

| File | Change |
|------|--------|
| `service/adb/PairingSession.kt` (new) | Volatile holder for the active endpoint (host / pairingPort / connectPort) so the reply path can pair without the UI |
| `service/adb/PairingNotifier.kt` (new) | HIGH-importance channel + direct-reply notification (`RemoteInput`), progress and self-expiring result banners. Reply PendingIntent is FLAG_MUTABLE as RemoteInput requires |
| `service/adb/PairingReplyReceiver.kt` (new, Hilt) | Reads the 6 digits from the RemoteInput, runs `pairAndGrant(code, host, pairingPort, connectPort)` via `goAsync`, posts progress/result; invalid input re-prompts inside the same notification |
| `presentation/detection/EnableDetectionViewModel.kt` | Discovery success → arms `PairingSession` + posts the prompt; port/connect edits keep the session in sync; session cleared + notification cancelled after pairing or when VM dies |
| `presentation/detection/EnableDetectionScreen.kt` | Built-In panel tells the user they can answer from the notification |
| `AndroidManifest.xml` | `PairingReplyReceiver`, exported=false |

## Flow after this change (v2 — formless checklist)

**Selector**: three equal-width segments in ONE row, single-select
(Built-In / Shizuku / Manual ADB). A notifications-permission card sits above
the selector until POST_NOTIFICATIONS is granted.

**Built-In = 3-step checklist, zero forms:**
1. Developer options — probed via `Settings.Global.DEVELOPMENT_SETTINGS_ENABLED`;
   unchecked opens About device ("tap Build number 5–7×").
2. Wireless debugging — probed via `Settings.Global.adb_wifi_enabled`;
   unchecked opens Developer options.
3. **Pair now** (gated on 1–2) — opens the Wireless debugging page AND starts a
   continuous mDNS watch + posts a "Watching for the pairing dialog…"
   heads-up. When the pairing service appears, the SAME notification upgrades
   to an inline 6-digit code box. Reply → pairs, grants, and relaunches Keyforge
   automatically. No port/code fields anywhere in the app.

Old `pairAndGrant()` in-app form path, its fields, and the one-shot discovery
were removed; the reply receiver is now the only Built-In completion path.

Failure paths surface in the same banner (wrong code → re-prompt keeps the
reply action; expired session → asks to restart in-app).

## Verify

1. Run the flow end to end on-device with the pairing dialog in front —
   complete it entirely from the notification.
2. Type 5 digits → notification re-prompts ("That wasn't 6 digits").
3. Steps 1–2 auto-recheck every ON_RESUME (returning from Settings updates the checks instantly).
3. Let the code expire → failure banner suggests generating a new one;
   in-app fields still work.
4. Deny notifications for Keyforge → prompt silently skipped; in-app flow
   unaffected.

## Debug tips

| Symptom | Likely cause |
|---------|--------------|
| No heads-up while app in background | POST_NOTIFICATIONS denied (check Permissions screen) or channel disabled |
| Reply does nothing | receiver skipped — check `dumpsys activity broadcasts` for policy skips; PendingIntent must be created by our own uid (it is) |
| "Session expired" right away | process restarted between discovery and reply — just tap Find pairing port again |


## RCA follow-up — banking grants dropped over Wireless (OPlus)

**Symptom**: Built-In pairing "succeeded" but the Banking auto-pause chip stayed
red; `WRITE_SECURE_SETTINGS` + usage access were missing while READ_LOGS was fine.

**Cause**: OxygenOS permits `pm grant READ_LOGS` over WIRELESS adb but silently
drops security-sensitive ops (`WRITE_SECURE_SETTINGS`, `appops set`) unless
Developer options → **"USB debugging (Security settings)"** is ON.

**Fix**: `pairAndGrant` now runs each command independently
(`runGrantsVerifying`) and verifies BOTH READ_LOGS and
`canAutoResumeAccessibility()` afterwards. Partial success returns a Failed
result whose message names the exact OEM toggle to enable and says to tap
Pair now again. Old code verified only READ_LOGS and reported false success.


## Follow-up — deep-link each checklist step

Every Built-In step now lands on its respective page:
1. About device (`DEVICE_INFO_SETTINGS`) for Build number taps
2. The Wireless debugging sub-screen itself (`ADB_WIRELESS_SETTINGS`, with
   Developer-options fallback) for the toggle
3. Pair now opens the same Wireless debugging page automatically before the
   watch starts, so "Pair device with pairing code" is right in front of the
   user while the heads-up upgrades to a code box.


## RCA instrumentation — per-command verify/retry + transcript file

`runGrantsVerifying` now checks each command's effect IN-PROCESS
(READ_LOGS via checkSelfPermission, WRITE_SECURE same, usage via AppOps),
retries up to 3× (400ms apart), drains and logs command output, and appends
every RUN/OUT/ERR/attempt line to `filesDir/unlock_grants.log`. Pull with:

    adb shell run-as com.nordairemapper cat files/unlock_grants.log

Rationale: logcat's 256KB ring rotates within minutes on ColorOS WM spam, so
file evidence is the only durable trace of what `pm grant` actually printed
over Wireless adb when the OEM drops security-sensitive ops.


## RCA CONFIRMED — blocking drain hung the grant flow (fixed)

`unlock_grants.log` from the user's run contained ONLY
`RUN pm grant …READ_LOGS` — no OUT, no attempt lines, no command #2/#3.

**Cause**: `pm grant` prints nothing on success and adbd-over-TLS doesn't send
EOF promptly; the drain loop (`read()` until EOF) blocked forever. Android's
broadcast timeout then killed the process. READ_LOGS applied because adbd had
already executed command #1 — commands #2 (WRITE_SECURE_SETTINGS) and #3
(appops) were never sent. Not an OEM policy block at all.

**Fix**: bounded non-blocking read window per command
(`SHELL_OUTPUT_WINDOW_MS=300`, poll via available()), never waits for EOF.
Output capture is best-effort; in-process verification remains authoritative.


## Bug fix — step 3 flipped to done without pairing

**Report**: user left mid-watch to enable notifications, returned, and step 3
showed success although nothing paired.

**Causes (three stacked)**:
1. Watch loop ran forever in background (`viewModelScope` outlives screen) —
   no timeout.
2. It accepted ANY `_adb-tls-pairing` advertisement on the LAN — including
   this phone's own page state or a neighbour's device — instantly setting
   `discoveredPort`.
3. Step 3 treated "port seen" as done, when done must mean UNLOCKED.

**Fix**: 4-minute hard cap on the watch (timeout clears the heads-up and asks
for a retry); discovered endpoints are accepted only when host == this
phone's Wi-Fi IPv4 (or loopback), foreign advertisements logged + ignored;
step-3 `done` is now `readLogsGranted`, with "Pair now (new code)" retry
label while port is known but unlock hasn't happened. `stopPairingWatch`
also cancels the abandoned ongoing notification.
