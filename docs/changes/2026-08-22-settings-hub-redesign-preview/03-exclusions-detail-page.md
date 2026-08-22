# 03 — Exclusions detail page in HTML previews

## Why
Compose moved Per-App Exclusions off the Settings hub onto its own screen. The HTML designs needed the same flow so the change is visible without installing the APK.

## What
| File | Change |
|------|--------|
| `design/settings-preview.html` | Two screens: Settings hub → Per-App Exclusions (empty state / app cards / add+remove); hub chip syncs |
| `design/nord-edge-prototype.html` | `exclusions` in `screens`; hub row `data-go="exclusions"`; card list UI + demo add/remove; chip on Reliability row |

## Verify
1. Open `design/settings-preview.html` in a browser.
2. Scroll to Reliability → tap **Per-App Exclusions**.
3. Add demo apps, remove one, go back — hub chip should show `None` / `N Apps`.
4. Same flow from Settings in `design/nord-edge-prototype.html`.

## Debug tips
| Symptom | Likely cause |
|---------|----------------|
| Tap does nothing in prototype | `exclusions` missing from `screens` array |
| Empty state never hides | JS still toggling old chip-list IDs |
| Chip stuck on None | `renderExcl()` not called after add |
