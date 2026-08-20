# Pill Bottom row + vibrant lockup + bolder top titles

## Why

1. Pill needed a **Bottom** mode: one horizontal icon row that scrolls when slots overflow.
2. Home/Onboarding OnePlus lockup used a soft bitmap that looked dull/blurred.
3. Top-bar headings requested ExtraBold, but Space Grotesk only ships up to Bold — ExtraBold was synthetic and looked soft.

## What

| File | Change |
|------|--------|
| `OverlayConfig.kt` | Pill positions: Left / Right / **Bottom** |
| `FloatingOverlayService.kt` | Bottom pill = `LazyRow` horizontal scroll |
| `OverlayPreview.kt` / `OverlaySettingsScreen.kt` | Bottom preview + position chips |
| `ic_oneplus_mark.xml` + `PhoneDiagram.kt` | Vector 1+ mark + solid Torch Red NEVER/SETTLE bars |
| `Type.kt` / `NordHeading.kt` | Real Bold + slightly larger `titleLarge` (24sp) |
| `PreferencesScreen.kt` | Use `titleLarge` like other bars |
| `design/nord-edge-prototype.html` | Bottom pill chrome |

## Verify

1. Overlay → Pill → **Bottom**: single row; swipe sideways if many slots.
2. Left/Right still vertical columns.
3. Home + Onboarding: sharp red 1+ mark and motto bars (not soft PNG).
4. Top bars (Plus Key, Settings, Preferences, …) read clearly bolder.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Bottom looks like grid | Layout still Grid, or stale APK |
| Pill Bottom not scrollable | Need >~4–5 slots / large icon size |
| Logo still soft | Stale APK still loading old bitmap |
