# Phase 5 — RemapAction executors

**Status:** ✅ Done  
**Gate:** `./gradlew assembleDebug`

## Goal

Execute every remappable action from the sealed hierarchy (overlay show may log stub until Phase 9).

## Deliverables

| Component | Behavior |
|-----------|----------|
| `RemapActionExecutor` | Implements `ActionDispatcher` |
| Global actions | Via `AccessibilityServiceHolder` |
| Flashlight | TorchCallback tracking |
| DND / WRITE_SETTINGS | Deep-link if not granted |
| Haptics | Honors settings flag |
| Hilt | Bind executor instead of log-only stub |

## Acceptance

- [x] All sealed branches handled in `when`
- [x] Soft-fail with logs; no crash on missing permission
- [x] `ShowOverlay` safe no-op/log until Phase 9

## Out of scope

UI to pick actions (Phase 7).
