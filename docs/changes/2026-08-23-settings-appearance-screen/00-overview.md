# Settings Appearance sub-screen

## Why
Settings hub was crowded; theme and notification toggles belong on a focused Appearance page like other shortcuts.

## What
| File | Change |
|------|--------|
| `AppearanceSettingsScreen.kt` | Theme, Dynamic Color, OLED Black, Service Notification |
| `SettingsScreen.kt` | Hub row → Appearance |
| `NordNavHost.kt` | `appearance` route |
| Design prototypes | Hub row + Appearance screen |

## Verify
Settings → Appearance → theme segment + toggles. Hub no longer shows inline theme block.
