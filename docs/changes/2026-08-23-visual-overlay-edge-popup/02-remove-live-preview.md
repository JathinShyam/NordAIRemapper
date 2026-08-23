# Remove Visual Overlay live preview

## Why
In-settings phone mock duplicated what **Preview on screen** already shows on device; removed to simplify the screen.

## What
| File | Change |
|------|--------|
| `VisualOverlayScreen.kt` | Removed Live preview section |
| `VisualOverlayPreview.kt` | Deleted |
| `design/nord-edge-prototype.html` | Removed live preview block + CSS |

## Verify
Visual Overlay screen goes Enable toggle → Style → Appearance → Hold duration → Preview on screen (no 220dp preview box).
