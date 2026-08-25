# 02 — Dedicated notifications page before Unlock

## Why

Notification permission was asked inside the Unlock page (gate card) and again
on "Keep it alive" — two places, repeated prompts. One dedicated page before
Unlock covers it once.

## What

| File | Change |
|------|--------|
| `presentation/onboarding/OnboardingScreen.kt` | PageCount 6 → 7. New page 2 "Heads-up codes" (POST_NOTIFICATIONS). Detection/unlock moves to page 3, overlay to 4, battery-only "Keep it alive" to 5 (notifications stripped), finish at 6 |
| `presentation/detection/EnableDetectionScreen.kt` | `UnlockMethodsSection(showNotificationGate = true)` — onboarding passes `false` so the gate card never duplicates. Built-In step 3 no longer disables on missing notifications; edge-case body points to system Settings instead |

The standalone Unlock screen (Home → Unlock) keeps its gate card (default).

## Verify

1. Onboarding order: Welcome → Accessibility → **Heads-up codes** → Detection →
   Display over apps → Keep it alive (battery only) → Finish.
2. Allow notifications on page 2 → Detection page shows NO notification card.
3. Deny + skip → Detection still lets you Pair now; if pairing needs the code
   box, step 3 hints to re-enable notifications in Settings.
4. No other onboarding page mentions notifications.

## Debug tips

| Symptom | Likely cause |
|---------|--------------|
| Gate card still visible in onboarding unlock | `showNotificationGate = false` not passed from `DetectionStepContent` |
| Pairing prompt never appears after skipping page 2 | POST_NOTIFICATIONS denied — expected; re-enable in Settings |
