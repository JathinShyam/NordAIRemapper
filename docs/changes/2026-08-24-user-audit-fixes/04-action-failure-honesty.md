# 04 — Action execution honesty (failure states, haptics, overlay hint)

## Why

- Global actions (screenshot, lock, recents, home, back, shade, QS) returned
  success feedback even when Accessibility was missing or `performGlobalAction`
  returned false. The Visual Overlay popup claimed success on a failed action.
- Haptic fired before the action was validated — vibration then failure toast.
- `visualOverlayEnabled` defaults ON; without "Display over other apps" the
  popup silently never rendered (debug log only).

## What

| File | Change |
|------|--------|
| `domain/model/ActionFeedback.kt` | + `ActionFeedbackState.ACTION_FAILED` |
| `presentation/common/RemapActionUi.kt` | `ACTION_FAILED` → error icon + caption "Failed" |
| `service/RemapActionExecutor.kt` | `globalAction()` returns Boolean; `globalActionResult()` maps false → `ACTION_FAILED`; haptic skipped when failed; one-time toast when overlay grant missing while visual confirmations enabled |

Toasts for each specific failure (missing DND access, fixed volume,
Accessibility off) are unchanged.

## Verify

1. Turn Accessibility off (or use an action while it rebinds), fire Screenshot:
   toast explains + popup shows "Failed"; no success haptic.
2. Revoke Display-over-apps, keep Visual Overlay on: first remap fires a
   one-time hint toast; subsequent failures stay silent until process restart.
3. Successful actions unchanged: state captions (On/Off/Ring/…), haptics, popup.

## Debug tips

| Symptom | Likely cause |
|---------|--------------|
| Popup shows "Failed" though action worked | `performGlobalAction` returned false asynchronously — check logcat tag `RemapActionExecutor` |
| Hint toast repeats | `overlayPermissionHintShown` is per-process by design; repeats mean executor recreated (should not — @Singleton) |
