# Visual Overlay — raise popup to Plus Key

## Why
On device the action pill sat slightly below the physical Plus Key; anchor raised and edge inset tightened.

## What
| File | Change |
|------|--------|
| `VisualActionPopup.kt` | Key anchor at 10% of key height (was 25%, originally 50% center); edge inset 4dp |

## Verify
Preview on screen → pill center aligns with left Plus Key; glow bloom on key contact point.
