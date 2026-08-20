# 03 — Nord 5 Stage silhouette + Plus Key remapping glow

## Why

Stage/Home silhouette looked generic / toy-narrow. Remapping off should leave a normal device with a quiet Plus Key.

## Sources (exact)

| Spec | Value |
|------|--------|
| Body | **163.4 × 77 × 8.1 mm** ([GSMArena](https://www.gsmarena.com/oneplus_nord_5_5g-13992.php)) |
| Body ratio W/H | **77 / 163.4 ≈ 0.4712** |
| Screen-to-body | ~90.1% |
| Display aspect | 19.8:9 |
| Layout | Left Plus Key; right volume above power; centered punch-hole; slightly-rounded flat frame |

## What

| Path | Change |
|------|--------|
| `design/nord-edge-prototype.html` | SVG `viewBox` in **millimetres** (`0 0 80 163.4`), body rect exactly `77×163.4`, height ~220px — no fake skinny inset |
| `ui/components/PhoneDiagram.kt` | Fits body to canvas at 77/163.4; only 1.5 mm key gutters; no 0.78 shrink |
| `presentation/home/HomeScreen.kt` | `fillMaxWidth(0.56f) + aspectRatio(77/163.4)`; `highlightKey = serviceEnabled` |

## Verify

Prototype Stage: silhouette should look like a real phone width (not a stick). Remapping on → cyan Plus Key; off → neutral. App Home: same ratio.
