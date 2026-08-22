# Home silhouette compact (match design height)

## Why

App Home silhouette used `fillMaxWidth(0.56f)` + aspect ratio, which grew to ~380dp tall on typical phones. Design `.nord5` is fixed **220px**. Users had to scroll to reach Single / Double / Long.

## What

| File | Change |
|------|--------|
| `presentation/home/HomeScreen.kt` | Hero band `height(250.dp)` (~14% above compact 220dp) |
| `ui/components/PhoneDiagram.kt` | Lockup width/offset scale with drawn phone (~54% body) via `BoxWithConstraints` |

Onboarding keeps the larger fraction-based silhouette (welcome hero).

## Verify

1. Open Home on phone — silhouette roughly matches prototype Stage size.
2. Remapping card + all three action rows visible without scrolling (no banner).
3. Never Settle lockup still centered and proportional on the glass.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Still scrolls | Banner present, or density/font scale huge; try `220.dp` |
| Lockup oversized | Fixed width sneak-back; confirm `lockupWidth = phoneWidthDp * 0.54f` |
| Silhouette tiny | Parent height not applied; check `Modifier.height(220.dp)` on Home only |
