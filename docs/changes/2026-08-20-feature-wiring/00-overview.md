# 00 — Feature wiring fixes overview

## Why

UI exposed Visual Overlay, floating-menu style/animation/enable, haptic intensity, and service notification controls that were persisted but not always honored at runtime.

## Fixes in this folder

| File | Covers |
|------|--------|
| [01-action-feedback-popup.md](./01-action-feedback-popup.md) | Visual Overlay action popup service |
| [02-floating-overlay-config.md](./02-floating-overlay-config.md) | Enable gate, accent, glow, animation |
| [03-haptics-notification.md](./03-haptics-notification.md) | Intensity preview + service notification |

## Already working (verified, unchanged)

- Haptic on/off + intensity → `RemapActionExecutor.performHaptic`
- Master remapping switch, lock-screen gates, exclusions, Try now
- Detection strategy / gesture timings / ShowOverlay slots/layout/position
- Backup remaps + overlay config + most settings

## Verify

```bash
export JAVA_HOME="$HOME/.jdks/jdk17"
./gradlew :app:assembleDebug
```

On device: enable Display over apps → Remap Try now with Visual Overlay on → brief popup. Feedback intensity tap → vibe. Overlay settings animation/accent → Show overlay menu reflects them. Service notification off → minimal FGS text.
