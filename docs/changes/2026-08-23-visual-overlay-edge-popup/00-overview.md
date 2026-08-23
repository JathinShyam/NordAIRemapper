# Visual Overlay — Plus Key edge popup + glow

## Why
Visual Overlay (action feedback popup) was bottom-center and looked unlike OxygenOS edge signals. Users expect a compact icon pill near the Plus Key with an optional edge glow — distinct from the floating app Overlay menu.

Also fixed a likely runtime failure on API 33: `startForeground` with `SPECIAL_USE` without version guard.

## What
| File | Change |
|------|--------|
| `ui/components/VisualActionPopup.kt` | Edge-aligned pill popup + edge glow layer |
| `ActionFeedbackOverlayService.kt` | Uses shared popup; API 34+ FGS type guard |
| `VisualOverlayScreen.kt` | Modern UI, permission chip, style cards, Preview on screen |

## Verify
1. Grant Display over other apps.
2. Settings → Visual Overlay → Preview on screen → pill on **left edge** at Plus Key height (PhoneDiagram geometry) with glow.
3. Remap Try now / press Plus Key → same edge popup when action fires.
4. Toggle Glow off → pill only, no edge line/bloom.
5. OnePlus vs Stock style cards match runtime popup.
Open `design/nord-edge-prototype.html` → Settings → Visual Overlay for design parity (no live preview box).

## Debug tips
| Symptom | Likely cause |
|---------|----------------|
| No popup | `visualOverlayEnabled` off or overlay permission missing |
| Popup at bottom | Old build — reinstall |
| Service dies instantly | logcat `ActionFeedback` startForeground on API 33 |
| Glow missing | `glowEffects` off in Overlay config |
