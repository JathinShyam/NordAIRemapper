# 00 — Nord Edge Compose implementation overview

## Why

User approved the Nord Edge HTML prototype and asked to implement it end-to-end in the app.

## Spec

- Prototype: `design/nord-edge-prototype.html`
- Prompt: `docs/design/2026-08-20-nord-edge-impl-prompt.md`

## Phases in this folder

| File | Covers |
|------|--------|
| [01-unlock-usb-first.md](./01-unlock-usb-first.md) | Unlock / Enable detection USB-first |
| [02-home-settings-lab.md](./02-home-settings-lab.md) | Home copy, Settings IA, Lab rename |
| [03-remap-studio.md](./03-remap-studio.md) | Remap chips, icons, search |
| [04-overlay-chord-motion.md](./04-overlay-chord-motion.md) | Overlay tile stagger |
| [05-onboarding-copy.md](./05-onboarding-copy.md) | Onboarding detection / Home CTA |
| [06-reverification-fixes.md](./06-reverification-fixes.md) | Overlay slot icons + USB-first Home banners |

## Verdict

Core Nord Edge acceptance checklist is **READY**. Optional polish not shipped: Home edge ripple on press, Unlock timeline bloom / pad morph motion.

## Verify

```bash
export JAVA_HOME="$HOME/.jdks/jdk17"
./gradlew :app:assembleDebug
```
