# Settings hub: drop battery CTA + Title Case

## Why

Battery optimization already lives under Settings → Permissions. The hub duplicate was noise. User prefers Title Case labels for readability.

## What

| File | Change |
|------|--------|
| `SettingsScreen.kt` | Remove inline `BatteryOptimizationBlock`; Reliability = Permissions + Exclusions; restore colored icon wells |
| `SettingsUi.kt` | Delete unused `BatteryOptimizationBlock`; Title Case empty-state / theme helper copy; optional accent wells on `SettingsNavRow` |
| `AppPermissions.kt` | Title Case permission titles, subtitles, status, hub summary |
| Settings / Tools screens | Title Case titles, subtitles, short status lines |
| `design/settings-preview.html` | Match hub (no battery block; Title Case secondaries) |

Battery exempt CTA remains on Permissions (and onboarding). `SettingsViewModel` battery helpers kept for any residual callers.

## Verify

```bash
export JAVA_HOME="$HOME/.jdks/jdk17"
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

Manual: Settings hub has no battery block; Permissions still opens battery settings; labels read as Title Case.

## Debug tips

| Symptom | Likely cause |
|---------|----------------|
| Hub still shows battery | Stale APK / wrong install |
| Battery nowhere | Permissions Reliability section missing BATTERY item |
