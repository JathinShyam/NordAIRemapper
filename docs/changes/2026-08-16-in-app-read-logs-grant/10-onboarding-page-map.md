# 10 — Onboarding page map

## Why

Page indices shifted when “Enable Plus Key detection” was inserted after Accessibility. Wrong index assumptions break skip/continue logic.

## What

`PageCount = 6` in `OnboardingScreen.kt`.

| Index | Title | Primary action |
|------:|-------|----------------|
| 0 | Nord AI Remapper | Get started → 1 |
| 1 | Accessibility | Open settings / Continue → 2 |
| 2 | Enable Plus Key detection | Enable detection (nav) / Continue if granted → 3; Skip → 3 |
| 3 | Display over apps | Overlay settings / Continue → 4; Skip → 4 |
| 4 | Keep it alive | Notifications / battery → 5 |
| 5 | You're all set | Complete onboarding → Home |

`OnboardingPermissionState.readLogsGranted` comes from `LogcatWatcherService.hasReadLogsPermission`.

Returning from Enable detection should `ON_RESUME` → `viewModel.refresh()` so step 2 can flip to Continue.

## Verify

Walk onboarding; at step 2 open Enable detection, grant (or skip), confirm later steps still advance and PageDots show 6.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Skip on detection jumps to wrong screen | Off-by-one after insert; check `page = 3` |
| Status still “needed” after grant | Resume refresh not firing; permission on wrong user |
