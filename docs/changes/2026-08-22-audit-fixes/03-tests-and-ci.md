# 03 — Unit tests for TRD §10 + CI gate

## Why
TRD §10 requires `GestureClassifier` timing tests and `RemapAction` JSON
round-trips; neither existed, and CI only ran `assembleDebug`, so even the two
existing test files never executed anywhere.

## What
| File | Change |
|------|--------|
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | `kotlinx-coroutines-test` (matches coroutines 1.9.0) |
| `test/.../GestureClassifierTest.kt` | Virtual-time tests: single fires only after the double window (299ms empty, 301ms fired), double on 2nd UP, long at threshold swallowing its UP, hold cancels pending single, **triple press = DOUBLE then SINGLE after window** (documented burst semantics), duplicate DOWN ignored, idle PULSE = one tap, stray UP no-op |
| `test/.../RemapActionSerializationTest.kt` | Catalog round-trips incl. parametrized actions; stable `@SerialName` spot checks (`none`, `show_overlay`, …); unknown subtype in a backup payload fails loudly instead of partial-apply |
| `.github/workflows/build-debug-apk.yml` | `./gradlew testDebugUnitTest` step before APK build/publish |

## Verify
1. `./gradlew :app:testDebugUnitTest` → 24 passing.
2. CI run on push shows "Run unit tests" green before release upload.

## Debug tips
| Symptom | Likely cause |
|---|---|
| Timing tests fail with empty gesture list | `advanceTimeBy` leaves tasks due exactly at the boundary un-run — assert at deadline+2ms (tests note this) |
