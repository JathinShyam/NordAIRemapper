# Remove Settings Preferences hub entry

## Why
Preferences duplicated Feedback (haptics) and Visual Overlay (popup toggle + style). Shortcuts should link to focused screens only.

## What
| File | Change |
|------|--------|
| `SettingsScreen.kt` | Removed Preferences row from Shortcuts |
| `NordNavHost.kt` | Removed `preferences` route |
| `PreferencesScreen.kt` | Deleted |
| `design/nord-edge-prototype.html` | Removed hub row, nav item, screen, JS |
| `design/settings-preview.html` | Removed hub row |

## Verify
Settings → Shortcuts: Feedback, Visual Overlay, Overlay Settings (no Preferences). Haptics still in Feedback; visual overlay toggle/style in Visual Overlay.
