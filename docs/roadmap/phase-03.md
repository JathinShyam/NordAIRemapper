# Phase 3 — Accessibility detection, gesture classifier, key learning

**Status:** ✅ Done  
**Gate:** `./gradlew assembleDebug`

## Goal

Ship the riskiest path first: observe hardware keys, classify gestures, learn Plus Key identity on device.

## Deliverables

| Component | Behavior |
|-----------|----------|
| `KeyEventBus` | SharedFlow of `RawKeyEvent` |
| `GestureClassifier` | Wait-then-decide single/double/long |
| `RemapEngine` | Filter by strategy + identity; dispatch actions |
| `PlusKeyAccessibilityService` | `FLAG_REQUEST_FILTER_KEY_EVENTS`, thin `onKeyEvent` |
| Accessibility XML | `canRequestFilterKeyEvents=true` |
| Key learning UI | List events; “Set as Plus Key”; service status |
| Nav | Routes `home`, `key_learning` |

## Acceptance

- [x] Service declared in manifest
- [x] Learning screen collects events when service enabled
- [x] Identity persisted via SettingsRepository
- [x] Unrelated keys not consumed (consume only learned match)

## Manual device test (still required on Nord 5)

1. Enable Accessibility for Keyforge.  
2. Open Key setup → press Plus Key.  
3. Confirm keyCode/scanCode appear; save identity.

## Out of scope

Logcat strategy, full Home chrome, remap UI.
