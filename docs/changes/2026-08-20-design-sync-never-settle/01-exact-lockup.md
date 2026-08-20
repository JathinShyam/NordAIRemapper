# Exact Never Settle silhouette lockup

## Why

User required an exact match (mark, motto letterforms, weight, tracking, bar alignment, color) — not a system-font approximation.

## What

| File | Change |
|------|--------|
| `app/.../res/drawable/never_settle_lockup.png` | Composed lockup: authentic OnePlus 1+ path + USPTO NEVER/SETTLE boxed motto (Torch Red `#EB0029`). Mark ≈ 50% of bar width for balanced stack. |
| `app/.../ui/components/PhoneDiagram.kt` | Silhouette uses the lockup drawable (no live text) |
| `app/.../ui/theme/Color.kt` | `HeadingRed` → `#EB0029` |
| `design/never-settle-lockup.png` | Same asset for HTML prototype |
| `design/nord-edge-prototype.html` | Silhouette motto = lockup image |
| `design/assets/` | Working refs (trademark JPEG, SVG sources) — not required at runtime |

## Verify

1. Home silhouette: red 1+ mark above two red bars with white NEVER / SETTLE matching classic OnePlus boxed motto.
2. Open `design/nord-edge-prototype.html` — same lockup on the phone glass.
3. `./gradlew :app:compileDebugKotlin`

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Soft / fringed letters | Regenerated without hard threshold; rebuild from `design/assets/never-settle-furm.jpg` |
| Mark missing “1” | Broken SVG fill when rasterizing `oneplus-red.svg` |
| Wrong aspect | Drawable is 800×711 — keep `aspectRatio(800f/711f)` |
