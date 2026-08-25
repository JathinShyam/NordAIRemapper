# 2026-08-25 — Unlock embedded in onboarding (no subpage detour)

## Why

Onboarding page 2 ("Enable Plus Key detection") used to navigate to the
standalone Unlock subpage (method selector + checklist), and the user had to
go unlock → back to onboarding → recheck → continue. Pointless to-and-fro.

## What

| File | Change |
|------|--------|
| `presentation/onboarding/OnboardingScreen.kt` | Page 2 is now `DetectionStepContent`: hosts the full `UnlockMethodsSection` (notification gate, Built-In / Shizuku / Manual ADB, status messages) plus the three status chips inline. Continue advances to page 3. Icon tile extracted to shared `StepIconTile`. Second VM (`EnableDetectionViewModel`) scoped to the onboarding nav entry; refreshes on ON_RESUME |
| `presentation/navigation/NordNavHost.kt` | Dropped `onOpenEnableDetection` from the onboarding call. The `ENABLE_DETECTION` route stays for Home / Key learning / Developer / Exclusions / Permissions |

No detection/service code touched; `EnableDetectionScreen` (subpage) itself is
unchanged for entries outside onboarding.

## Verify

1. Fresh install → onboarding: page 2 shows the method selector + Built-In
   checklist directly under the step header.
2. Complete Built-In pairing right there; chips flip green; "Continue" moves
   to page 3 without any navigation hop.
3. "Skip" path gone — Continue works regardless, unlock stays optional.
4. Back arrow still steps back through onboarding pages.
5. Home → Unlock subpage flow unchanged.

## Debug tips

| Symptom | Likely cause |
|---------|--------------|
| Chips don't update after returning from Settings | ON_RESUME refresh missing — check `DisposableEffect` in `DetectionStepContent` |
| Pairing notification completes but page 2 doesn't react | Service relaunch recreates activity → pager resets to page 0 (`rememberSaveable`); user taps forward or we persist last page later |
