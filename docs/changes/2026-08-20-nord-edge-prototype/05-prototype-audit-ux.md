# 05 — Prototype audit & copy UX pass

## Why

Full design review found heading/content mismatches, Stage vs Home naming, Remap always saving to Single, wrong Preferences screen, and a CSS class collision on the Nord 5 SVG.

## Fixes applied (`design/nord-edge-prototype.html`)

| Area | Fix |
|------|-----|
| SVG | Display class `screen` → `glass` (was hidden by `.screen { display:none }`) |
| Naming | User-facing **Home** (not Stage); Remap / Overlay / Overlay settings / Backup & Restore |
| Remap | Tracks Single/Double/Long; title updates; Done + Try now; saves to the opened pad |
| Home | Status “Service active”; Actions label; master subtitle; Double = Rear camera |
| Preferences | Matches app: haptic + visual overlay + OnePlus/Stock style (theme moved to Settings) |
| Feedback | Haptic feedback toggle + Vibration intensity Light/Medium/**Heavy** |
| Visual Overlay | Full title; action-popup copy |
| Lock Screen | Enabled/Disabled when locked |
| Overlay | Close + tap backdrop to dismiss; title “Overlay” |
| Lab | Accessibility (not A11y) |
| Onboarding | Finish → Home; a11y secondary “I've enabled it” |

## Verify

Hard-refresh the HTML. Open Double pad → Remap title “Double” → Done updates Double row. Preferences should not show Theme. Toggle Remapping → Plus Key glow off. Overlay Close returns Home.
