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

## Flow after this change

1. Open Wireless debugging → Pair dialog open.
2. Tap **Find pairing port** → app discovers port → **heads-up appears:
   "Finish pairing right here — type the 6-digit code"**.
3. Pull down / tap "Enter pairing code", type the code, send.
4. Notification flips to "Pairing…" then "Paired and unlocked" (auto-expires);
   grants run and the watcher reconnects exactly like the in-app path.

Failure paths surface in the same banner (wrong code → re-prompt keeps the
reply action; expired session → asks to restart in-app).

## Verify

1. Run the flow end to end on-device with the pairing dialog in front —
   complete it entirely from the notification.
2. Type 5 digits → notification re-prompts ("That wasn't 6 digits").
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
