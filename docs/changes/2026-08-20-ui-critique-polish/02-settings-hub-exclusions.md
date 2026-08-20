# 02 — Settings hub + exclusions

## Why
Settings hub rows were visually flat; exclusions showed raw package names; theme copy was outdated prototype text.

## What
| File | Change |
|------|--------|
| `presentation/settings/SettingsScreen.kt` | HubRow `accentContainer`/`accentTint`; ChevronRight icon; per-row accent colors; exclusions resolve app label via PackageManager; Apps empty state; toggle haptics; theme subtitle |

## Verify
- Each Settings hub row has distinct icon tint (Feedback green, Overlay purple, Restart red, etc.).
- Excluded app shows human label + package subtitle.
- Empty exclusions shows Apps icon + message.

## Debug tips
| Symptom | Likely cause |
|---------|----------------|
| Label falls back to package | App uninstalled or PackageManager lookup failed |
