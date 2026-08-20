# 01 — Action feedback popup

## Why

Preferences / Visual Overlay toggled `visualOverlayEnabled` and style fields, but remaps never showed an action popup.

## What

| File | Change |
|------|--------|
| `service/ActionFeedbackOverlayService.kt` | New FGS overlay: icon + label, style/accent/glow/hold/animation |
| `RemapActionExecutor.kt` | Calls feedback when `visualOverlayEnabled` (skips `ShowOverlay` / `None`) |
| `AndroidManifest.xml` | Register FGS `specialUse` |
| `VisualOverlayScreen.kt` | Enable toggle + Preview popup CTA |

## Verify

1. Grant Display over other apps.
2. Preferences or Visual Overlay → enable.
3. Remap → Try now (not Show overlay) → bottom popup for hold duration.
4. Change OnePlus/Stock, accent, glow, hold → Preview popup reflects them.
5. Disable visual overlay → no popup.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| No popup | Overlay permission; toggle off; action is Show overlay |
| Flash then gone | holdDurationMs low; FGS start failed (logcat `ActionFeedback`) |
