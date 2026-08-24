# 08 — Unlock methods: Built-In / Shizuku / Manual ADB

## Why

The Unlock screen mixed its two paths in one long scroll (USB first, wireless
behind an "advanced" disclosure). Users with Shizuku already running had no
tap-to-grant option at all and were forced through pairing or a PC.

## What

| File | Change |
|------|--------|
| `gradle/libs.versions.toml`, `app/build.gradle.kts` | + `dev.rikka.shizuku:api/provider` 13.1.5; `buildFeatures.aidl = true` |
| `AndroidManifest.xml` | `ShizukuProvider` declared (only used on the Shizuku path); `<queries>` for `moe.shizuku.manager` |
| `service/shizuku/IGrantService.aidl` + `ShizukuGrantService.kt` (new) | User service that runs inside the Shizuku server process; executes one shell command per call, returns exit code |
| `service/ShizukuGrant.kt` (new) | Availability/permission checks (`pingBinder`, API ≥ 11), binds the user service (`UserServiceArgs.version(1)`), runs the same idempotent `UNLOCK_SHELL_COMMANDS`, verifies exit codes |
| `presentation/detection/EnableDetectionViewModel.kt` | `DetectionMethod {BUILTIN, SHIZUKU, MANUAL_ADB}` selector state; Shizuku state machine (installed → running → permission → granting); permission-result listener registered/cleared in VM lifecycle; shared `afterGrantSucceeded()` verification for all paths; removed dead `showAdvanced` |
| `presentation/detection/EnableDetectionScreen.kt` | Restructured into three selectable method cards (radio semantics, selected border + check), each expanding only its own panel: Built-In = full wireless pairing flow; Shizuku = status checklist + "Unlock via Shizuku" + open-app/recheck; Manual ADB = copy-command card + recheck. Error rows now carry an icon |
| `presentation/onboarding/OnboardingScreen.kt` | Detection step copy now names the three options |
| `EnableDetectionScreen.kt` follow-up | Extracted public `UnlockMethodsSection(viewModel)`; full screen = status header + section. Lab embeds the same section (own `EnableDetectionViewModel` instance) in its READ_LOGS card when not granted, replacing the old copy-command block |

## Security notes

- Shizuku path executes exactly `ElevatedPermissions.UNLOCK_SHELL_COMMANDS`
  (READ_LOGS + WRITE_SECURE_SETTINGS grants + usage-stats appops) — identical
  to what the USB paste block runs. No other commands are ever sent.
- The user service is versioned (`SERVICE_VERSION = 1`); bump it if the AIDL
  changes so Shizuku restarts it.
- No new runtime permission requested from the user beyond Shizuku's own
  consent dialog.

## Verify

1. Open Unlock from onboarding or Home banner: three cards render; Built-In
   preselected; picking each shows only its panel.
2. Without Shizuku installed: Shizuku card reads "app not installed"; Grant
   disabled; panel explains.
3. With Shizuku started but unauthorized: tapping Unlock fires Shizuku's
   permission dialog; allow → grants run → READ_LOGS chip flips green and the
   log-visibility probe result appears.
4. Built-In pairing still works end to end (regression).
5. Manual ADB copy still copies all three commands.

## Debug tips

| Symptom | Likely cause |
|---------|--------------|
| "Shizuku not running" though app installed | manager app not started this boot — open Shizuku, start wireless/adb |
| Grant fails mid-way with exit code | command output drained but failed — run the same command over USB adb to see stderr |
| Service never connects after app update without reinstall | bumped code but not `SERVICE_VERSION` — Shizuku keeps the stale process |
