# 01 — Stable debug signing (shared repo keystore)

## Why

Every CI run built `assembleDebug` on a fresh runner, so Android's
auto-generated debug keystore differed per build. Consequences:

- Each `latest-debug` APK had a DIFFERENT signature than the previous one.
- In-place updates failed with `INSTALL_FAILED_UPDATE_INCOMPATIBLE`.
- Every update therefore required uninstall → reinstall, which wipes ALL
  Unlock grants (READ_LOGS / WRITE_SECURE_SETTINGS / usage access) — the exact
  grants the Unlock flow exists to create.

Observed live 2026-08-25 ~22:35: `adb install -r` of the freshly published
latest-debug over the previous latest-debug failed with the signature error.

## What

| File | Change |
|------|--------|
| `keystore/debug.keystore` (new, committed) | Shared debug keystore (`androiddebugkey/android`, standard debug creds — not a secret) |
| `app/build.gradle.kts` | `signingConfigs.debug` points at `rootProject.file("keystore/debug.keystore")`; `debug` buildType uses it |

`.gitignore` already had `*.keystore` + `!debug.keystore`, so the committed
file is tracked without loosening the ignore rule.

## Effect

All future builds — local `./gradlew :app:assembleDebug` AND CI — share one
signature. `latest-debug` updates install in-place (`adb install -r` or plain
package installer), preserving Unlock grants.

One-time transition cost: the currently installed build (signed by an old
ephemeral key) must be uninstalled once more before the first shared-key build
goes on. After that, never again.

## Verify

1. `./gradlew :app:assembleDebug` locally → install succeeds over a prior
   shared-key build without uninstall.
2. Next CI run's APK installs in-place over the local build.
