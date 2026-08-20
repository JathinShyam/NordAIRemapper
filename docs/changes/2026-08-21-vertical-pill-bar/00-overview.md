# Vertical pill bar (Left / Right only)

## Why

Pill bar was laid out as two horizontal rows of three icons — same silhouette as Grid — so switching layout style looked identical. Pill should be a **single vertical column** of icons on the **Left or Right** edge only; Grid keeps Top / Middle / Bottom.

## What

| File | Change |
|------|--------|
| `domain/model/OverlayConfig.kt` | `LEFT` / `RIGHT` positions; `coerceFor` / `isValidFor`; default pill + LEFT |
| `FloatingOverlayService.kt` | Pill = one `Column` of tiles; edge align/pad; SLIDE on X |
| `OverlayPreview.kt` | Vertical strip preview, edge-aligned |
| `OverlaySettingsScreen.kt` | Position chips filtered by layout style; copy updated |
| `OverlaySettingsViewModel.kt` | Coerce position when style changes / when observing |
| `design/nord-edge-prototype.html` | Pill chrome + layout/position segs |

## Verify

1. Overlay Settings → Layout **Pill bar** → Position shows **Left / Right** only.
2. Preview and live overlay: icons stack in one vertical column (not 3×2).
3. Switch to **Grid** → Position becomes **Top / Middle / Bottom**; panel is 3×2 again.
4. Switching styles coerces invalid positions (e.g. Middle → Left for pill).

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Pill still looks like a grid | Stale APK, or still on Grid layout |
| No position selected | Saved Top/Middle with pill — open settings once (observe coerces) or switch layout |
| Old `LEFT_EDGE` JSON | Maps to `LEFT` via serializer |
