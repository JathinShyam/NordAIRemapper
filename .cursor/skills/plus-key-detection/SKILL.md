---
name: plus-key-detection
description: >-
  Diagnose and change Plus Key detection, gesture classification, logcat
  parsing, accessibility key filtering, key learning, and RemapEngine.
  Use when editing LogcatWatcherService, GestureClassifier, RemapEngine,
  PlusKeyAccessibilityService, KeyEventBus, KeyLearning*, or when single/
  double/long press fire the wrong action.
---

# Plus Key detection

Read `AGENTS.md` first. Detection is the product’s hardest layer.

## Two strategies

| | A — Accessibility | B — Logcat |
|---|---|---|
| When it works | Key reaches `onKeyEvent` | Nord 5 / OxygenOS (usual case) |
| Identity | Learned `keyCode`/`scanCode` | Match pattern only |
| Source enum | `DetectionStrategy.ACCESSIBILITY` | `DetectionStrategy.LOGCAT` |

Plus Key is handled by `OplusKeyEventUtil` and **often never appears in Key setup**. Volume up/down showing there only proves Accessibility works for normal keys.

## Pipeline rules

1. Detectors **emit only**. No Room/IO on the key dispatch thread.
2. `RemapEngine` filters by `settings.detectionStrategy`.
3. Accessibility DOWN/UP: `matchesPlusKey`. Prefer `scanCode` when `> 0`.
4. Logcat DOWN/UP: **skip** `matchesPlusKey`. Codes are `-1`. If identity is unconfigured, matching would drop every edge and only a leftover PULSE path would “work” — that is how hold used to fire single/double.
5. `GestureClassifier` is wait-then-decide on Main.immediate:
   - DOWN starts long-press timer
   - hold ≥ `longPressThresholdMs` → `LONG_PRESS`, ignore that UP
   - UP before long → tap count; 2 within `doublePressWindowMs` → `DOUBLE_PRESS`; else `SINGLE_PRESS` after the window
   - never fire single before the double window ends
   - ignore duplicate DOWN while already down

## Logcat parsing (regression-prone)

Typical Nord 5 lines for one tap:

- `KEYLOG_PhoneWindowManagerExtImpl` `KEYCODE_ACTION_BUTTON_CLICK` `ACTION_DOWN` then `ACTION_UP` (scanCode 735) — **preferred match**
- `KEYLOG_OplusKeyEventUtil` `should not notify undefined keys` **twice on down and twice on up** (legacy pattern)

**Do:**

- Default `logcatPattern` is `KEYCODE_ACTION_BUTTON_CLICK`. Migrate stored `KEYLOG_OplusKeyEventUtil` to that.
- `logcat -b main -T 1` (one buffer; `-T 1` skips replay)
- Skip self logs (`LogcatWatcher` / `RemapEngine`) — logging a matched line used to re-trigger the watcher
- Use `LogcatKeyEdgeCoalescer`: one physical press → one DOWN + one UP
- Parse `action_down` / `action_up` first. Never substring-match `"down"` (`undefined` contains it)

**Do not:**

- Count each matching line as a completed tap (`onPulse` as two taps)
- Hard-code a Nord 5 keyCode/scanCode as the only detection path
- Consume volume/power in Accessibility

## Symptom → likely cause

| User report | Likely bug |
|---|---|
| Single sometimes does the double action | Two log lines per press counted as two taps |
| Hold does single/double, never long | DOWN dropped (`matchesPlusKey` on logcat) or only PULSE path |
| Every tap becomes long press | DOWN with no UP (over-debounce or 1-line firmware) |
| Key setup shows volume, not Plus Key | Expected on Nord 5 — use logcat |
| Two rows per volume press | DOWN and UP; group in learning UI, do not “fix” by dropping UP |

## After changing this area

Build `:app:assembleDebug`. For the phone, follow [ship-debug-apk](../ship-debug-apk/SKILL.md) (CI APK vs local signature). Ask the user to retest single, double, and ~1s hold.
