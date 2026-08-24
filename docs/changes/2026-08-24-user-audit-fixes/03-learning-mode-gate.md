# 03 — Learning mode gates the engine

## Why

Key setup is a setup flow, but `RemapEngine` kept classifying gestures while it
was open: pressing the Plus Key during re-learning fired the assigned actions,
and under ACCESSIBILITY strategy `shouldConsume` swallowed the learned key.
Confusing at best; at worst it looks like the app is haunted mid-troubleshoot.

## What

| File | Change |
|------|--------|
| `service/LearningMode.kt` (new) | Process-wide `@Volatile active` flag |
| `service/RemapEngine.kt` | `shouldConsume` returns false while learning (keys pass through); `onGesture` still records `lastPlusKeySeenAtMs` (health stays truthful) but skips exclusion/lock/dispatch |
| `presentation/developer/KeyLearningViewModel.kt` | Sets `LearningMode.active = true` in `init`, false in `onCleared`; removed dead `CapturedPress.toRawEvent()` |

Pipeline rules preserved: detectors still emit only; strategy/identity filters
untouched; logcat pre-match untouched; volume/power still pass through.

## Verify

1. Assign an action to Single. Open Key setup and press the Plus Key several
   times — no action fires, no key consumption; "Last Plus Key press" updates.
2. Navigate Key setup → Unlock → back: still gated (VM alive in backstack).
3. Pop back to Home: press Plus Key → action fires again.

## Debug tips

| Symptom | Likely cause |
|---------|--------------|
| Actions fire during Key setup | VM cleared early or flag reset by another screen — check `LearningMode.active` writers |
| Detection permanently dead after leaving Key setup | `onCleared` not running / second VM instance set active=true then cleared — grep all writers |
