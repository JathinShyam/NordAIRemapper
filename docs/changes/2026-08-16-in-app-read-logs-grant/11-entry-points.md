# 11 — Entry points to Enable detection

## Why

Multiple surfaces open the same route. If one regresses, users bounce to Developer/Shizuku-era UX.

## What

Route: `Routes.ENABLE_DETECTION` (`"enable_detection"`) → `EnableDetectionScreen`.

| Entry | Trigger |
|-------|---------|
| Home banner | `HomeBannerAction.OPEN_ENABLE_DETECTION` when READ_LOGS missing (AUTO / ACCESSIBILITY companion / LOGCAT) |
| Onboarding step 2 | Primary “Enable detection” when not granted |
| Developer | “Enable Plus Key detection” when not granted |
| Key setup hint | “Enable detection” when Plus Key missing from Accessibility captures |

`OPEN_DEVELOPER` remains on `HomeBannerAction` for other uses but READ_LOGS banners no longer use it.

## Verify

From each entry above, back stack returns to the previous screen after Done.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Banner still says Developer / Grant READ_LOGS | Stale APK or wrong `primaryAction` |
| Key learning still opens Developer | `onOpenEnableDetection` not wired in `NordNavHost` |
