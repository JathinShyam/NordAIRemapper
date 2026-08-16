# 06 — Nav / Home / Onboarding wiring

## Why

Surfaces the grant before remaps fail silently, and teaches the Nord 5 constraint during onboarding.

## What

### Navigation (`NordNavHost.kt`)

- Route `enable_detection` → `EnableDetectionScreen`
- Home: `onOpenEnableDetection`
- Onboarding: `onOpenEnableDetection`
- Key learning: `onOpenEnableDetection` (was Developer)
- Developer: `onOpenEnableDetection`

### Home

| File | Change |
|------|--------|
| `HomeUiState.kt` | `HomeBannerAction.OPEN_ENABLE_DETECTION` |
| `HomeViewModel.kt` | READ_LOGS banners point to Enable detection (not Developer); copy mentions in-app Wireless pair, no Shizuku |
| `HomeScreen.kt` | Handles new banner action |

### Onboarding

| File | Change |
|------|--------|
| `OnboardingScreen.kt` | Page count 6; new step after Accessibility: “Enable Plus Key detection” (`Icons.Outlined.Sensors`); skip allowed |
| `OnboardingViewModel.kt` | `readLogsGranted` via `LogcatWatcherService.hasReadLogsPermission` |

## Verify

Fresh install: Accessibility → Enable detection step → screen opens. Home with Auto + no READ_LOGS shows Enable detection banner.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Banner still opens Developer | Stale build or `OPEN_DEVELOPER` left on a path |
| Onboarding skip leaves remaps dead | Expected until grant; Home banner should still prompt |
| `readLogsGranted` stuck false after grant | Refresh on resume; permission on wrong user profile |
