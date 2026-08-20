# 03 — Remap, onboarding, Unlock

## Why
Remap try snackbar was raw debug text; category chips needed category color; onboarding welcome should show the phone; Unlock ADB command needed clearer copy affordance.

## What
| File | Change |
|------|--------|
| `presentation/remap/RemapScreen.kt` | "Tried current action" → "Trying action…"; selected category chips use `categoryAccent` |
| `presentation/onboarding/OnboardingScreen.kt` | Page 0 PhoneDiagram; AnimatedContent slide/fade; explicit `StatusTone` from permission booleans |
| `presentation/detection/EnableDetectionScreen.kt` | Monospace ADB command in clickable Card; "Tap to copy" hint |

## Verify
- Remap → Try now → snackbar "Trying action…"
- Onboarding pages animate horizontally; page 0 shows phone diagram
- Unlock USB card tap copies command

## Debug tips
| Symptom | Likely cause |
|---------|----------------|
| Onboarding animation jank | AnimatedContent weight inside Box — check page content height |
