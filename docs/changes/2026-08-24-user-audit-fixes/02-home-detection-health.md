# 02 — Home reflects detection liveness, not just settings flags

## Why

The Home ribbon pulsed green purely from `serviceEnabled && accessibilityEnabled`
— both static flags. The watcher could be blind (logd consent), killed, or
notification-denied while the UI looked perfectly healthy. Also:
- "Last Used: 12s ago" froze between state emissions (no clock).
- Banner order showed "Assign your presses" (claims "Keyforge sees every press")
  while paused — false, because the watcher is stopped when paused.
- Denying POST_NOTIFICATIONS silently disabled every failure alert.

## What

| File | Change |
|------|--------|
| `presentation/home/HomeUiState.kt` | + `tailSawNonSelf`, `notificationsEnabled`, ticking `nowMs` |
| `presentation/home/HomeViewModel.kt` | RuntimeFlags extended; 10s `ticker` flow combined into uiState (only while subscribed); banner order: paused **before** assign-presses; `openAppNotificationSettings()` |
| `presentation/home/HomeScreen.kt` | Ribbon: "Starting log stream…" (grey, no pulse) when enabled+granted but tail never saw non-self lines; tap → Unlock screen. Inline warning row when notifications are off (deep-links to app notification settings). Master-switch row is now `toggleable`. Settings icon 48dp target |
| `presentation/common/Ticker.kt` (new) | `rememberNowTicker()` shared composable |

## Verify

1. Fresh boot with grants: Home shows "Starting log stream…" briefly after
   opening the app, then "Service active" once the tail sees system logs.
2. Kill the FGS (`adb shell am stopservice .../LogcatWatcherService`): ribbon
   recovers on next resume; death notification still posted.
3. Disable notifications for Keyforge → warning row appears under master
   switch; tapping opens the system notification page.
4. Leave Home visible >10s with an old last-seen value: text ticks.

## Debug tips

| Symptom | Likely cause |
|---------|--------------|
| Stuck on "Starting log stream…" | logd blind — run device-smoke.sh; reboot / re-run Unlock |
| Ticker never updates | uiState not resubscribed (WhileSubscribed window) — check collector in HomeScreen |
