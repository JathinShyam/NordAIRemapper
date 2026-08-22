# Audit fixes — stability, tests, light theme, overlay apps

## Why

Full codebase audit (see `IMPROVEMENT_PLAN.md` at repo root) found: an unbounded
logcat process leak, silent watcher death, a boot-crash path, missing TRD §10
tests with CI never running any, broken light-theme accents, main-thread app
enumeration, unsafe backup import, no app/URL shortcuts in overlay slots, and
no detection health signal.

Commits `8721497..0314072` (branch work after `e09fce0`).

## Files

| Topic | Doc |
|---|---|
| Watcher single-tail + hot-reload + reconnect | [01-logcat-watcher-stability.md](01-logcat-watcher-stability.md) |
| Boot receiver + guarded FGS starts | [02-boot-and-fgs-guards.md](02-boot-and-fgs-guards.md) |
| Unit tests + CI gate | [03-tests-and-ci.md](03-tests-and-ci.md) |
| Detection feedback (pulse filter, system-key guard) | [04-detection-feedback.md](04-detection-feedback.md) |
| Service hardening (exclusions, media, glow, backup rules) | [05-service-hardening.md](05-service-hardening.md) |
| UI: accents, pickers, onboarding | [06-ui-accent-pickers-onboarding.md](06-ui-accent-pickers-onboarding.md) |
| Backup confirmations | [07-backup-safety.md](07-backup-safety.md) |
| Overlay app/URL slots | [08-overlay-app-url-slots.md](08-overlay-app-url-slots.md) |
| Last-seen health line | [09-last-seen-health.md](09-last-seen-health.md) |

## Verify

1. `./gradlew :app:testDebugUnitTest :app:assembleDebug` green (24 tests).
2. On device: toggle any setting repeatedly → `adb shell ps -A | grep logcat`
   shows exactly one app-owned logcat process while detection is enabled.
3. Lab pattern edit applies without toggling the master switch.
