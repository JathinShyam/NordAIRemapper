# Settings hub Compose port

## Why

HTML preview (`design/settings-preview.html`) was approved as the Settings redesign. The app still used stacked cards and incomplete About / Reliability treatment.

## What

| File | Change |
|------|--------|
| `presentation/settings/SettingsScreen.kt` | Full hub rewrite matching preview section order and copy |
| `presentation/settings/SettingsUi.kt` | Hub group / row / theme segment / status chip / battery / exclusions helpers |
| `presentation/settings/SettingsViewModel.kt` | Battery CTA opens exempt-request or battery settings list |

## Structure

Appearance → Shortcuts → Tools → Reliability → About

- One grouped surface per section (hairline dividers)
- Pill theme segment (Dark / Light / System)
- Battery status chip + CTA; exclusions empty panel / chips
- About: version + GitHub hub row

## Verify

1. `./gradlew :app:assembleDebug`
2. Open Settings: grouped surfaces, Title Case labels
3. Theme segment changes appearance; toggles persist
4. Battery CTA / GitHub / exclusions picker still work

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Theme not applying | `MainActivity` not reading `settings.themeMode` |
| Battery chip stuck | Missing `ON_RESUME` → `refreshBattery()` |
| Exclusion labels blank | PackageManager label lookup failed — falls back to package name |
