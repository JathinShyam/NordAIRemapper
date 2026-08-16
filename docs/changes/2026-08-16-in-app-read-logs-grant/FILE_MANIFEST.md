# File manifest — in-app READ_LOGS grant

Complete inventory of paths touched by this change set. Use this when bisecting regressions.

## New files

| Path | Role | Doc |
|------|------|-----|
| `app/src/main/java/com/nordairemapper/service/adb/NordAdbConnectionManager.kt` | ADB RSA key/cert + `AbsAdbConnectionManager` | [03](./03-nord-adb-connection-manager.md) |
| `app/src/main/java/com/nordairemapper/service/adb/ReadLogsGrantViaWirelessAdb.kt` | Pair / grant / verify / sync watcher | [04](./04-read-logs-grant-via-wireless-adb.md) |
| `presentation/detection/EnableDetectionScreen.kt` | Consumer setup UI | [05](./05-enable-detection-ui.md), [13](./13-wireless-debugging-ssid-allow.md) |
| `presentation/detection/EnableDetectionViewModel.kt` | Setup UI state + actions | [05](./05-enable-detection-ui.md), [13](./13-wireless-debugging-ssid-allow.md) |

## Modified — build / platform

| Path | Change summary | Doc |
|------|----------------|-----|
| `settings.gradle.kts` | JitPack repository | [01](./01-gradle-dependencies.md) |
| `gradle/libs.versions.toml` | libadb, sun-security-android, conscrypt versions + aliases | [01](./01-gradle-dependencies.md) |
| `app/build.gradle.kts` | implementation of those three | [01](./01-gradle-dependencies.md) |
| `app/proguard-rules.pro` | keep libadb / Conscrypt / sun (+ `android.sun`) | [01](./01-gradle-dependencies.md), [09](./09-proguard-and-release.md) |
| `app/src/main/AndroidManifest.xml` | INTERNET, network state, multicast, NEARBY_WIFI_DEVICES | [02](./02-manifest-permissions.md) |

## Modified — presentation

| Path | Change summary | Doc |
|------|----------------|-----|
| `presentation/navigation/NordNavHost.kt` | `Routes.ENABLE_DETECTION`; wire Home / onboarding / Key learning / Developer | [06](./06-nav-home-onboarding-wiring.md) |
| `presentation/home/HomeUiState.kt` | `HomeBannerAction.OPEN_ENABLE_DETECTION` | [06](./06-nav-home-onboarding-wiring.md) |
| `presentation/home/HomeViewModel.kt` | Banners → Enable detection; copy update | [06](./06-nav-home-onboarding-wiring.md) |
| `presentation/home/HomeScreen.kt` | Handle new banner action + callback | [06](./06-nav-home-onboarding-wiring.md) |
| `presentation/onboarding/OnboardingScreen.kt` | New step after Accessibility; page count 6 | [06](./06-nav-home-onboarding-wiring.md), [10](./10-onboarding-page-map.md) |
| `presentation/onboarding/OnboardingViewModel.kt` | `readLogsGranted` flag | [06](./06-nav-home-onboarding-wiring.md) |
| `presentation/developer/DeveloperScreen.kt` | Enable detection CTA; USB advanced only | [07](./07-developer-shizuku-deemphasize.md) |
| `presentation/developer/DeveloperViewModel.kt` | Removed Shizuku / wireless / copy-pm helpers | [07](./07-developer-shizuku-deemphasize.md) |
| `presentation/developer/KeyLearningScreen.kt` | Hint + button → Enable detection | [07](./07-developer-shizuku-deemphasize.md) |

## Modified — service / skills

| Path | Change summary | Doc |
|------|----------------|-----|
| `service/DetectionCoordinator.kt` | Removed `ReadLogsGrantHelper.openShizukuOrPlayStore` | [07](./07-developer-shizuku-deemphasize.md) |
| `service/LogcatWatcherService.kt` | KDoc: prefer in-app pair | [07](./07-developer-shizuku-deemphasize.md), [08](./08-detection-sync-and-docs.md) |
| `.cursor/skills/device-testing/SKILL.md` | Phone-only test plan | [08](./08-detection-sync-and-docs.md) |

## Unchanged but depended on

| Path | Why it matters |
|------|----------------|
| `service/DetectionCoordinator.syncLogcatWatcher` | Called after successful grant |
| `service/LogcatWatcherService.hasReadLogsPermission` / `start` / `stop` | Verify + run watcher |
| `service/ReadLogsGrantHelper.ON_DEVICE_SHELL_COMMAND` | Sole shell command string |
| `domain/repository/SettingsRepository` | Strategy + `serviceEnabled` for sync |

## Related product docs updated in this effort

| Path | Change |
|------|--------|
| `docs/ARCHITECTURE.md` | Package tree: `presentation/detection`, `service/adb`, `DetectionCoordinator`; link to this folder (v1.1) |
| `.cursor/skills/device-testing/SKILL.md` | Phone-only grant + test plan (see [08](./08-detection-sync-and-docs.md)) |

Change-log convention itself (`docs/changes/`, `AGENTS.md`, `.cursor/rules/project.mdc`) is documented under [../2026-08-16-change-log-convention/](../2026-08-16-change-log-convention/).

## Not changed (intentionally)

- `RemapEngine` / `GestureClassifier` / accessibility `onKeyEvent` — grant path only; detection rules already accept logcat companion
- No Hilt `@Module` for ADB — `ReadLogsGrantViaWirelessAdb` is `@Inject constructor` + `@Singleton`; `NordAdbConnectionManager` is manual singleton
