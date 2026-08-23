# Visual Overlay — raise popup to Plus Key

## Why
On device the action pill sat slightly below the physical Plus Key. RCA: diagram center (21.9%) matches PhoneDiagram but Nord 5 full-screen overlay needs ~40dp raise.

## What
| File | Change |
|------|--------|
| `VisualActionPopup.kt` | `computeVisualOverlayAnchorY()` — key center minus 40dp raise; debug log |
| `VisualActionPopupPlacementTest.kt` | Documents Nord 5 pixel math |

## Verify
1. `./gradlew :app:testDebugUnitTest --tests VisualActionPopupPlacementTest`
2. Install build → Preview on screen → `adb logcat -d | grep VisualActionPopup` shows `anchorY≈403px` on 2800px screen (40dp + 2.5% raise — midpoint of 40dp-only and 40dp+5%)
3. Pill center ~14% from top
