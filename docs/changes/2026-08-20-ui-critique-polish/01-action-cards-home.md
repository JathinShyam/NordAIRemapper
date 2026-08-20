# 01 — Action cards + Home polish

## Why
Home action cards looked uniform; press-type badges were missing; master switch needed clearer hierarchy; phone diagram should offer demo feedback on tap.

## What
| File | Change |
|------|--------|
| `ui/components/ActionCard.kt` | Optional `iconContainer`/`iconTint`; badge overlay on icon corner when both set; Crossfade on subtitle |
| `presentation/home/HomeScreen.kt` | Category accents from `categoryAccent(categoryFor(action))`; press badges 1×/2×/⏳; elevated master card; haptic Switch; diagram tap pulse |

## Verify
- Open Home with assigned actions — icons use category colors; badge chip on icon corner.
- Toggle remapping — card border shifts to primaryContainer tint; haptic fires.
- Tap phone silhouette — brief Plus Key glow even when remapping off.

## Debug tips
| Symptom | Likely cause |
|---------|----------------|
| Badge overlaps icon oddly | Check 20dp overlay offset on 36dp box |
| No pulse on tap | `demoPulse` coroutine not launching — verify clickable on PhoneDiagram |
