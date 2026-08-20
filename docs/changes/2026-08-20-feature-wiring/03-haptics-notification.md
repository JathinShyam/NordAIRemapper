# 03 — Haptics preview + service notification

## Why

Haptic intensity worked at remap time but Feedback had no preview pulse. Service notification toggle was ignored by Logcat FGS.

## What

| File | Change |
|------|--------|
| `FeedbackScreen.kt` | Selecting Light/Medium/Heavy vibrates a preview |
| `LogcatWatcherService.kt` | Honors `showServiceNotification` (detailed vs minimal FGS text) |

## Verify

1. Feedback → tap Heavy → firm click vibe immediately.
2. Settings → Service notification off → restart detection → notification title “Nord AI Remapper” / “Running”.
3. Toggle on → “Plus Key detection active”.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| No vibe preview | Haptic off in Feedback; no VIBRATE permission (manifest) |
| Notification unchanged | Watcher not restarted after toggle |
