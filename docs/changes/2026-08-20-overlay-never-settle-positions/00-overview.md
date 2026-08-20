# Overlay polish — Never Settle, positions, pill bar, slot picker

## Why

Home silhouette felt empty; Left/Right overlay alignment was confusing and broke pill-bar layout; slot picker was plain text unlike Remap.

## What

| File | Change |
|------|--------|
| `PhoneDiagram.kt` | OnePlus mark + NEVER/SETTLE; theme-aware chrome for light/dark |
| `OverlayConfig.kt` | Positions → Top / Middle / Bottom (+ legacy JSON migration) |
| `FloatingOverlayService.kt` | Vertical placement for grid + pill; fixed-width pill tiles |
| `OverlaySettingsScreen.kt` | Position chips; Remap-style category-colored action sheet |
| `OverlayPreview.kt` | Pill preview uses fixed-width rows |
| `ActionCategoryAccent.kt` | Shared category accent colors |
| `design/nord-edge-prototype.html` | Motto on Nord 5 silhouette |

## Verify

1. Home silhouette shows “Never Settle” on the phone screen.
2. Overlay Settings → Position is Top / Middle / Bottom only.
3. Pill bar: icons form straight rows; top/middle/bottom placement works.
4. Slot picker shows colored icon rows like Remap.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Old Left/Right saved | Migrates to Middle via `OverlayPositionSerializer` |
| Pill icons still uneven | Stale APK — reinstall; tiles must use fixed `tileWidth` |
