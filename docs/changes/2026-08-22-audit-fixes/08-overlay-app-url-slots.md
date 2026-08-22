# 08 — Overlay slots support app shortcuts + URLs

## Why
Slot catalog filtered out `LaunchApp`/`OpenUrl` even though README markets the
overlay for apps and the executor already supported both. App shortcuts are
the most-wanted floating-menu content.

## What
| File | Change |
|------|--------|
| `presentation/overlay/OverlaySettingsScreen.kt` | Catalog keeps both entries; picking them dismisses the catalog and chains to the existing `AppPickerSheet` / `UrlInputSheet` (one modal at a time), saving into the tapped slot; URL sheet pre-fills the slot's current URL |
| `presentation/overlay/OverlaySettingsViewModel.kt` | App enumeration (`queryLaunchableApps`) + loading state for the chained picker |

Rendering needs no change: `displayName()/icon()` cover LaunchApp label and
"Open link"; `FloatingOverlayService` dispatches through `ActionDispatcher`.

## Verify
1. Overlay settings → tap Slot 1 → Apps → Launch app… → pick an app.
2. Trigger overlay via ShowOverlay press → tile launches the app.
3. Same for URL slot; preview shows the label.

## Debug tips
| Symptom | Likely cause |
|---|---|
| Tile shows package name not label | App uninstalled since assignment (`getLaunchIntentForPackage` null → "App not found" toast) |
