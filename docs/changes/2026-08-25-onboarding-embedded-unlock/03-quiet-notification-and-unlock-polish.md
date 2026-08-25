# 03 — Quiet service notification + onboarding Unlock page polish

## Why

1. The persistent "Plus Key detection active" FGS notification felt noisy.
   Android forbids removing an FGS notification outright, but the detailed
   variant was also the DEFAULT — unnecessary for most users.
2. The onboarding Unlock page read as a wall of stacked chips and cards.

## What

| File | Change |
|------|--------|
| `domain/model/AppSettings.kt` | `showServiceNotification` default `true` → `false`: fresh installs get the minimal silent "Keyforge · Running" stub; Appearance → Service Notification still re-enables details |
| `service/LogcatWatcherService.kt` | Both notification variants now `VISIBILITY_SECRET` (never on lock screen). Channel stays IMPORTANCE_MIN |
| `presentation/onboarding/OnboardingScreen.kt` | DetectionStepContent: status chips collapsed into one compact row (READ_LOGS / Log access / Banking pause); `SectionLabel "Choose how to unlock"` above methods; when fully unlocked the method UI is replaced by a single success card |

## Verify

1. Fresh install → enable detection → notification shows tiny silent
   "Keyforge / Running"; not on lock screen.
2. Appearance → Service Notification ON → detailed text returns.
3. Onboarding unlock page: one chip row instead of three stacked chips;
   after successful pairing the whole section collapses into the success card.

## Debug tips

| Symptom | Likely cause |
|---------|--------------|
| Detailed notification still shows after update | Stored pref wins over new default — toggle off in Appearance |
| Success card never appears | One of the three conditions false; check which chip would be Warning |
